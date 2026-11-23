package com.printhub.model;

public enum JobStatus {
    PENDING("Pending"), PROCESSING("Processing"), COMPLETED("Completed"), CANCELLED("Cancelled");

    private final String label;

    JobStatus(String label) {
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
