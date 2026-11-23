package com.printhub.service;

import com.printhub.db.Database;
import com.printhub.db.Money;
import com.printhub.db.Settings;
import com.printhub.model.Discount;
import com.printhub.model.PaperSize;
import com.printhub.model.Permission;
import com.printhub.model.PriceTable;
import com.printhub.model.Service;
import java.util.ArrayList;
import java.util.List;

/** Service prices per paper size, the rush fee, discounts, and the automatic total for an order. */
public class PricingService {

    private static final double DEFAULT_RUSH_FEE = 20;

    private final Database db;
    private final Settings settings;
    private final PriceTable prices = new PriceTable();
    private final List<Discount> discounts = new ArrayList<>();

    public PricingService(Database db) {
        this.db = db;
        this.settings = new Settings(db);
        for (Object[] row : db.query("SELECT service, size, centavos FROM price",
                rs -> new Object[] {rs.getString(1), rs.getString(2), rs.getLong(3)})) {
            try {
                prices.set(Service.valueOf((String) row[0]), PaperSize.valueOf((String) row[1]),
                        Money.fromCentavos((Long) row[2]));
            } catch (IllegalArgumentException unknownConstant) {
                // a price for a service or size that no longer exists; ignore it
            }
        }
        prices.setRushFee(settings.getDouble("rushFee", DEFAULT_RUSH_FEE));
        discounts.addAll(db.query("SELECT name, kind, value FROM discount ORDER BY rowid",
                rs -> new Discount(rs.getString(1), Discount.Kind.valueOf(rs.getString(2)), rs.getDouble(3))));
    }

    /** The editable price table. Call {@link #save()} after changing it. */
    public PriceTable prices() {
        return prices;
    }

    public void save() {
        Session.require(Permission.EDIT_PRICES);
        synchronized (db.lock()) {
            db.inTransaction(() -> {
                db.update("DELETE FROM price");
                for (Service s : Service.values()) {
                    for (PaperSize z : PaperSize.values()) {
                        db.update("INSERT INTO price(service, size, centavos) VALUES (?,?,?)", s.name(), z.name(),
                                Money.toCentavos(prices.get(s, z)));
                    }
                }
                settings.set("rushFee", Double.toString(prices.getRushFee()));
            });
        }
    }

    public List<Discount> discounts() {
        synchronized (db.lock()) {
            return new ArrayList<>(discounts);
        }
    }

    public Discount addDiscount(Discount discount) {
        Session.require(Permission.EDIT_PRICES);
        synchronized (db.lock()) {
            if (discount.getName() == null || discount.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Enter a discount name.");
            }
            for (Discount d : discounts) {
                if (d.getName().equalsIgnoreCase(discount.getName())) {
                    throw new IllegalArgumentException("There is already a discount named " + d.getName() + ".");
                }
            }
            db.update("INSERT INTO discount(name, kind, value) VALUES (?,?,?)", discount.getName(),
                    discount.getKind().name(), discount.getValue());
            discounts.add(discount);
            return discount;
        }
    }

    public void deleteDiscount(String name) {
        Session.require(Permission.EDIT_PRICES);
        synchronized (db.lock()) {
            db.update("DELETE FROM discount WHERE name = ?", name);
            discounts.removeIf(d -> d.getName().equalsIgnoreCase(name));
        }
    }

    /** Unit price × pages × copies, minus the discount, plus the rush fee. Always worked out here, never trusted from a screen. */
    public double total(Service service, PaperSize size, int pages, int copies, boolean rush, Discount discountOrNull) {
        if (pages <= 0 || copies <= 0) {
            throw new IllegalArgumentException("Pages and copies must be more than zero.");
        }
        int quantity;
        try {
            quantity = Math.multiplyExact(pages, copies);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("That is too many pages.");
        }
        synchronized (db.lock()) {
            double subtotal = prices.get(service, size) * quantity;
            double off = discountOrNull == null ? 0 : discountOrNull.amountOff(subtotal);
            double total = subtotal - off + (rush ? prices.getRushFee() : 0);
            return Money.fromCentavos(Money.toCentavos(total));
        }
    }
}
