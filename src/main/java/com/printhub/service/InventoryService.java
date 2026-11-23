package com.printhub.service;

import com.printhub.db.Database;
import com.printhub.db.Money;
import com.printhub.model.MovementType;
import com.printhub.model.PaperSize;
import com.printhub.model.Permission;
import com.printhub.model.StockMovement;
import com.printhub.model.Supply;
import com.printhub.model.WasteReason;
import com.printhub.model.WasteRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inventory: items, stock levels and the stock movement history. Supplies are cached by id, so the
 * same Supply object is returned every time; every stock change is written to the database together
 * with a movement row.
 */
public class InventoryService {

    private static final Pattern ID = Pattern.compile("SUP-(\\d+)");

    private final Database db;
    /** Every supply, including deleted ones, in id order. */
    private final Map<String, Supply> cache = new LinkedHashMap<>();
    /** Stock as last written to the database, so update() can tell when the stock was edited. */
    private final Map<String, Integer> savedStock = new HashMap<>();

    public InventoryService(Database db) {
        this.db = db;
        for (Supply s : db.query("SELECT id, name, category, size, unit, stock, reorder_level, unit_cost_centavos,"
                + " active FROM supply ORDER BY id", rs -> {
                    Supply supply = new Supply(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getInt(6), rs.getInt(7));
                    supply.setUnitCost(Money.fromCentavos(rs.getLong(8)));
                    supply.setActive(rs.getInt(9) == 1);
                    return supply;
                })) {
            cache.put(s.getId(), s);
            savedStock.put(s.getId(), s.getStock());
        }
    }

    // ---- Items ------------------------------------------------------------------------------

    public List<Supply> supplies() {
        synchronized (db.lock()) {
            List<Supply> list = new ArrayList<>();
            for (Supply s : cache.values()) {
                if (s.isActive()) {
                    list.add(s);
                }
            }
            return list;
        }
    }

    /** The active supply with this id, or null. */
    public Supply find(String id) {
        synchronized (db.lock()) {
            Supply s = cache.get(id);
            return s != null && s.isActive() ? s : null;
        }
    }

    /** The next free id: SUP-001, SUP-002, ... */
    public String newId() {
        synchronized (db.lock()) {
            int max = 0;
            for (String id : cache.keySet()) {
                Matcher m = ID.matcher(id);
                if (m.matches()) {
                    max = Math.max(max, Integer.parseInt(m.group(1)));
                }
            }
            return String.format("SUP-%03d", max + 1);
        }
    }

    public Supply add(Supply supply) {
        synchronized (db.lock()) {
            validate(supply);
            if (cache.containsKey(supply.getId())) {
                throw new IllegalArgumentException("There is already an item with id " + supply.getId() + ".");
            }
            db.update("INSERT INTO supply(id, name, category, size, unit, stock, reorder_level, unit_cost_centavos,"
                    + " active) VALUES (?,?,?,?,?,?,?,?,1)", supply.getId(), supply.getName(), supply.getCategory(),
                    supply.getSize(), supply.getUnit(), supply.getStock(), supply.getReorderLevel(),
                    Money.toCentavos(supply.getUnitCost()));
            supply.setActive(true);
            cache.put(supply.getId(), supply);
            savedStock.put(supply.getId(), supply.getStock());
            return supply;
        }
    }

    /**
     * Saves the supply's current fields. The screens edit the Supply object and then call this.
     * A changed stock count is written to the history as an ADJUST movement (signed).
     */
    public void update(Supply supply) {
        synchronized (db.lock()) {
            validate(supply);
            if (!cache.containsKey(supply.getId())) {
                throw new IllegalArgumentException("Unknown item " + supply.getId() + ".");
            }
            int delta = supply.getStock() - savedStock.get(supply.getId());
            db.inTransaction(() -> {
                db.update("UPDATE supply SET name=?, category=?, size=?, unit=?, stock=?, reorder_level=?,"
                        + " unit_cost_centavos=? WHERE id=?", supply.getName(), supply.getCategory(), supply.getSize(),
                        supply.getUnit(), supply.getStock(), supply.getReorderLevel(),
                        Money.toCentavos(supply.getUnitCost()), supply.getId());
                if (delta != 0) {
                    movement(supply, MovementType.ADJUST, delta, null, null);
                }
            });
            savedStock.put(supply.getId(), supply.getStock());
        }
    }

    /** Soft delete: the item disappears from lists, its history stays. */
    public void delete(String id) {
        Session.require(Permission.DELETE_SUPPLY);
        synchronized (db.lock()) {
            Supply s = cache.get(id);
            if (s == null) {
                throw new IllegalArgumentException("Unknown item " + id + ".");
            }
            db.update("UPDATE supply SET active = 0 WHERE id = ?", id);
            s.setActive(false);
        }
    }

