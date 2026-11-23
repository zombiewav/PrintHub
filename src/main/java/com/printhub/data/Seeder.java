package com.printhub.data;

import com.printhub.db.Database;
import com.printhub.db.Money;
import com.printhub.model.JobStatus;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PriceTable;
import com.printhub.model.Service;
import com.printhub.model.WasteReason;
import com.printhub.service.PasswordHasher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the sample shop on the very first run so every screen has something to show.
 * Rows are written straight to the database (not through the services) so the sample
 * transactions and waste keep their old dates.
 */
final class Seeder {

    private record SeedJob(int no, String customer, Service service, PaperSize size, int pages, int copies,
            boolean rush, int hour, int minute, JobStatus status) {
    }

    private static final Map<PaperSize, String> PAPER = Map.of(PaperSize.A4, "SUP-001", PaperSize.SHORT, "SUP-002",
            PaperSize.LONG, "SUP-003");

    private Seeder() {
    }

    static void seed(Database db) {
        if (!db.isEmpty()) {
            return;
        }
        db.inTransaction(() -> {
            accounts(db);
            PriceTable prices = prices(db);
            List<SeedJob> jobs = jobs();
            supplies(db, jobs);
            jobsAndPayments(db, prices, jobs);
            wasteHistory(db);
            suppliersAndRestocks(db);
            discounts(db);
        });
    }

    private static void accounts(Database db) {
        db.update("INSERT INTO account(username, password_hash, full_name, role) VALUES (?,?,?,?)",
                "lmendoza", PasswordHasher.hash("printhub"), "Liza Mendoza", "STAFF");
        db.update("INSERT INTO account(username, password_hash, full_name, role) VALUES (?,?,?,?)",
                "admin", PasswordHasher.hash("admin123"), "Shop Admin", "ADMIN");
    }

    private static PriceTable prices(Database db) {
        PriceTable prices = new PriceTable();
        double[][] table = {
            // A4, Short, Long
            {2, 2, 3},        // B&W
            {10, 10, 12},     // Colored
            {25, 25, 30},     // Lamination
            {50, 50, 60},     // ID Picture
            {2, 2, 3},        // Photocopying
            {5, 5, 5},        // Scanning
            {30, 30, 30}      // Document Binding
        };
        Service[] services = {Service.BW, Service.COLORED, Service.LAMINATION, Service.ID_PICTURE, Service.PHOTOCOPY,
            Service.SCANNING, Service.BINDING};
        PaperSize[] sizes = {PaperSize.A4, PaperSize.SHORT, PaperSize.LONG};
        for (int s = 0; s < services.length; s++) {
            for (int z = 0; z < sizes.length; z++) {
                prices.set(services[s], sizes[z], table[s][z]);
                db.update("INSERT INTO price(service, size, centavos) VALUES (?,?,?)", services[s].name(),
                        sizes[z].name(), Money.toCentavos(table[s][z]));
            }
        }
        prices.setRushFee(20);
        db.update("INSERT INTO setting(key, value) VALUES ('rushFee', '20.0')");
        return prices;
    }

    private static List<SeedJob> jobs() {
        JobStatus p = JobStatus.PENDING;
        JobStatus c = JobStatus.COMPLETED;
        return List.of(
                new SeedJob(229, "Maria Santos", Service.ID_PICTURE, PaperSize.A4, 1, 3, false, 10, 15, p),
                new SeedJob(230, "Pedro Bautista", Service.BW, PaperSize.LONG, 25, 10, false, 10, 31, p),
                new SeedJob(231, "Juan Dela Cruz", Service.COLORED, PaperSize.A4, 12, 5, true, 10, 42, p),
                new SeedJob(232, "BU College of Science", Service.LAMINATION, PaperSize.A4, 20, 1, false, 10, 50, p),
                new SeedJob(233, "Ana Reyes", Service.BW, PaperSize.SHORT, 40, 2, true, 10, 58, p),
                new SeedJob(234, "Grace Villanueva", Service.BW, PaperSize.A4, 8, 1, false, 11, 5, p),
                new SeedJob(235, "Ramon Cruz", Service.COLORED, PaperSize.SHORT, 4, 2, false, 11, 12, p),
                new SeedJob(228, "Carlo Mendoza", Service.COLORED, PaperSize.A4, 30, 3, false, 9, 48,
                        JobStatus.PROCESSING),
                new SeedJob(227, "Rina Aquino", Service.BW, PaperSize.A4, 5, 1, false, 9, 30, c),
                new SeedJob(226, "Sofia Navarro", Service.BW, PaperSize.LONG, 14, 10, false, 9, 20, c),
                new SeedJob(224, "Mark Dizon", Service.BW, PaperSize.A4, 41, 7, false, 9, 2, c),
                new SeedJob(222, "Elena Ramos", Service.COLORED, PaperSize.A4, 15, 6, false, 8, 41, c),
                new SeedJob(220, "Jun Garcia", Service.ID_PICTURE, PaperSize.SHORT, 11, 1, false, 8, 30, c));
    }

