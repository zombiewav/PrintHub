package com.printhub.model;

public enum WasteReason {
    PAPER_JAM("Paper Jam"), MISPRINT("Misprint"), DAMAGED("Damaged");

    private final String label;

    WasteReason(String label) {
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
