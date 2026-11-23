package com.printhub.model;

import java.time.LocalDateTime;

/** A restocking purchase from a supplier. */
public class Restock {

    private final long id;
    private final LocalDateTime time;
    private final Supplier supplier;
    private final Supply supply;
    private final int qty;
    private final double totalCost;
    private final String staff;

    public Restock(long id, LocalDateTime time, Supplier supplier, Supply supply, int qty,
            double totalCost, String staff) {
        this.id = id;
        this.time = time;
        this.supplier = supplier;
        this.supply = supply;
        this.qty = qty;
        this.totalCost = totalCost;
        this.staff = staff;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public Supply getSupply() {
        return supply;
    }

    public int getQty() {
        return qty;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public String getStaff() {
        return staff;
    }
}
