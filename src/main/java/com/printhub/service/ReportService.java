package com.printhub.service;

import com.printhub.model.JobOrder;
import com.printhub.model.JobStatus;
import com.printhub.model.MovementType;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Permission;
import com.printhub.model.Restock;
import com.printhub.model.Service;
import com.printhub.model.StockMovement;
import com.printhub.model.Supply;
import com.printhub.model.Transaction;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Read-only reports for the owner: sales, inventory, restocking, usage and jobs. Admin only. */
public class ReportService {

    /** Sales for a period. Void payments are not counted. */
    public record SalesReport(double total, double cash, double gcash, int count, Map<LocalDate, Double> perDay) {
    }

    public record JobCounts(int completed, int cancelled) {
    }

    private final PaymentService payments;
    private final InventoryService inventory;
    private final SupplierService suppliers;
    private final JobQueueService queue;

    public ReportService(PaymentService payments, InventoryService inventory, SupplierService suppliers,
            JobQueueService queue) {
        this.payments = payments;
        this.inventory = inventory;
        this.suppliers = suppliers;
        this.queue = queue;
    }

    // ---- Sales ------------------------------------------------------------------------------

    public SalesReport salesForDay(LocalDate day) {
        return sales(day, day);
    }

    /** The Monday to Sunday week that contains the date. */
    public SalesReport salesForWeek(LocalDate anyDayInWeek) {
        LocalDate monday = anyDayInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return sales(monday, monday.plusDays(6));
    }

    public SalesReport salesForMonth(YearMonth month) {
        return sales(month.atDay(1), month.atEndOfMonth());
    }

    private SalesReport sales(LocalDate from, LocalDate to) {
        Session.require(Permission.VIEW_REPORTS);
        double cash = 0;
        double gcash = 0;
        int count = 0;
        Map<LocalDate, Double> perDay = new TreeMap<>();
        for (Transaction t : payments.transactions()) {
            LocalDate day = t.getDateTime().toLocalDate();
            if (t.getStatus() != PaymentStatus.PAID || day.isBefore(from) || day.isAfter(to)) {
                continue;
            }
            count++;
            if (t.getMethod() == PaymentMethod.CASH) {
                cash += t.getAmount();
            } else {
                gcash += t.getAmount();
            }
            perDay.merge(day, t.getAmount(), Double::sum);
        }
        return new SalesReport(cash + gcash, cash, gcash, count, perDay);
    }

    // ---- Inventory --------------------------------------------------------------------------

    public List<Supply> inventoryAvailability() {
        Session.require(Permission.VIEW_REPORTS);
        return inventory.supplies();
    }

    public List<Supply> lowStock() {
        Session.require(Permission.VIEW_REPORTS);
        return inventory.lowStock();
    }

    public double inventoryValuation() {
        Session.require(Permission.VIEW_REPORTS);
        return inventory.valuation();
    }

    public double restockExpenses(LocalDate from, LocalDate to) {
        Session.require(Permission.VIEW_REPORTS);
        double total = 0;
        for (Restock r : suppliers.history(null)) {
            LocalDate day = r.getTime().toLocalDate();
            if (!day.isBefore(from) && !day.isAfter(to)) {
                total += r.getTotalCost();
            }
        }
        return total;
    }

    /** Units used by completed jobs, per item, biggest first. Restocks and waste are not usage. */
    public LinkedHashMap<String, Integer> mostUsedMaterials() {
        Session.require(Permission.VIEW_REPORTS);
        Map<String, Integer> used = new LinkedHashMap<>();
        for (StockMovement m : inventory.movements(null)) {
            if (m.getType() == MovementType.OUT) {
                used.merge(m.getSupply().displayName(), m.getQty(), Integer::sum);
            }
        }
        return sortedDescending(used);
    }

    // ---- Jobs -------------------------------------------------------------------------------

    /** Number of non-cancelled jobs per service, most popular first. */
    public LinkedHashMap<Service, Integer> popularServices() {
        Session.require(Permission.VIEW_REPORTS);
        Map<Service, Integer> counts = new EnumMap<>(Service.class);
        for (JobOrder j : queue.all()) {
            if (j.getStatus() != JobStatus.CANCELLED) {
                counts.merge(j.getService(), 1, Integer::sum);
            }
        }
        return sortedDescending(counts);
    }

    public JobCounts jobCounts() {
        Session.require(Permission.VIEW_REPORTS);
        return new JobCounts(queue.completed().size(), queue.cancelled().size());
    }

    private static <K> LinkedHashMap<K, Integer> sortedDescending(Map<K, Integer> counts) {
        List<Map.Entry<K, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        LinkedHashMap<K, Integer> sorted = new LinkedHashMap<>();
        for (Map.Entry<K, Integer> e : entries) {
            sorted.put(e.getKey(), e.getValue());
        }
        return sorted;
    }
}
