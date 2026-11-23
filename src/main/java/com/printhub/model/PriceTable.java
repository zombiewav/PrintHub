package com.printhub.model;

import java.util.EnumMap;
import java.util.Map;

/** Unit price per service and paper size, plus the flat rush fee. */
public class PriceTable {

    private final Map<Service, Map<PaperSize, Double>> prices = new EnumMap<>(Service.class);
    private double rushFee;

    public PriceTable() {
        for (Service s : Service.values()) {
            prices.put(s, new EnumMap<>(PaperSize.class));
        }
    }

    public double get(Service service, PaperSize size) {
        return prices.get(service).getOrDefault(size, 0.0);
    }

    public void set(Service service, PaperSize size, double price) {
        prices.get(service).put(size, price);
    }

    public double getRushFee() {
        return rushFee;
    }

    public void setRushFee(double rushFee) {
        this.rushFee = rushFee;
    }

    /** Unit price x pages x copies, plus the rush fee when the order is rush. */
    public double total(Service service, PaperSize size, int pages, int copies, boolean rush) {
        return get(service, size) * pages * copies + (rush ? rushFee : 0);
    }
}
