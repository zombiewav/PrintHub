package com.printhub.model;

import java.time.LocalDateTime;

/** One line of a supply's history: stock in, used by a job, wasted, damaged or adjusted. */
public class StockMovement {

    private final long id;
    private final LocalDateTime time;
    private final Supply supply;
    private final MovementType type;
    private final int qty;
    private final WasteReason reason;
    private final String jobId;
    private final String staff;

    public StockMovement(long id, LocalDateTime time, Supply supply, MovementType type, int qty,
            WasteReason reason, String jobId, String staff) {
        this.id = id;
        this.time = time;
        this.supply = supply;
        this.type = type;
        this.qty = qty;
        this.reason = reason;
        this.jobId = jobId;
        this.staff = staff;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public Supply getSupply() {
        return supply;
    }

    public MovementType getType() {
        return type;
    }

    public int getQty() {
        return qty;
    }

    /** Only set for waste and damage. */
    public WasteReason getReason() {
        return reason;
    }

    public String getJobId() {
        return jobId;
    }

    public String getStaff() {
        return staff;
    }
}
