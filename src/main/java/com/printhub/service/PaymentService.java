package com.printhub.service;

import com.printhub.db.Database;
import com.printhub.db.Money;
import com.printhub.model.JobOrder;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Receipt;
import com.printhub.model.Transaction;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cash and GCash payments, change, receipts and the daily cash summary (pesos). */
public class PaymentService {

    /** Collections for one day, split by payment method. Void transactions are not counted. */
    public record DailySummary(double cash, double gcash, double total, int count) {
    }

    private static final Pattern ID = Pattern.compile("TXN-(\\d+)");

    private final Database db;
    /** Newest first. */
    private final List<Transaction> transactions = new ArrayList<>();
    private int nextNumber = 1;

    public PaymentService(Database db) {
        this.db = db;
        transactions.addAll(db.query("SELECT id, time, customer, job_id, amount_centavos, method, reference, status,"
                + " discount_centavos, tendered_centavos, change_centavos, handled_by FROM payment"
                + " ORDER BY time DESC, id DESC", rs -> new Transaction(rs.getString(1),
                        LocalDateTime.parse(rs.getString(2)), rs.getString(3), rs.getString(4),
                        Money.fromCentavos(rs.getLong(5)), PaymentMethod.valueOf(rs.getString(6)), rs.getString(7),
                        PaymentStatus.valueOf(rs.getString(8)), Money.fromCentavos(rs.getLong(9)),
                        Money.fromCentavos(rs.getLong(10)), Money.fromCentavos(rs.getLong(11)), rs.getString(12))));
        for (Transaction t : transactions) {
            Matcher m = ID.matcher(t.getId());
            if (m.matches()) {
                nextNumber = Math.max(nextNumber, Integer.parseInt(m.group(1)) + 1);
            }
        }
    }

    /**
     * Takes the payment for a job. The amount is the job's total. Cash must cover it and gives change;
     * GCash needs a reference number. A job can only be paid once.
     */
    public Transaction record(JobOrder job, PaymentMethod method, double tendered, String reference) {
        synchronized (db.lock()) {
            Transaction existing = transactionFor(job);
            if (existing != null && existing.getStatus() == PaymentStatus.PAID) {
                throw new IllegalStateException(job.getId() + " is already paid.");
            }
            validate(job.getTotal(), method, tendered, reference);
            long amount = Money.toCentavos(job.getTotal());
            long given = method == PaymentMethod.CASH ? Money.toCentavos(tendered) : 0;
            long change = method == PaymentMethod.CASH ? given - amount : 0;
            String ref = method == PaymentMethod.GCASH ? reference.trim() : null;
            Transaction t = new Transaction(String.format("TXN-%04d", nextNumber), LocalDateTime.now(),
                    job.getCustomer(), job.getId(), Money.fromCentavos(amount), method, ref, PaymentStatus.PAID,
                    job.getDiscountAmount(), Money.fromCentavos(given), Money.fromCentavos(change),
                    Session.username());
            db.update("INSERT INTO payment(id, time, customer, job_id, amount_centavos, method, reference, status,"
                    + " discount_centavos, tendered_centavos, change_centavos, handled_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    t.getId(), t.getDateTime().toString(), t.getCustomer(), t.getJobId(), amount, method.name(), ref,
                    PaymentStatus.PAID.name(), Money.toCentavos(job.getDiscountAmount()), given, change,
                    t.getHandledBy());
            nextNumber++;
            transactions.add(0, t);
            return t;
        }
    }

    /** Throws IllegalArgumentException if this payment could not be recorded: short cash or no GCash reference. */
    public void validate(double amountDue, PaymentMethod method, double tendered, String reference) {
        if (method == PaymentMethod.CASH) {
            if (Money.toCentavos(tendered) < Money.toCentavos(amountDue)) {
                throw new IllegalArgumentException("Amount tendered is less than the amount due.");
            }
        } else if (reference == null || reference.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter the GCash reference number.");
        }
    }

    /** The job's transaction (paid or void), or null if it has not been paid. */
    public Transaction transactionFor(JobOrder job) {
        synchronized (db.lock()) {
            for (Transaction t : transactions) {
                if (t.getJobId().equals(job.getId())) {
                    return t;
                }
            }
            return null;
        }
    }

    /** Marks the job's payment void (the job was cancelled after it was paid). No effect if unpaid. */
    public void voidFor(JobOrder job) {
        synchronized (db.lock()) {
            Transaction t = transactionFor(job);
            if (t != null && t.getStatus() == PaymentStatus.PAID) {
                db.update("UPDATE payment SET status = ? WHERE id = ?", PaymentStatus.VOID.name(), t.getId());
                t.setStatus(PaymentStatus.VOID);
            }
        }
    }

    /** Every transaction, newest first. */
    public List<Transaction> transactions() {
        synchronized (db.lock()) {
            return new ArrayList<>(transactions);
        }
    }

    /** Transactions on one day, newest first, void ones included. */
    public List<Transaction> on(LocalDate day) {
        List<Transaction> list = new ArrayList<>();
        for (Transaction t : transactions()) {
            if (t.getDateTime().toLocalDate().equals(day)) {
                list.add(t);
            }
        }
        return list;
    }

    public DailySummary dailySummary(LocalDate day) {
        long cash = 0;
        long gcash = 0;
        int count = 0;
        for (Transaction t : on(day)) {
            if (t.getStatus() != PaymentStatus.PAID) {
                continue;
            }
            count++;
            if (t.getMethod() == PaymentMethod.CASH) {
                cash += Money.toCentavos(t.getAmount());
            } else {
                gcash += Money.toCentavos(t.getAmount());
            }
        }
        return new DailySummary(Money.fromCentavos(cash), Money.fromCentavos(gcash),
                Money.fromCentavos(cash + gcash), count);
    }

    public Receipt receipt(Transaction transaction, JobOrder job) {
        return Receipt.of(transaction, job);
    }
}