    private static void validate(Supply s) {
        if (s.getName() == null || s.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Enter the item name.");
        }
        if (s.getStock() < 0 || s.getReorderLevel() < 0) {
            throw new IllegalArgumentException("Stock and reorder level cannot be negative.");
        }
    }

    // ---- Stock changes ----------------------------------------------------------------------

    public void restock(Supply supply, int qty) {
        change(supply, MovementType.IN, qty, null, null);
    }

    /** Takes materials used by a finished job out of stock. */
    public void deduct(Supply supply, int qty, String jobId) {
        change(supply, MovementType.OUT, qty, null, jobId);
    }

    /** Deducts wasted or damaged material and writes it to the history. */
    public void recordWaste(Supply supply, int qty, WasteReason reason, String jobId) {
        MovementType type = reason == WasteReason.DAMAGED ? MovementType.DAMAGED : MovementType.WASTE;
        change(supply, type, qty, reason, jobId);
    }

    private void change(Supply supply, MovementType type, int qty, WasteReason reason, String jobId) {
        synchronized (db.lock()) {
            if (qty <= 0) {
                throw new IllegalArgumentException("Quantity must be more than zero.");
            }
            int before = supply.getStock();
            if (type == MovementType.IN) {
                supply.restock(qty);
            } else {
                supply.deduct(qty);
            }
            try {
                db.inTransaction(() -> {
                    db.update("UPDATE supply SET stock = ? WHERE id = ?", supply.getStock(), supply.getId());
                    movement(supply, type, qty, reason, jobId);
                });
                savedStock.put(supply.getId(), supply.getStock());
            } catch (RuntimeException e) {
                supply.setStock(before);
                throw e;
            }
        }
    }

    private void movement(Supply supply, MovementType type, int qty, WasteReason reason, String jobId) {
        db.update("INSERT INTO stock_movement(time, supply_id, type, qty, reason, job_id, staff) VALUES (?,?,?,?,?,?,?)",
                LocalDateTime.now().toString(), supply.getId(), type.name(), qty,
                reason == null ? null : reason.name(), jobId, Session.username());
    }

    // ---- Monitoring -------------------------------------------------------------------------

    /** Items at or below their reorder level (including out of stock). */
    public List<Supply> lowStock() {
        List<Supply> list = new ArrayList<>();
        for (Supply s : supplies()) {
            if (s.needsAttention()) {
                list.add(s);
            }
        }
        return list;
    }

    /** Newest first. Pass null for every supply. */
    public List<StockMovement> movements(Supply supplyOrNull) {
        synchronized (db.lock()) {
            String sql = "SELECT id, time, supply_id, type, qty, reason, job_id, staff FROM stock_movement"
                    + (supplyOrNull == null ? "" : " WHERE supply_id = ?") + " ORDER BY id DESC";
            Object[] params = supplyOrNull == null ? new Object[0] : new Object[] {supplyOrNull.getId()};
            return db.query(sql, rs -> new StockMovement(rs.getLong(1), LocalDateTime.parse(rs.getString(2)),
                    cache.get(rs.getString(3)), MovementType.valueOf(rs.getString(4)), rs.getInt(5),
                    rs.getString(6) == null ? null : WasteReason.valueOf(rs.getString(6)), rs.getString(7),
                    rs.getString(8)), params);
        }
    }

    /** Waste and damage history, newest first. */
    public List<WasteRecord> wasteLog() {
        List<WasteRecord> log = new ArrayList<>();
        for (StockMovement m : movements(null)) {
            if (m.getType() == MovementType.WASTE || m.getType() == MovementType.DAMAGED) {
                log.add(new WasteRecord(m.getTime(), m.getSupply(), m.getQty(),
                        m.getReason() == null ? WasteReason.DAMAGED : m.getReason(), m.getJobId()));
            }
        }
        return log;
    }

    /** What the stock on the shelf cost the shop, in pesos. */
    public double valuation() {
        synchronized (db.lock()) {
            double total = 0;
            for (Supply s : supplies()) {
                total += s.getStock() * s.getUnitCost();
            }
            return total;
        }
    }

    // ---- Lookups used by orders -------------------------------------------------------------

    public Supply findPaper(PaperSize size) {
        for (Supply s : supplies()) {
            if (s.getCategory().equals("Paper") && s.getSize().equals(size.label())) {
                return s;
            }
        }
        return null;
    }

    public Supply findInk(boolean color) {
        for (Supply s : supplies()) {
            if (s.getCategory().equals("Ink") && s.getName().startsWith(color ? "Color" : "Black")) {
                return s;
            }
        }
        return null;
    }
}
