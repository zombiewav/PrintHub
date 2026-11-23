package com.printhub.model;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** A plain-text receipt for one transaction. */
public class Receipt {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US);

    private final Transaction transaction;
    private final JobOrder job;

    private Receipt(Transaction transaction, JobOrder job) {
        this.transaction = transaction;
        this.job = job;
    }

    public static Receipt of(Transaction transaction, JobOrder job) {
        return new Receipt(transaction, job);
    }

    private static String peso(double amount) {
        return String.format(Locale.US, "₱%,.2f", amount);
    }

    public String text() {
        StringBuilder sb = new StringBuilder();
        sb.append("PRINTHUB\n");
        sb.append("Official Receipt\n");
        sb.append("------------------------------\n");
        sb.append("Receipt No: ").append(transaction.getId()).append('\n');
        sb.append("Date:       ").append(transaction.getDateTime().format(DATE)).append('\n');
        sb.append("Customer:   ").append(transaction.getCustomer()).append('\n');
        sb.append("Job:        ").append(job.getId()).append('\n');
        sb.append("------------------------------\n");
        sb.append(job.getService().label()).append(" · ").append(job.getSize().label()).append('\n');
        sb.append(job.getPages()).append(" pages × ").append(job.getCopies()).append(" copies\n");
        if (job.isRush()) {
            sb.append("Rush order\n");
        }
        if (transaction.getDiscount() > 0) {
            sb.append("Discount:   -").append(peso(transaction.getDiscount())).append('\n');
        }
        sb.append("------------------------------\n");
        sb.append("TOTAL:      ").append(peso(transaction.getAmount())).append('\n');
        sb.append("Paid by:    ").append(transaction.getMethod().label()).append('\n');
        if (transaction.getMethod() == PaymentMethod.CASH) {
            sb.append("Tendered:   ").append(peso(transaction.getTendered())).append('\n');
            sb.append("Change:     ").append(peso(transaction.getChange())).append('\n');
        } else if (transaction.getReference() != null) {
            sb.append("GCash Ref:  ").append(transaction.getReference()).append('\n');
        }
        sb.append("------------------------------\n");
        sb.append("Served by:  ").append(transaction.getHandledBy()).append('\n');
        sb.append("Thank you!\n");
        return sb.toString();
    }
}
