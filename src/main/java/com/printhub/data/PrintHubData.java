package com.printhub.data;

import com.printhub.model.Account;
import com.printhub.model.JobOrder;
import com.printhub.model.JobStatus;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PriceTable;
import com.printhub.model.Service;
import com.printhub.model.StockEstimate;
import com.printhub.model.Supply;
import com.printhub.model.Transaction;
import com.printhub.model.WasteReason;
import com.printhub.model.WasteRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * In-memory data for the whole app, seeded with sample records so every screen has something to show.
 * Screens read from here and register a listener to redraw when anything changes.
 * Swap this class for file or database storage later without touching the screens.
 */
public final class PrintHubData {

    private static final PrintHubData INSTANCE = new PrintHubData();

    public static PrintHubData get() {
        return INSTANCE;
    }

    private final List<Account> accounts = new ArrayList<>();
    private final PriceTable prices = new PriceTable();
    private final Map<String, Supply> supplies = new LinkedHashMap<>();
    private final PriorityQueue<JobOrder> pending = new PriorityQueue<>();
    private final List<JobOrder> processing = new ArrayList<>();
    private final List<JobOrder> completed = new ArrayList<>();
    private final List<WasteRecord> waste = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private int nextJobNo = 236;
    private int nextTxnNo = 422;
    private int nextSupplyNo = 11;

    private PrintHubData() {
        seed();
    }

    // ---- Change notification ----------------------------------------------------------------

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void changed() {
        for (Runnable r : new ArrayList<>(listeners)) {
            r.run();
        }
    }

    // ---- Login ------------------------------------------------------------------------------

    public Account authenticate(String username, String password) {
        for (Account a : accounts) {
            if (a.matches(username, password)) {
                return a;
            }
        }
        return null;
    }

    // ---- Prices -----------------------------------------------------------------------------

    public PriceTable prices() {
        return prices;
    }

    // ---- Supplies ---------------------------------------------------------------------------

    public List<Supply> supplies() {
        return new ArrayList<>(supplies.values());
    }

    public Supply findSupply(String id) {
        return supplies.get(id);
    }

    public String newSupplyId() {
        return String.format("SUP-%03d", nextSupplyNo);
    }

    public void addSupply(Supply supply) {
        supplies.put(supply.getId(), supply);
        nextSupplyNo++;
        changed();
    }

    public void removeSupply(String id) {
        supplies.remove(id);
        changed();
    }

    public void restock(Supply supply, int qty) {
        supply.restock(qty);
        changed();
    }

    public List<Supply> lowStock() {
        List<Supply> list = new ArrayList<>();
        for (Supply s : supplies.values()) {
            if (s.needsAttention()) {
                list.add(s);
            }
        }
        return list;
    }

    private Supply findPaper(PaperSize size) {
        for (Supply s : supplies.values()) {
            if (s.getCategory().equals("Paper") && s.getSize().equals(size.label())) {
                return s;
            }
        }
        return null;
    }

    private Supply findInk(boolean color) {
        for (Supply s : supplies.values()) {
            if (s.getCategory().equals("Ink") && s.getName().startsWith(color ? "Color" : "Black")) {
                return s;
            }
        }
        return null;
    }

    // ---- Orders and the job queue -----------------------------------------------------------

    public String nextOrderId() {
        return String.format("JOB-%04d", nextJobNo);
    }

    /** Checks paper (by size) and ink against what is on the shelf. */
    public StockEstimate estimate(Service service, PaperSize size, int quantity) {
        List<StockEstimate.Line> paper = new ArrayList<>();
        for (PaperSize z : PaperSize.values()) {
            Supply s = findPaper(z);
            boolean used = z == size;
            paper.add(new StockEstimate.Line(z.label(), used, used ? quantity : 0,
                    s == null ? 0 : s.getStock(), "sheets"));
        }
        List<StockEstimate.Line> ink = new ArrayList<>();
        Supply black = findInk(false);
        Supply color = findInk(true);
        ink.add(new StockEstimate.Line("Black ink", service.usesBlackInk(),
                service.usesBlackInk() ? quantity : 0, black == null ? 0 : black.getStock(), "pages"));
        ink.add(new StockEstimate.Line("Color ink", service.usesColorInk(),
                service.usesColorInk() ? quantity : 0, color == null ? 0 : color.getStock(), "pages"));
        return new StockEstimate(paper, ink);
    }

    /**
     * Takes the order: deducts paper and ink, adds the job to the priority queue and records the payment.
     * Rush jobs go ahead of normal ones.
     */
    public JobOrder placeOrder(String customer, Service service, PaperSize size, int pages, int copies,
            boolean rush, PaymentMethod method, String reference) {
        int quantity = pages * copies;
        if (!estimate(service, size, quantity).isSufficient()) {
            throw new IllegalStateException("Not enough stock for this order.");
        }
        findPaper(size).deduct(quantity);
        if (service.usesBlackInk()) {
            findInk(false).deduct(quantity);
        }
        if (service.usesColorInk()) {
            findInk(true).deduct(quantity);
        }

        JobOrder job = new JobOrder(nextJobNo++, customer, service, size, pages, copies, rush, LocalTime.now());
        pending.add(job);

        double total = prices.total(service, size, pages, copies, rush);
        transactions.add(0, new Transaction(String.format("TXN-%04d", nextTxnNo++), LocalDateTime.now(),
                customer, job.getId(), total, method, reference));
        changed();
        return job;
    }

