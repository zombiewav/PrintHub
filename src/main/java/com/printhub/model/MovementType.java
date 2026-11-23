package com.printhub.model;

/** Why a supply's stock changed. */
public enum MovementType {
    IN("In"), OUT("Out"), DAMAGED("Damaged"), WASTE("Waste"), ADJUST("Adjusted");

    private final String label;

    MovementType(String label) {
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