    /** Stock is today's number plus what the queued jobs will use, so what is *available* equals today's number. */
    private static void supplies(Database db, List<SeedJob> jobs) {
        Map<String, Integer> reserved = new HashMap<>();
        for (SeedJob j : jobs) {
            if (j.status() == JobStatus.COMPLETED) {
                continue;
            }
            int qty = j.pages() * j.copies();
            reserved.merge(PAPER.get(j.size()), qty, Integer::sum);
            if (j.service().usesBlackInk()) {
                reserved.merge("SUP-004", qty, Integer::sum);
            }
            if (j.service().usesColorInk()) {
                reserved.merge("SUP-005", qty, Integer::sum);
            }
        }
        reserved.merge("SUP-004", 20, Integer::sum); // the sample online job below: B&W Short 10 x 2
        reserved.merge("SUP-002", 20, Integer::sum);

        String none = "—";
        Object[][] rows = {
            {"SUP-001", "Bond Paper 70gsm", "Paper", "A4", "sheets", 1250, 500, 0.45},
            {"SUP-002", "Bond Paper 70gsm", "Paper", "Short", "sheets", 980, 500, 0.45},
            {"SUP-003", "Bond Paper 70gsm", "Paper", "Long", "sheets", 180, 300, 0.55},
            {"SUP-004", "Black Ink (Epson 003)", "Ink", none, "pages", 2400, 1000, 0.15},
            {"SUP-005", "Color Ink Set (Epson 003)", "Ink", none, "pages", 420, 500, 0.60},
            {"SUP-006", "Lamination Film Pouch", "Lamination Film", "A4", "pcs", 60, 100, 8.0},
            {"SUP-007", "Lamination Film Pouch", "Lamination Film", "Long", "pcs", 0, 50, 10.0},
            {"SUP-008", "Glossy Photo Paper 180gsm", "Photo Paper", "A4", "sheets", 90, 50, 12.0},
            {"SUP-009", "Glossy Photo Paper 180gsm", "Photo Paper", "4R", "sheets", 140, 100, 6.0},
            {"SUP-010", "Lamination Film Pouch", "Lamination Film", "Short", "pcs", 120, 100, 8.0}
        };
        for (Object[] r : rows) {
            int stock = (Integer) r[5] + reserved.getOrDefault((String) r[0], 0);
            db.update("INSERT INTO supply(id, name, category, size, unit, stock, reorder_level, unit_cost_centavos,"
                    + " active) VALUES (?,?,?,?,?,?,?,?,1)", r[0], r[1], r[2], r[3], r[4], stock, r[6],
                    Money.toCentavos((Double) r[7]));
        }
    }

    private static void jobsAndPayments(Database db, PriceTable prices, List<SeedJob> jobs) {
        LocalDate today = LocalDate.now();
        for (SeedJob j : jobs) {
            double total = prices.total(j.service(), j.size(), j.pages(), j.copies(), j.rush());
            db.update("INSERT INTO job_order(number, customer, service, size, pages, copies, rush, status,"
                    + " payment_status, source, file_path, total_centavos, discount_name, discount_centavos,"
                    + " created_at) VALUES (?,?,?,?,?,?,?,?,'PAID','WALK_IN',NULL,?,NULL,0,?)", j.no(), j.customer(),
                    j.service().name(), j.size().name(), j.pages(), j.copies(), j.rush() ? 1 : 0, j.status().name(),
                    Money.toCentavos(total), today.atTime(j.hour(), j.minute()).toString());
        }
        // A customer who sent a job from their phone and has not paid yet.
        db.update("INSERT INTO job_order(number, customer, service, size, pages, copies, rush, status, payment_status,"
                + " source, file_path, total_centavos, discount_name, discount_centavos, created_at)"
                + " VALUES (236,'Kim Lopez','BW','SHORT',10,2,0,'PENDING','UNPAID','ONLINE',NULL,?,NULL,0,?)",
                Money.toCentavos(prices.total(Service.BW, PaperSize.SHORT, 10, 2, false)),
                LocalDateTime.now().toString());

        txn(db, 421, today, 11, 12, "Ramon Cruz", 235, 80, PaymentMethod.CASH, null);
        txn(db, 420, today, 11, 5, "Grace Villanueva", 234, 16, PaymentMethod.CASH, null);
        txn(db, 419, today, 10, 58, "Ana Reyes", 233, 180, PaymentMethod.GCASH, "4021 553 981 204");
        txn(db, 418, today, 10, 50, "BU College of Science", 232, 500, PaymentMethod.GCASH, "4021 553 774 090");
        txn(db, 417, today, 10, 42, "Juan Dela Cruz", 231, 620, PaymentMethod.CASH, null);
        txn(db, 416, today, 10, 31, "Pedro Bautista", 230, 750, PaymentMethod.CASH, null);
        txn(db, 415, today, 10, 15, "Maria Santos", 229, 150, PaymentMethod.GCASH, "4021 553 120 388");
        txn(db, 414, today, 9, 48, "Carlo Mendoza", 228, 900, PaymentMethod.CASH, null);
        txn(db, 413, today, 9, 20, "Sofia Navarro", 226, 420, PaymentMethod.GCASH, "4021 552 906 417");
        txn(db, 412, today, 9, 2, "Mark Dizon", 224, 574, PaymentMethod.CASH, null);
        txn(db, 411, today, 8, 41, "Elena Ramos", 222, 900, PaymentMethod.CASH, null);
        txn(db, 410, today, 8, 30, "Jun Garcia", 220, 550, PaymentMethod.GCASH, "4021 552 331 765");
    }

