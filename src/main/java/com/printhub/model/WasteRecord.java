package com.printhub.model;

import java.time.LocalDateTime;

public class WasteRecord {

    private final LocalDateTime dateTime;
    private final Supply supply;
    private final int qty;
    private final WasteReason reason;
    private final String jobId;

    public WasteRecord(LocalDateTime dateTime, Supply supply, int qty, WasteReason reason, String jobId) {
        this.dateTime = dateTime;
        this.supply = supply;
        this.qty = qty;
        this.reason = reason;
        this.jobId = jobId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public Supply getSupply() {
        return supply;
    }

    public int getQty() {
        return qty;
    }

    public String qtyText() {
        return qty + " " + supply.getUnit();
    }

    public WasteReason getReason() {
        return reason;
    }

    /** Related job, or null when the waste is not tied to a job. */
    public String getJobId() {
        return jobId;
    }
}
