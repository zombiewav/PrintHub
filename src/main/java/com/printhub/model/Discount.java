package com.printhub.model;

/** A named discount: a percentage off, or a fixed peso amount off. */
public class Discount {

    public enum Kind { PERCENT, FIXED }

    private final String name;
    private final Kind kind;
    private final double value;

    public Discount(String name, Kind kind, double value) {
        this.name = name;
        this.kind = kind;
        this.value = value;
    }

    /** Pesos taken off the subtotal. Never negative and never more than the subtotal. */
    public double amountOff(double subtotal) {
        double off = kind == Kind.PERCENT ? subtotal * value / 100.0 : value;
        return Math.max(0, Math.min(off, subtotal));
    }

    public String getName() {
        return name;
    }

    public Kind getKind() {
        return kind;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}
