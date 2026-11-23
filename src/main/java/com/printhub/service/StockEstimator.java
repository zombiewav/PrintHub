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
 * Works out which materials an order needs and whether the shelf can cover it, counting
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

    /**
     * Materials one job uses: bond paper, lamination film or photo paper depending on the service (scanning and
     * binding use none), plus black and/or color ink.
     */
    public Map<Supply, Integer> needs(Service service, PaperSize size, int quantity) {
        Map<Supply, Integer> needs = new LinkedHashMap<>();
        Supply sheet = switch (service.material()) {
            case PAPER -> inventory.findPaper(size);
            case LAMINATION_FILM -> inventory.findFilm(size);
            case PHOTO_PAPER -> inventory.findPhotoPaper(size);
            case NONE -> null;
        };
        if (sheet != null) {
            needs.put(sheet, quantity);
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
        Service.Material material = service.material();

        List<StockEstimate.Line> paper = new ArrayList<>();
        for (PaperSize z : PaperSize.values()) {
            boolean used = material == Service.Material.PAPER && z == size;
            paper.add(new StockEstimate.Line(z.label(), used, used ? quantity : 0,
                    available(inventory.findPaper(z), reserved), "sheets"));
        }
        List<StockEstimate.Line> inks = new ArrayList<>();
        inks.add(new StockEstimate.Line("Black ink", service.usesBlackInk(), service.usesBlackInk() ? ink : 0,
                available(inventory.findInk(false), reserved), "pages"));
        inks.add(new StockEstimate.Line("Color ink", service.usesColorInk(), service.usesColorInk() ? ink : 0,
                available(inventory.findInk(true), reserved), "pages"));
        List<StockEstimate.Line> other = new ArrayList<>();
        boolean film = material == Service.Material.LAMINATION_FILM;
        boolean photo = material == Service.Material.PHOTO_PAPER;
        other.add(new StockEstimate.Line("Lamination film", film, film ? quantity : 0,
                available(inventory.findFilm(size), reserved), "pcs"));
        other.add(new StockEstimate.Line("Photo paper", photo, photo ? quantity : 0,
                available(inventory.findPhotoPaper(size), reserved), "sheets"));
        return new StockEstimate(paper, inks, other);
    }

    private static int available(Supply supply, Map<Supply, Integer> reserved) {
        return supply == null ? 0 : supply.getStock() - reserved.getOrDefault(supply, 0);
    }
}
