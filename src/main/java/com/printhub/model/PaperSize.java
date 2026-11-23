package com.printhub.model;

public enum PaperSize {
    A4("A4"), SHORT("Short"), LONG("Long");

    private final String label;

    PaperSize(String label) {
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
