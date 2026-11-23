package com.printhub.model;

/** Paid, still to be paid, or void (the job was cancelled after payment). */
public enum PaymentStatus {
    PAID("Paid"), UNPAID("Unpaid"), VOID("Void");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
