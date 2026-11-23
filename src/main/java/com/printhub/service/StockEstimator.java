package com.printhub.service;

import com.printhub.db.Settings;
import com.printhub.model.JobOrder;
import com.printhub.model.JobStatus;
import com.printhub.model.PaperSize;
import com.printhub.model.Service;
import com.printhub.model.StockEstimate;
import com.printhub.model.Supply;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Works out which paper and ink an order needs and whether the shelf can cover it, counting
 * what pending and processing jobs have already claimed.
 */
public class StockEstimator {

    private final InventoryService inventory;
    private final Settings settings;

    public StockEstimator(InventoryService inventory, Settings settings) {
        this.inventory = inventory;
        this.settings = settings;
    }

    private int inkPerPage() {
        return Math.max(1, settings.getInt("inkPerPage", 1));
    }

    /** Materials one job uses: paper of its size, plus black and/or color ink depending on the service. */
    public Map<Supply, Integer> needs(Service service, PaperSize size, int quantity) {
        Map<Supply, Integer> needs = new LinkedHashMap<>();
        Supply paper = inventory.findPaper(size);
        if (paper != null) {
            needs.put(paper, quantity);
        }
        int ink = quantity * inkPerPage();
        if (service.usesBlackInk()) {
            Supply black = inventory.findInk(false);
            if (black != null) {
                needs.put(black, ink);
            }
        }
        if (service.usesColorInk()) {
            Supply color = inventory.findInk(true);
            if (color != null) {
                needs.put(color, ink);
            }
        }
        return needs;
    }

    /** Checks the order against stock minus what the active jobs have already claimed. */
    public StockEstimate estimate(Service service, PaperSize size, int quantity, Collection<JobOrder> activeJobs) {
        Map<Supply, Integer> reserved = new HashMap<>();
        for (JobOrder j : activeJobs) {
            if (j.getStatus() == JobStatus.PENDING || j.getStatus() == JobStatus.PROCESSING) {
                needs(j.getService(), j.getSize(), j.sheets()).forEach((s, n) -> reserved.merge(s, n, Integer::sum));
            }
        }
        int ink = quantity * inkPerPage();

        List<StockEstimate.Line> paper = new ArrayList<>();
        for (PaperSize z : PaperSize.values()) {
            Supply s = inventory.findPaper(z);
            boolean used = z == size;
            paper.add(new StockEstimate.Line(z.label(), used, used ? quantity : 0, available(s, reserved), "sheets"));
        }
        List<StockEstimate.Line> inks = new ArrayList<>();
        inks.add(new StockEstimate.Line("Black ink", service.usesBlackInk(), service.usesBlackInk() ? ink : 0,
                available(inventory.findInk(false), reserved), "pages"));
        inks.add(new StockEstimate.Line("Color ink", service.usesColorInk(), service.usesColorInk() ? ink : 0,
                available(inventory.findInk(true), reserved), "pages"));
        return new StockEstimate(paper, inks);
    }

    private static int available(Supply supply, Map<Supply, Integer> reserved) {
        return supply == null ? 0 : supply.getStock() - reserved.getOrDefault(supply, 0);
    }
}
