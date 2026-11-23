package com.printhub.model;

/** Where a job came from: ordered at the counter, or sent from a phone over the shop's Wi-Fi. */
public enum JobSource {
    WALK_IN("Walk-in"), ONLINE("Online");

    private final String label;

    JobSource(String label) {
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
