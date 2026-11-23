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

    public Transaction(String id, LocalDateTime dateTime, String customer, String jobId,
            double amount, PaymentMethod method, String reference) {
        this.id = id;
        this.dateTime = dateTime;
        this.customer = customer;
        this.jobId = jobId;
        this.amount = amount;
        this.method = method;
        this.reference = reference;
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
}
