package com.printhub.model;

import java.time.LocalDateTime;

public class Transaction {

    private final String id;
    private final LocalDateTime dateTime;
    private final String customer;
    private final String jobId;
    private final double amount;
    private final PaymentMethod method;
    private final String reference;
    private PaymentStatus status;
    private final double discount;
    private final double tendered;
    private final double change;
    private final String handledBy;

    public Transaction(String id, LocalDateTime dateTime, String customer, String jobId,
            double amount, PaymentMethod method, String reference, PaymentStatus status,
            double discount, double tendered, double change, String handledBy) {
        this.id = id;
        this.dateTime = dateTime;
        this.customer = customer;
        this.jobId = jobId;
        this.amount = amount;
        this.method = method;
        this.reference = reference;
        this.status = status;
        this.discount = discount;
        this.tendered = tendered;
        this.change = change;
        this.handledBy = handledBy;
    }

    /** A paid transaction with no discount, tendered amount or staff name (sample data). */
    public Transaction(String id, LocalDateTime dateTime, String customer, String jobId,
            double amount, PaymentMethod method, String reference) {
        this(id, dateTime, customer, jobId, amount, method, reference, PaymentStatus.PAID, 0,
                method == PaymentMethod.CASH ? amount : 0, 0, "system");
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public String getCustomer() {
        return customer;
    }

    public String getJobId() {
        return jobId;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    /** GCash reference number, or null for cash. */
    public String getReference() {
        return reference;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    /** Pesos taken off by a discount. */
    public double getDiscount() {
        return discount;
    }

    /** Cash handed over by the customer (0 for GCash). */
    public double getTendered() {
        return tendered;
    }

    public double getChange() {
        return change;
    }

    /** Username of the staff member who took the payment. */
    public String getHandledBy() {
        return handledBy;
    }
}
