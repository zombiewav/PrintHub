package com.printhub.model;

/** Admin can do everything; staff run the counter (orders, payments, queue, waste, restock). */
public enum Role {
    ADMIN("Admin"), STAFF("Staff");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean allows(Permission permission) {
        return this == ADMIN;
    }

    @Override
    public String toString() {
        return label;
    }
}
