package com.printhub.service;

import com.printhub.db.Database;
import com.printhub.db.Money;
import com.printhub.db.Settings;
import com.printhub.ds.MinHeap;
import com.printhub.model.JobOrder;
import com.printhub.model.JobSource;
import com.printhub.model.JobStatus;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Service;
import com.printhub.model.Supply;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The print queue.
 * <ul>
 * <li>Paid pending jobs sit in a {@link MinHeap}: rush first, then first come, first served.</li>
 * <li>Unpaid pending jobs wait in a separate list. They never block the line, and when one is marked
 * paid it moves into the heap and takes its original place.</li>
 * <li>The dispatcher starts the next ready job whenever a printer slot is free.</li>
 * </ul>
 */
public class JobQueueService {

    private final Database db;
    private final InventoryService inventory;
    private final StockEstimator estimator;
    private final Settings settings;

    private final Map<Integer, JobOrder> byNumber = new LinkedHashMap<>();
    private final MinHeap<JobOrder> ready = new MinHeap<>();
    private final List<JobOrder> waiting = new ArrayList<>();
    private int nextNumber = 1;

    public JobQueueService(Database db, InventoryService inventory, StockEstimator estimator, Settings settings) {
        this.db = db;
        this.inventory = inventory;
        this.estimator = estimator;
        this.settings = settings;
        load();
    }

    private void load() {
        List<JobOrder> jobs = db.query("SELECT number, customer, service, size, pages, copies, rush, status,"
                + " payment_status, source, file_path, total_centavos, discount_name, created_at, cancelled_at,"
                + " discount_centavos FROM job_order ORDER BY number", rs -> {
                    JobOrder j = new JobOrder(rs.getInt(1), rs.getString(2), Service.valueOf(rs.getString(3)),
                            PaperSize.valueOf(rs.getString(4)), rs.getInt(5), rs.getInt(6), rs.getInt(7) == 1,
                            LocalDateTime.parse(rs.getString(14)));
                    j.setStatus(JobStatus.valueOf(rs.getString(8)));
                    j.setPaymentStatus(PaymentStatus.valueOf(rs.getString(9)));
                    j.setSource(JobSource.valueOf(rs.getString(10)));
                    j.setFilePath(rs.getString(11));
                    j.setTotal(Money.fromCentavos(rs.getLong(12)));
                    j.setDiscountName(rs.getString(13));
                    j.setDiscountAmount(Money.fromCentavos(rs.getLong(16)));
                    if (rs.getString(15) != null) {
                        j.setCancelledAt(LocalDateTime.parse(rs.getString(15)));
                    }
                    return j;
                });
        for (JobOrder j : jobs) {
            byNumber.put(j.getNumber(), j);
            nextNumber = Math.max(nextNumber, j.getNumber() + 1);
            if (j.getStatus() == JobStatus.PENDING) {
                enqueue(j);
            }
        }
    }

    private void enqueue(JobOrder job) {
        if (job.getPaymentStatus() == PaymentStatus.PAID) {
            ready.add(job);
        } else {
            waiting.add(job);
        }
    }

    private void dequeue(JobOrder job) {
        if (!ready.remove(job)) {
            waiting.remove(job);
        }
    }

    // ---- Taking jobs ------------------------------------------------------------------------

    public String nextId() {
        synchronized (db.lock()) {
            return String.format("JOB-%04d", nextNumber);
        }
    }

    /** Adds a job with no discount. See the longer overload. */
    public JobOrder submit(String customer, Service service, PaperSize size, int pages, int copies, boolean rush,
            JobSource source, PaymentStatus payment, String filePath, double total, String discountName) {
        return submit(customer, service, size, pages, copies, rush, source, payment, filePath, total, discountName, 0);
    }

    /** Adds a job to the queue. A paid job is ready at once; an unpaid one waits for payment. */
    public JobOrder submit(String customer, Service service, PaperSize size, int pages, int copies, boolean rush,
            JobSource source, PaymentStatus payment, String filePath, double total, String discountName,
            double discountAmount) {
        if (pages <= 0 || copies <= 0) {
            throw new IllegalArgumentException("Pages and copies must be more than zero.");
        }
        try {
            Math.multiplyExact(pages, copies);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("That is too many pages.");
        }
        synchronized (db.lock()) {
            String name = customer == null || customer.trim().isEmpty() ? "Walk-in" : customer.trim();
            JobOrder job = new JobOrder(nextNumber, name, service, size, pages, copies, rush, LocalDateTime.now());
            job.setPaymentStatus(payment);
            job.setSource(source);
            job.setFilePath(filePath);
            job.setTotal(total);
            job.setDiscountName(discountName);
            job.setDiscountAmount(discountAmount);
            db.update("INSERT INTO job_order(number, customer, service, size, pages, copies, rush, status,"
                    + " payment_status, source, file_path, total_centavos, discount_name, discount_centavos,"
                    + " created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", job.getNumber(), name, service.name(),
                    size.name(), pages, copies, rush ? 1 : 0, JobStatus.PENDING.name(), payment.name(), source.name(),
                    filePath, Money.toCentavos(total), discountName, Money.toCentavos(discountAmount),
                    job.getCreated().toString());
            nextNumber++;
            byNumber.put(job.getNumber(), job);
            enqueue(job);
            dispatch();
            return job;
        }
    }

    // ---- Moving through the queue -----------------------------------------------------------

    /** The job that would start next: the first paid pending job, or null. */
    public JobOrder peekNext() {
        synchronized (db.lock()) {
            return ready.peek();
        }
    }

    /** Starts the next paid job (rush first). Unpaid jobs are skipped. Returns null when none is ready. */
    public JobOrder startNext() {
        synchronized (db.lock()) {
            JobOrder job = ready.poll();
            if (job != null) {
                job.setStatus(JobStatus.PROCESSING);
                save(job);
            }
            return job;
        }
    }