    /** Pending jobs in queue order (rush first, then first come, first served). */
    public List<JobOrder> pendingJobs() {
        List<JobOrder> list = new ArrayList<>(pending);
        Collections.sort(list);
        return list;
    }

    public JobOrder peekNext() {
        return pending.peek();
    }

    public List<JobOrder> processingJobs() {
        return new ArrayList<>(processing);
    }

    public List<JobOrder> completedJobs() {
        return new ArrayList<>(completed);
    }

    public List<JobOrder> allJobs() {
        List<JobOrder> list = new ArrayList<>(pendingJobs());
        list.addAll(processing);
        list.addAll(completed);
        Collections.sort(list, (a, b) -> Integer.compare(b.getNumber(), a.getNumber()));
        return list;
    }

    public JobOrder startNextJob() {
        JobOrder job = pending.poll();
        if (job != null) {
            job.setStatus(JobStatus.PROCESSING);
            processing.add(job);
            changed();
        }
        return job;
    }

    public void completeJob(JobOrder job) {
        if (processing.remove(job)) {
            job.setStatus(JobStatus.COMPLETED);
            completed.add(0, job);
            changed();
        }
    }

    // ---- Waste ------------------------------------------------------------------------------

    public List<WasteRecord> wasteLog() {
        return new ArrayList<>(waste);
    }

    /** Deducts the wasted quantity from stock and writes it to the waste log. */
    public void recordWaste(Supply supply, int qty, WasteReason reason, String jobId) {
        supply.deduct(qty);
        waste.add(0, new WasteRecord(LocalDateTime.now(), supply, qty, reason, jobId));
        changed();
    }

    // ---- Sales ------------------------------------------------------------------------------

    public List<Transaction> transactions() {
        return new ArrayList<>(transactions);
    }

    // ---- Sample data ------------------------------------------------------------------------

