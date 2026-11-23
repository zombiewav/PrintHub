package com.printhub.service;

import com.printhub.db.Database;
import com.printhub.db.Money;
import com.printhub.model.Permission;
import com.printhub.model.Restock;
import com.printhub.model.Supplier;
import com.printhub.model.Supply;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The shop's suppliers, what each one provides, and the restocking purchases made from them. */
public class SupplierService {

    private static final Pattern ID = Pattern.compile("SPL-(\\d+)");

    private final Database db;
    private final InventoryService inventory;
    /** Every supplier, including deleted ones, so old purchases still show a name. */
    private final Map<String, Supplier> cache = new LinkedHashMap<>();
    private final Set<String> deleted = new HashSet<>();

    public SupplierService(Database db, InventoryService inventory) {
        this.db = db;
        this.inventory = inventory;
        for (Object[] row : db.query("SELECT id, name, address, phone, active FROM supplier ORDER BY id",
                rs -> new Object[] {new Supplier(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                        rs.getInt(5)})) {
            Supplier s = (Supplier) row[0];
            cache.put(s.getId(), s);
            if ((Integer) row[1] == 0) {
                deleted.add(s.getId());
            }
        }
        for (String[] link : db.query("SELECT supplier_id, supply_id FROM supplier_supply",
                rs -> new String[] {rs.getString(1), rs.getString(2)})) {
            Supplier s = cache.get(link[0]);
            if (s != null) {
                s.getSupplyIds().add(link[1]);
            }
        }
    }

    public List<Supplier> all() {
        synchronized (db.lock()) {
            List<Supplier> list = new ArrayList<>();
            for (Supplier s : cache.values()) {
                if (!deleted.contains(s.getId())) {
                    list.add(s);
                }
            }
            return list;
        }
    }

    public Supplier find(String id) {
        synchronized (db.lock()) {
            return deleted.contains(id) ? null : cache.get(id);
        }
    }

    public String newId() {
        synchronized (db.lock()) {
            int max = 0;
            for (String id : cache.keySet()) {
                Matcher m = ID.matcher(id);
                if (m.matches()) {
                    max = Math.max(max, Integer.parseInt(m.group(1)));
                }
            }
            return String.format("SPL-%03d", max + 1);
        }
    }

    public Supplier add(String name, String address, String phone, Collection<Supply> provides) {
        synchronized (db.lock()) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Enter the supplier name.");
            }
            Supplier s = new Supplier(newId(), name.trim(), address, phone);
            for (Supply p : provides) {
                s.getSupplyIds().add(p.getId());
            }
            db.inTransaction(() -> {
                db.update("INSERT INTO supplier(id, name, address, phone, active) VALUES (?,?,?,?,1)", s.getId(),
                        s.getName(), s.getAddress(), s.getPhone());
                saveLinks(s);
            });
            cache.put(s.getId(), s);
            return s;
        }
    }

    /** Saves the supplier's current fields and products. Screens edit the object, then call this. */
    public void update(Supplier supplier) {
        synchronized (db.lock()) {
            if (supplier.getName() == null || supplier.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Enter the supplier name.");
            }
            db.inTransaction(() -> {
                db.update("UPDATE supplier SET name=?, address=?, phone=? WHERE id=?", supplier.getName(),
                        supplier.getAddress(), supplier.getPhone(), supplier.getId());
                saveLinks(supplier);
            });
        }
    }

    private void saveLinks(Supplier s) {
        db.update("DELETE FROM supplier_supply WHERE supplier_id = ?", s.getId());
        for (String supplyId : s.getSupplyIds()) {
            db.update("INSERT INTO supplier_supply(supplier_id, supply_id) VALUES (?,?)", s.getId(), supplyId);
        }
    }

    /** Soft delete: purchase history is kept. */
    public void delete(String id) {
        Session.require(Permission.MANAGE_SUPPLIERS);
        synchronized (db.lock()) {
            if (!cache.containsKey(id)) {
                throw new IllegalArgumentException("Unknown supplier " + id + ".");
            }
            db.update("UPDATE supplier SET active = 0 WHERE id = ?", id);
            deleted.add(id);
        }
    }

    /**
     * Buys stock from a supplier: raises the supply's stock, records the purchase and sets the supply's
     * unit cost to this purchase's price per unit.
     */
    public Restock restock(Supplier supplier, Supply supply, int qty, double totalCost) {
        synchronized (db.lock()) {
            if (qty <= 0) {
                throw new IllegalArgumentException("Quantity must be more than zero.");
            }
            if (totalCost < 0) {
                throw new IllegalArgumentException("The cost cannot be negative.");
            }
            if (!supplier.getSupplyIds().contains(supply.getId())) {
                throw new IllegalArgumentException(supplier.getName() + " does not provide " + supply.displayName() + ".");
            }
            int stockBefore = supply.getStock();
            double costBefore = supply.getUnitCost();
            LocalDateTime now = LocalDateTime.now();
            try {
                db.inTransaction(() -> {
                    inventory.restock(supply, qty);
                    supply.setUnitCost(totalCost / qty);
                    inventory.update(supply);
                    db.update("INSERT INTO restock(time, supplier_id, supply_id, qty, total_centavos, staff)"
                            + " VALUES (?,?,?,?,?,?)", now.toString(), supplier.getId(), supply.getId(), qty,
                            Money.toCentavos(totalCost), Session.username());
                });
            } catch (RuntimeException e) {
                supply.setStock(stockBefore);
                supply.setUnitCost(costBefore);
                throw e;
            }
            long id = db.query("SELECT last_insert_rowid()", rs -> rs.getLong(1)).get(0);
            return new Restock(id, now, supplier, supply, qty, totalCost, Session.username());
        }
    }

    /** Purchases, newest first. Pass null for every supplier. */
    public List<Restock> history(Supplier supplierOrNull) {
        synchronized (db.lock()) {
            String sql = "SELECT id, time, supplier_id, supply_id, qty, total_centavos, staff FROM restock"
                    + (supplierOrNull == null ? "" : " WHERE supplier_id = ?") + " ORDER BY id DESC";
            Object[] params = supplierOrNull == null ? new Object[0] : new Object[] {supplierOrNull.getId()};
            return db.query(sql, rs -> new Restock(rs.getLong(1), LocalDateTime.parse(rs.getString(2)),
                    cache.get(rs.getString(3)), inventory.lookup(rs.getString(4)), rs.getInt(5),
                    Money.fromCentavos(rs.getLong(6)), rs.getString(7)), params);
        }
    }

    public double totalExpenses(Supplier supplierOrNull) {
        double total = 0;
        for (Restock r : history(supplierOrNull)) {
            total += r.getTotalCost();
        }
        return total;
    }

    /** Suppliers that provide this supply, for the reorder reminder. */
    public List<Supplier> suppliersFor(Supply supply) {
        List<Supplier> list = new ArrayList<>();
        for (Supplier s : all()) {
            if (s.getSupplyIds().contains(supply.getId())) {
                list.add(s);
            }
        }
        return list;
    }
}
