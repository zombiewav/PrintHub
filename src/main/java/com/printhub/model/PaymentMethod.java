package com.printhub.model;

public enum PaymentMethod {
    CASH("Cash"), GCASH("GCash");

    private final String label;

    PaymentMethod(String label) {
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