    private static void txn(Database db, int no, LocalDate day, int hour, int minute, String customer, int jobNo,
            double amount, PaymentMethod method, String ref) {
        long centavos = Money.toCentavos(amount);
        db.update("INSERT INTO payment(id, time, customer, job_id, amount_centavos, method, reference, status,"
                + " discount_centavos, tendered_centavos, change_centavos, handled_by)"
                + " VALUES (?,?,?,?,?,?,?,'PAID',0,?,0,'lmendoza')", String.format("TXN-%04d", no),
                day.atTime(hour, minute).toString(), customer, String.format("JOB-%04d", jobNo), centavos,
                method.name(), ref, method == PaymentMethod.CASH ? centavos : 0);
    }

    private static void wasteHistory(Database db) {
        LocalDate today = LocalDate.now();
        waste(db, today, 11, 2, "SUP-003", 3, WasteReason.PAPER_JAM, "JOB-0230");
        waste(db, today, 9, 40, "SUP-005", 9, WasteReason.MISPRINT, "JOB-0224");
        waste(db, today.minusDays(1), 16, 15, "SUP-008", 2, WasteReason.MISPRINT, "JOB-0221");
        waste(db, today.minusDays(1), 14, 30, "SUP-006", 4, WasteReason.DAMAGED, "JOB-0219");
        waste(db, today.minusDays(3), 15, 5, "SUP-001", 6, WasteReason.PAPER_JAM, "JOB-0212");
        waste(db, today.minusDays(4), 10, 20, "SUP-002", 5, WasteReason.PAPER_JAM, "JOB-0210");
        waste(db, today.minusDays(5), 13, 45, "SUP-003", 5, WasteReason.DAMAGED, null);
    }

    private static void waste(Database db, LocalDate day, int hour, int minute, String supplyId, int qty,
            WasteReason reason, String jobId) {
        db.update("INSERT INTO stock_movement(time, supply_id, type, qty, reason, job_id, staff)"
                + " VALUES (?,?,?,?,?,?,'lmendoza')", day.atTime(hour, minute).toString(), supplyId,
                reason == WasteReason.DAMAGED ? "DAMAGED" : "WASTE", qty, reason.name(), jobId);
    }

    private static void suppliersAndRestocks(Database db) {
        db.update("INSERT INTO supplier(id, name, address, phone, active) VALUES"
                + " ('SPL-001','Prime Paper Trading','Quezon Avenue, Lucena City','0917 555 0101',1)");
        db.update("INSERT INTO supplier(id, name, address, phone, active) VALUES"
                + " ('SPL-002','Ink Depot Lucena','Diversion Road, Lucena City','0918 555 0202',1)");
        for (String supply : new String[] {"SUP-001", "SUP-002", "SUP-003", "SUP-006", "SUP-007", "SUP-008",
            "SUP-009", "SUP-010"}) {
            db.update("INSERT INTO supplier_supply(supplier_id, supply_id) VALUES ('SPL-001', ?)", supply);
        }
        for (String supply : new String[] {"SUP-004", "SUP-005"}) {
            db.update("INSERT INTO supplier_supply(supplier_id, supply_id) VALUES ('SPL-002', ?)", supply);
        }
        LocalDateTime when = LocalDate.now().minusDays(6).atTime(9, 0);
        restock(db, when, "SPL-001", "SUP-001", 500, 225);
        restock(db, when.plusHours(1), "SPL-002", "SUP-004", 1000, 150);
    }

    private static void restock(Database db, LocalDateTime when, String supplierId, String supplyId, int qty,
            double cost) {
        db.update("INSERT INTO restock(time, supplier_id, supply_id, qty, total_centavos, staff) VALUES (?,?,?,?,?,'admin')",
                when.toString(), supplierId, supplyId, qty, Money.toCentavos(cost));
        db.update("INSERT INTO stock_movement(time, supply_id, type, qty, reason, job_id, staff)"
                + " VALUES (?,?,'IN',?,NULL,NULL,'admin')", when.toString(), supplyId, qty);
    }

    private static void discounts(Database db) {
        db.update("INSERT INTO discount(name, kind, value) VALUES ('Student 10%', 'PERCENT', 10)");
        db.update("INSERT INTO discount(name, kind, value) VALUES ('Senior/PWD 20%', 'PERCENT', 20)");
    }
}
