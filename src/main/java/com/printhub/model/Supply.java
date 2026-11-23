package com.printhub.model;

/** An inventory item. Stock can only change through deduct() and restock(). */
public class Supply {

    public static final String NO_SIZE = "—";

    private final String id;
    private String name;
    private String category;
    private String size;
    private String unit;
    private int stock;
    private int reorderLevel;

    public Supply(String id, String name, String category, String size, String unit, int stock, int reorderLevel) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.size = size;
        this.unit = unit;
        this.stock = stock;
        this.reorderLevel = reorderLevel;
    }

    public void deduct(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be more than zero.");
        }
        if (qty > stock) {
            throw new IllegalArgumentException("Only " + stock + " " + unit + " left in stock.");
        }
        stock -= qty;
    }

    public void restock(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be more than zero.");
        }
        stock += qty;
    }

    /** OK, Low (at or below the reorder level) or Out. */
    public String status() {
        if (stock <= 0) {
            return "Out";
        }
        return stock <= reorderLevel ? "Low" : "OK";
    }

    public boolean needsAttention() {
        return !status().equals("OK");
    }

    public boolean hasSize() {
        return !NO_SIZE.equals(size);
    }

    /** Bond Paper 70gsm · Long */
    public String displayName() {
        return hasSize() ? name + " · " + size : name;
    }

    /** Short label for alerts: Bond Paper · Long, Color Ink Set, Lamination Film · A4. */
    public String shortName() {
        String base = name.replaceAll("\\s*\\(.*?\\)", "").replaceAll("\\s*\\d+gsm", "").replace(" Pouch", "").trim();
        return hasSize() ? base + " · " + size : base;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = Math.max(0, stock);
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = Math.max(0, reorderLevel);
    }

    @Override
    public String toString() {
        return displayName();
    }
}