    private void seed() {
        accounts.add(new Account("lmendoza", "printhub", "Liza Mendoza", "Cashier"));

        prices.set(Service.BW, PaperSize.A4, 2);
        prices.set(Service.BW, PaperSize.SHORT, 2);
        prices.set(Service.BW, PaperSize.LONG, 3);
        prices.set(Service.COLORED, PaperSize.A4, 10);
        prices.set(Service.COLORED, PaperSize.SHORT, 10);
        prices.set(Service.COLORED, PaperSize.LONG, 12);
        prices.set(Service.LAMINATION, PaperSize.A4, 25);
        prices.set(Service.LAMINATION, PaperSize.SHORT, 25);
        prices.set(Service.LAMINATION, PaperSize.LONG, 30);
        prices.set(Service.ID_PICTURE, PaperSize.A4, 50);
        prices.set(Service.ID_PICTURE, PaperSize.SHORT, 50);
        prices.set(Service.ID_PICTURE, PaperSize.LONG, 60);
        prices.setRushFee(20);

        String none = Supply.NO_SIZE;
        put(new Supply("SUP-001", "Bond Paper 70gsm", "Paper", "A4", "sheets", 1250, 500));
        put(new Supply("SUP-002", "Bond Paper 70gsm", "Paper", "Short", "sheets", 980, 500));
        put(new Supply("SUP-003", "Bond Paper 70gsm", "Paper", "Long", "sheets", 180, 300));
        put(new Supply("SUP-004", "Black Ink (Epson 003)", "Ink", none, "pages", 2400, 1000));
        put(new Supply("SUP-005", "Color Ink Set (Epson 003)", "Ink", none, "pages", 420, 500));
        put(new Supply("SUP-006", "Lamination Film Pouch", "Lamination Film", "A4", "pcs", 60, 100));
        put(new Supply("SUP-007", "Lamination Film Pouch", "Lamination Film", "Long", "pcs", 0, 50));
        put(new Supply("SUP-008", "Glossy Photo Paper 180gsm", "Photo Paper", "A4", "sheets", 90, 50));
        put(new Supply("SUP-009", "Glossy Photo Paper 180gsm", "Photo Paper", "4R", "sheets", 140, 100));
        put(new Supply("SUP-010", "Lamination Film Pouch", "Lamination Film", "Short", "pcs", 120, 100));

        pending.add(new JobOrder(229, "Maria Santos", Service.ID_PICTURE, PaperSize.A4, 1, 3, false, LocalTime.of(10, 15)));
        pending.add(new JobOrder(230, "Pedro Bautista", Service.BW, PaperSize.LONG, 25, 10, false, LocalTime.of(10, 31)));
        pending.add(new JobOrder(231, "Juan Dela Cruz", Service.COLORED, PaperSize.A4, 12, 5, true, LocalTime.of(10, 42)));
        pending.add(new JobOrder(232, "BU College of Science", Service.LAMINATION, PaperSize.A4, 20, 1, false, LocalTime.of(10, 50)));
        pending.add(new JobOrder(233, "Ana Reyes", Service.BW, PaperSize.SHORT, 40, 2, true, LocalTime.of(10, 58)));
        pending.add(new JobOrder(234, "Grace Villanueva", Service.BW, PaperSize.A4, 8, 1, false, LocalTime.of(11, 5)));
        pending.add(new JobOrder(235, "Ramon Cruz", Service.COLORED, PaperSize.SHORT, 4, 2, false, LocalTime.of(11, 12)));

        JobOrder busy = new JobOrder(228, "Carlo Mendoza", Service.COLORED, PaperSize.A4, 30, 3, false, LocalTime.of(9, 48));
        busy.setStatus(JobStatus.PROCESSING);
        processing.add(busy);

        done(new JobOrder(227, "Rina Aquino", Service.BW, PaperSize.A4, 5, 1, false, LocalTime.of(9, 30)));
        done(new JobOrder(226, "Sofia Navarro", Service.BW, PaperSize.LONG, 14, 10, false, LocalTime.of(9, 20)));
        done(new JobOrder(224, "Mark Dizon", Service.BW, PaperSize.A4, 41, 7, false, LocalTime.of(9, 2)));
        done(new JobOrder(222, "Elena Ramos", Service.COLORED, PaperSize.A4, 15, 6, false, LocalTime.of(8, 41)));
        done(new JobOrder(220, "Jun Garcia", Service.ID_PICTURE, PaperSize.SHORT, 11, 1, false, LocalTime.of(8, 30)));

        LocalDate today = LocalDate.now();
        txn(421, today, 11, 12, "Ramon Cruz", 235, 80, PaymentMethod.CASH, null);
        txn(420, today, 11, 5, "Grace Villanueva", 234, 16, PaymentMethod.CASH, null);
        txn(419, today, 10, 58, "Ana Reyes", 233, 180, PaymentMethod.GCASH, "4021 553 981 204");
        txn(418, today, 10, 50, "BU College of Science", 232, 500, PaymentMethod.GCASH, "4021 553 774 090");
        txn(417, today, 10, 42, "Juan Dela Cruz", 231, 620, PaymentMethod.CASH, null);
        txn(416, today, 10, 31, "Pedro Bautista", 230, 750, PaymentMethod.CASH, null);
        txn(415, today, 10, 15, "Maria Santos", 229, 150, PaymentMethod.GCASH, "4021 553 120 388");
        txn(414, today, 9, 48, "Carlo Mendoza", 228, 900, PaymentMethod.CASH, null);
        txn(413, today, 9, 20, "Sofia Navarro", 226, 420, PaymentMethod.GCASH, "4021 552 906 417");
        txn(412, today, 9, 2, "Mark Dizon", 224, 574, PaymentMethod.CASH, null);
        txn(411, today, 8, 41, "Elena Ramos", 222, 900, PaymentMethod.CASH, null);
        txn(410, today, 8, 30, "Jun Garcia", 220, 550, PaymentMethod.GCASH, "4021 552 331 765");

        waste(today, 11, 2, "SUP-003", 3, WasteReason.PAPER_JAM, "JOB-0230");
        waste(today, 9, 40, "SUP-005", 9, WasteReason.MISPRINT, "JOB-0224");
        waste(today.minusDays(1), 16, 15, "SUP-008", 2, WasteReason.MISPRINT, "JOB-0221");
        waste(today.minusDays(1), 14, 30, "SUP-006", 4, WasteReason.DAMAGED, "JOB-0219");
        waste(today.minusDays(3), 15, 5, "SUP-001", 6, WasteReason.PAPER_JAM, "JOB-0212");
        waste(today.minusDays(4), 10, 20, "SUP-002", 5, WasteReason.PAPER_JAM, "JOB-0210");
        waste(today.minusDays(5), 13, 45, "SUP-003", 5, WasteReason.DAMAGED, null);
    }

    private void put(Supply s) {
        supplies.put(s.getId(), s);
    }

    private void done(JobOrder job) {
        job.setStatus(JobStatus.COMPLETED);
        completed.add(job);
    }

    private void txn(int no, LocalDate day, int hour, int minute, String customer, int jobNo,
            double amount, PaymentMethod method, String ref) {
        transactions.add(new Transaction(String.format("TXN-%04d", no), day.atTime(hour, minute), customer,
                String.format("JOB-%04d", jobNo), amount, method, ref));
    }

    private void waste(LocalDate day, int hour, int minute, String supplyId, int qty, WasteReason reason, String jobId) {
        waste.add(new WasteRecord(day.atTime(hour, minute), supplies.get(supplyId), qty, reason, jobId));
    }
}