    /** Marks an unpaid pending job as paid so it can be printed. */
    public void markPaid(JobOrder job) {
        synchronized (db.lock()) {
            if (job.getStatus() != JobStatus.PENDING) {
                throw new IllegalStateException("This job is " + job.getStatus().label().toLowerCase()
                        + ", so it cannot be marked paid.");
            }
            if (job.getPaymentStatus() == PaymentStatus.PAID) {
                throw new IllegalStateException("This job is already paid.");
            }
            waiting.remove(job);
            job.setPaymentStatus(PaymentStatus.PAID);
            ready.add(job);
            save(job);
            dispatch();
        }
    }

    /** Finishes a job that is being printed and takes its paper and ink out of stock. */
    public void complete(JobOrder job) {
        synchronized (db.lock()) {
            if (job.getStatus() != JobStatus.PROCESSING) {
                throw new IllegalStateException("Only a job that is being printed can be completed.");
            }
            Map<Supply, Integer> needs = estimator.needs(job.getService(), job.getSize(), job.sheets());
            for (Map.Entry<Supply, Integer> need : needs.entrySet()) {
                if (need.getKey().getStock() < need.getValue()) {
                    throw new IllegalStateException("Not enough " + need.getKey().displayName()
                            + " in stock to finish this job.");
                }
            }
            db.inTransaction(() -> {
                needs.forEach((supply, qty) -> inventory.deduct(supply, qty, job.getId()));
                job.setStatus(JobStatus.COMPLETED);
                save(job);
            });
            dispatch();
        }
    }

    /** Cancels a pending or processing job. Nothing was deducted yet, so stock is untouched. */
    public void cancel(JobOrder job) {
        synchronized (db.lock()) {
            if (job.getStatus() != JobStatus.PENDING && job.getStatus() != JobStatus.PROCESSING) {
                throw new IllegalStateException("A " + job.getStatus().label().toLowerCase()
                        + " job cannot be cancelled.");
            }
            dequeue(job);
            job.setStatus(JobStatus.CANCELLED);
            job.setCancelledAt(LocalDateTime.now());
            save(job);
            dispatch();
        }
    }

    /** Cancels unpaid online jobs that have waited longer than the timeout. Returns how many. */
    public int expireUnpaid(LocalDateTime now) {
        synchronized (db.lock()) {
            int minutes = settings.getInt("unpaidTimeoutMinutes", 30);
            List<JobOrder> expired = new ArrayList<>();
            for (JobOrder j : waiting) {
                if (j.getSource() == JobSource.ONLINE && j.getCreated().plusMinutes(minutes).isBefore(now)) {
                    expired.add(j);
                }
            }
            for (JobOrder j : expired) {
                cancel(j);
            }
            return expired.size();
        }
    }

    /** Starts ready jobs while a printer slot is free (only when auto-dispatch is on). */
    public void dispatch() {
        synchronized (db.lock()) {
            if (!settings.getBool("autoDispatch", true)) {
                return;
            }
            int slots = Math.max(1, settings.getInt("printerSlots", 1));
            while (processing().size() < slots && peekNext() != null) {
                startNext();
            }
        }
    }

    private void save(JobOrder job) {
        db.update("UPDATE job_order SET status = ?, payment_status = ?, cancelled_at = ?, file_path = ?,"
                + " total_centavos = ?, discount_name = ? WHERE number = ?", job.getStatus().name(),
                job.getPaymentStatus().name(), job.getCancelledAt() == null ? null : job.getCancelledAt().toString(),
                job.getFilePath(), Money.toCentavos(job.getTotal()), job.getDiscountName(), job.getNumber());
    }

    // ---- Lists ------------------------------------------------------------------------------

    /** Pending jobs, paid and unpaid, in queue order (rush first, then first come first served). */
    public List<JobOrder> pending() {
        synchronized (db.lock()) {
            List<JobOrder> list = new ArrayList<>(ready.toSortedList());
            list.addAll(waiting);
            Collections.sort(list);
            return list;
        }
    }

    public List<JobOrder> processing() {
        return withStatus(JobStatus.PROCESSING, Comparator.comparingInt(JobOrder::getNumber));
    }

    /** Newest job number first. */
    public List<JobOrder> completed() {
        return withStatus(JobStatus.COMPLETED, Comparator.comparingInt(JobOrder::getNumber).reversed());
    }

    public List<JobOrder> cancelled() {
        return withStatus(JobStatus.CANCELLED, Comparator.comparingInt(JobOrder::getNumber).reversed());
    }

    /** Every job, newest number first. */
    public List<JobOrder> all() {
        synchronized (db.lock()) {
            List<JobOrder> list = new ArrayList<>(byNumber.values());
            list.sort(Comparator.comparingInt(JobOrder::getNumber).reversed());
            return list;
        }
    }

    /** Pending and processing jobs: the ones that have claimed stock. */
    public List<JobOrder> activeJobs() {
        synchronized (db.lock()) {
            List<JobOrder> list = new ArrayList<>();
            for (JobOrder j : byNumber.values()) {
                if (j.getStatus() == JobStatus.PENDING || j.getStatus() == JobStatus.PROCESSING) {
                    list.add(j);
                }
            }
            return list;
        }
    }

    private List<JobOrder> withStatus(JobStatus status, Comparator<JobOrder> order) {
        synchronized (db.lock()) {
            List<JobOrder> list = new ArrayList<>();
            for (JobOrder j : byNumber.values()) {
                if (j.getStatus() == status) {
                    list.add(j);
                }
            }
            list.sort(order);
            return list;
        }
    }
}
