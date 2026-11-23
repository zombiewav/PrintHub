package com.printhub.model;

import java.util.List;

/** Result of checking whether the shop has enough paper and ink for an order. */
public class StockEstimate {

    /** One material: paper of a size, or black / color ink. */
    public static class Line {
        private final String label;
        private final boolean used;
        private final int required;
        private final int available;
        private final String unit;

        public Line(String label, boolean used, int required, int available, String unit) {
            this.label = label;
            this.used = used;
            this.required = required;
            this.available = available;
            this.unit = unit;
        }

        public String getLabel() {
            return label;
        }

        /** False when the order does not need this material ("Not used"). */
        public boolean isUsed() {
            return used;
        }

        public int getRequired() {
            return required;
        }

        public int getAvailable() {
            return available;
        }

        public String getUnit() {
            return unit;
        }

        public boolean isSufficient() {
            return !used || available >= required;
        }

        /** Stock left once this order is done (what is available now when the order does not use it). */
        public int getRemainingAfter() {
            return used ? available - required : available;
        }

        public int shortBy() {
            return used ? Math.max(0, required - available) : 0;
        }
    }

    private final List<Line> paper;
    private final List<Line> ink;

    public StockEstimate(List<Line> paper, List<Line> ink) {
        this.paper = paper;
        this.ink = ink;
    }

    public List<Line> getPaper() {
        return paper;
    }

    public List<Line> getInk() {
        return ink;
    }

    public boolean isSufficient() {
        for (Line l : paper) {
            if (!l.isSufficient()) {
                return false;
            }
        }
        for (Line l : ink) {
            if (!l.isSufficient()) {
                return false;
            }
        }
        return true;
    }
}
