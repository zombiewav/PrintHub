package com.printhub.service;

import com.printhub.db.Database;
import com.printhub.model.Discount;
import com.printhub.model.JobOrder;
import com.printhub.model.JobSource;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Service;
import com.printhub.model.StockEstimate;
import com.printhub.model.Transaction;

/**
 * Ties the queue, stock check and payments together so an order is either fully taken or not
 * taken at all: nothing is queued if the stock is short or the payment is invalid.
 */
public class OrderService {

    private final Database db;
    private final JobQueueService queue;
    private final PaymentService payments;
    private final PricingService pricing;
    private final StockEstimator estimator;

    public OrderService(Database db, JobQueueService queue, PaymentService payments, PricingService pricing,
            StockEstimator estimator) {
        this.db = db;
        this.queue = queue;
        this.payments = payments;
        this.pricing = pricing;
        this.estimator = estimator;
    }

    /** Can the shop cover this order, counting what queued jobs have already claimed? */
    public StockEstimate estimate(Service service, PaperSize size, int quantity) {
        synchronized (db.lock()) {
            return estimator.estimate(service, size, quantity, queue.activeJobs());
        }
    }

    public double quote(Service service, PaperSize size, int pages, int copies, boolean rush, Discount discount) {
        return pricing.total(service, size, pages, copies, rush, discount);
    }

    /** Takes a counter order: checks stock and payment, queues the job as paid and records the transaction. */
    public JobOrder placeWalkIn(String customer, Service service, PaperSize size, int pages, int copies,
            boolean rush, Discount discountOrNull, PaymentMethod method, double tendered, String reference) {
        return placeWalkIn(customer, service, size, pages, copies, rush, discountOrNull, method, tendered, reference,
                null, null);
    }

    /**
     * Same, also keeping the GCash sender number and the service detail (ID picture size and count,
     * binding type).
     */
    public JobOrder placeWalkIn(String customer, Service service, PaperSize size, int pages, int copies,
            boolean rush, Discount discountOrNull, PaymentMethod method, double tendered, String reference,
            String sender, String detail) {
        synchronized (db.lock()) {
            double total = pricing.total(service, size, pages, copies, rush, discountOrNull);
            if (!estimate(service, size, pages * copies).isSufficient()) {
                throw new IllegalStateException("Not enough stock for this order.");
            }
            payments.validate(total, method, tendered, reference);
            double subtotal = pricing.prices().get(service, size) * pages * copies;
            double discountAmount = discountOrNull == null ? 0 : discountOrNull.amountOff(subtotal);
            String discountName = discountOrNull == null ? null : discountOrNull.getName();
            JobOrder[] placed = new JobOrder[1];
            db.inTransaction(() -> {
                placed[0] = queue.submit(customer, service, size, pages, copies, rush, JobSource.WALK_IN,
                        PaymentStatus.PAID, null, total, discountName, discountAmount, detail);
                payments.record(placed[0], method, tendered, reference, sender);
            });
            return placed[0];
        }
    }

    /** Staff confirms payment for a waiting (usually online) job so it can be printed. */
    public Transaction markPaid(JobOrder job, PaymentMethod method, double tendered, String reference) {
        return markPaid(job, method, tendered, reference, null);
    }

    /** Same, also keeping the GCash number the customer paid from. */
    public Transaction markPaid(JobOrder job, PaymentMethod method, double tendered, String reference,
            String sender) {
        synchronized (db.lock()) {
            payments.validate(job.getTotal(), method, tendered, reference);
            Transaction[] recorded = new Transaction[1];
            db.inTransaction(() -> {
                queue.markPaid(job);
                recorded[0] = payments.record(job, method, tendered, reference, sender);
            });
            return recorded[0];
        }
    }

    /** Cancels a job; a payment already taken for it is marked void. */
    public void cancel(JobOrder job) {
        synchronized (db.lock()) {
            boolean wasPaid = job.getPaymentStatus() == PaymentStatus.PAID;
            db.inTransaction(() -> {
                queue.cancel(job);
                if (wasPaid) {
                    payments.voidFor(job);
                }
            });
        }
    }
}
