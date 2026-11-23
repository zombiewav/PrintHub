package com.printhub.model;

import java.util.List;

/** Result of checking whether the shop has enough paper, ink and other materials for an order. */
public class StockEstimate {

    /** One material: paper of a size, black / color ink, lamination film or photo paper. */
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
    private final List<Line> other;

    public StockEstimate(List<Line> paper, List<Line> ink) {
        this(paper, ink, List.of());
    }

    public StockEstimate(List<Line> paper, List<Line> ink, List<Line> other) {
        this.paper = paper;
        this.ink = ink;
        this.other = other;
    }

    public List<Line> getPaper() {
        return paper;
    }

    public List<Line> getInk() {
        return ink;
    }

    /** Lamination film and photo paper. */
    public List<Line> getOther() {
        return other;
    }

    public boolean isSufficient() {
        for (List<Line> lines : List.of(paper, ink, other)) {
            for (Line l : lines) {
                if (!l.isSufficient()) {
                    return false;
                }
            }
        }
        return true;
    }
}
