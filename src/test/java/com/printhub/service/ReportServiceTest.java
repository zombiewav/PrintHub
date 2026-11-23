package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.printhub.db.Database;
import com.printhub.db.Settings;
import com.printhub.model.Account;
import com.printhub.model.JobOrder;
import com.printhub.model.JobSource;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Role;
import com.printhub.model.Service;
import com.printhub.model.Supplier;
import com.printhub.model.Supply;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportServiceTest {

    private Database db;
    private Settings settings;
    private InventoryService inv;
    private SupplierService suppliers;
    private JobQueueService queue;
    private ReportService reports;
    private Supply paper;
    private Supply ink;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        settings = new Settings(db);
        settings.set("autoDispatch", "false");
        inv = new InventoryService(db);
        paper = inv.add(new Supply("SUP-002", "Bond Paper 70gsm", "Paper", "Short", "sheets", 1000, 500));
        ink = inv.add(new Supply("SUP-004", "Black Ink", "Ink", Supply.NO_SIZE, "pages", 2000, 1000));
        suppliers = new SupplierService(db, inv);
        queue = new JobQueueService(db, inv, new StockEstimator(inv, settings), settings);
        Session.set(new Account("admin", "x", "Admin", Role.ADMIN));
        reports = new ReportService(new PaymentService(db), inv, suppliers, queue);
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    private void payment(String id, LocalDate day, double amount, String method, String status) {
        db.update("INSERT INTO payment(id,time,customer,job_id,amount_centavos,method,reference,status,handled_by)"
                + " VALUES(?,?,'C',?,?,?,NULL,?,'admin')", id, day.atTime(10, 0).toString(), "JOB-" + id,
                Math.round(amount * 100), method, status);
        reports = new ReportService(new PaymentService(db), inv, suppliers, queue);
    }

    private JobOrder job(Service service) {
        return queue.submit("C", service, PaperSize.SHORT, 1, 1, false, JobSource.WALK_IN, PaymentStatus.PAID, null, 2, null);
    }

    @Test
    void weekRunsMondayToSunday() {
        payment("1", LocalDate.of(2026, 11, 15), 100, "CASH", "PAID"); // Sunday before
        payment("2", LocalDate.of(2026, 11, 16), 200, "CASH", "PAID"); // Monday
        payment("3", LocalDate.of(2026, 11, 22), 300, "GCASH", "PAID"); // Sunday
        payment("4", LocalDate.of(2026, 11, 23), 400, "CASH", "PAID"); // Monday after
        ReportService.SalesReport week = reports.salesForWeek(LocalDate.of(2026, 11, 18));
        assertEquals(500, week.total(), 0.0001);
        assertEquals(200, week.cash(), 0.0001);
        assertEquals(300, week.gcash(), 0.0001);
        assertEquals(2, week.count());
        assertEquals(Map.of(LocalDate.of(2026, 11, 16), 200.0, LocalDate.of(2026, 11, 22), 300.0), week.perDay());
    }

    @Test
    void monthTotalIgnoresVoidAndOtherMonths() {
        payment("1", LocalDate.of(2026, 10, 31), 100, "CASH", "PAID");
        payment("2", LocalDate.of(2026, 11, 1), 200, "CASH", "PAID");
        payment("3", LocalDate.of(2026, 11, 30), 300, "GCASH", "PAID");
        payment("4", LocalDate.of(2026, 11, 15), 999, "CASH", "VOID");
        payment("5", LocalDate.of(2026, 12, 1), 400, "CASH", "PAID");
        ReportService.SalesReport month = reports.salesForMonth(YearMonth.of(2026, 11));
        assertEquals(500, month.total(), 0.0001);
        assertEquals(2, month.count());
    }

    @Test
    void dayReportMatchesDailyCashSummaryExample() {
        LocalDate day = LocalDate.of(2026, 11, 20);
        payment("1", day, 1250, "CASH", "PAID");
        payment("2", day, 850, "GCASH", "PAID");
        ReportService.SalesReport r = reports.salesForDay(day);
        assertEquals(2100, r.total(), 0.0001);
        assertEquals(1250, r.cash(), 0.0001);
        assertEquals(850, r.gcash(), 0.0001);
    }

    @Test
    void mostUsedMaterialsSortedDescending() {
        inv.deduct(ink, 30, "JOB-0001");
        inv.deduct(paper, 100, "JOB-0001");
        inv.deduct(ink, 20, "JOB-0002");
        inv.restock(paper, 500); // IN movements are not "usage"
        inv.recordWaste(paper, 5, com.printhub.model.WasteReason.PAPER_JAM, null); // waste is not "usage" either
        Map<String, Integer> used = reports.mostUsedMaterials();
        assertEquals(List.of("Bond Paper 70gsm · Short", "Black Ink"), List.copyOf(used.keySet()));
        assertEquals(100, used.get("Bond Paper 70gsm · Short"));
        assertEquals(50, used.get("Black Ink"));
    }

    @Test
    void popularServicesExcludeCancelledAndSortDescending() {
        job(Service.BW);
        job(Service.BW);
        job(Service.COLORED);
        JobOrder cancelled = job(Service.LAMINATION);
        queue.cancel(cancelled);
        Map<Service, Integer> popular = reports.popularServices();
        assertEquals(List.of(Service.BW, Service.COLORED), List.copyOf(popular.keySet()));
        assertEquals(2, popular.get(Service.BW));
    }

    @Test
    void jobCountsSplitCompletedAndCancelled() {
        JobOrder done = job(Service.BW);
        job(Service.BW);
        JobOrder cancelled = job(Service.BW);
        queue.startNext();
        queue.complete(done);
        queue.cancel(cancelled);
        ReportService.JobCounts counts = reports.jobCounts();
        assertEquals(1, counts.completed());
        assertEquals(1, counts.cancelled());
    }

    @Test
    void restockExpensesFilteredByDateRange() {
        Supplier s = suppliers.add("Prime", "x", "y", List.of(paper));
        suppliers.restock(s, paper, 100, 500);
        suppliers.restock(s, paper, 50, 300);
        db.update("UPDATE restock SET time = ? WHERE total_centavos = 50000", LocalDate.of(2026, 10, 1).atTime(9, 0).toString());
        assertEquals(300, reports.restockExpenses(LocalDate.now(), LocalDate.now()), 0.0001);
        assertEquals(500, reports.restockExpenses(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1)), 0.0001);
        assertEquals(800, reports.restockExpenses(LocalDate.of(2026, 1, 1), LocalDate.now()), 0.0001);
    }

    @Test
    void inventoryReportsDelegateToInventory() {
        paper.setUnitCost(0.5);
        inv.update(paper);
        assertEquals(2, reports.inventoryAvailability().size());
        assertEquals(1000 * 0.5, reports.inventoryValuation(), 0.0001);
        inv.deduct(paper, 600, "JOB-0001");
        assertEquals(List.of(paper), reports.lowStock());
    }

    @Test
    void staffCannotViewReports() {
        Session.set(new Account("lm", "x", "Liza", Role.STAFF));
        assertThrows(SecurityException.class, () -> reports.salesForDay(LocalDate.now()));
        assertThrows(SecurityException.class, () -> reports.jobCounts());
        assertThrows(SecurityException.class, () -> reports.inventoryValuation());
    }
}
