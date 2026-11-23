package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.db.Settings;
import com.printhub.model.Discount;
import com.printhub.model.JobOrder;
import com.printhub.model.JobSource;
import com.printhub.model.JobStatus;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Service;
import com.printhub.model.StockEstimate;
import com.printhub.model.Supply;
import com.printhub.model.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private Database db;
    private JobQueueService queue;
    private PaymentService payments;
    private OrderService orders;
    private InventoryService inv;
    private Supply shortPaper;
    private Supply longPaper;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        Settings settings = new Settings(db);
        settings.set("autoDispatch", "false");
        inv = new InventoryService(db);
        inv.add(new Supply("SUP-001", "Bond Paper 70gsm", "Paper", "A4", "sheets", 1250, 500));
        shortPaper = inv.add(new Supply("SUP-002", "Bond Paper 70gsm", "Paper", "Short", "sheets", 1000, 500));
        longPaper = inv.add(new Supply("SUP-003", "Bond Paper 70gsm", "Paper", "Long", "sheets", 180, 300));
        inv.add(new Supply("SUP-004", "Black Ink (Epson 003)", "Ink", Supply.NO_SIZE, "pages", 2400, 1000));
        StockEstimator estimator = new StockEstimator(inv, settings);
        queue = new JobQueueService(db, inv, estimator, settings);
        payments = new PaymentService(db);
        PricingService pricing = new PricingService(db);
        pricing.prices().set(Service.BW, PaperSize.SHORT, 2);
        pricing.prices().set(Service.BW, PaperSize.A4, 2);
        pricing.prices().set(Service.BW, PaperSize.LONG, 3);
        orders = new OrderService(db, queue, payments, pricing, estimator);
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    private JobOrder cashOrder(PaperSize size, int pages, int copies, boolean rush, double tendered) {
        return orders.placeWalkIn("Maria Santos", Service.BW, size, pages, copies, rush, null, PaymentMethod.CASH,
                tendered, null);
    }

    @Test
    void placeWalkInCreatesPaidJobAndTransaction() {
        JobOrder job = cashOrder(PaperSize.SHORT, 10, 2, false, 100);
        assertEquals(PaymentStatus.PAID, job.getPaymentStatus());
        assertEquals(JobSource.WALK_IN, job.getSource());
        assertEquals(40, job.getTotal(), 0.0001);
        assertEquals(1, queue.pending().size());
        Transaction t = payments.transactionFor(job);
        assertNotNull(t);
        assertEquals(40, t.getAmount(), 0.0001);
        assertEquals(60, t.getChange(), 0.0001);
        assertEquals(1000, shortPaper.getStock()); // stock is deducted when the job completes, not now
    }

    @Test
    void rushOrderAddsTheRushFee() {
        assertEquals(60, cashOrder(PaperSize.SHORT, 10, 2, true, 100).getTotal(), 0.0001);
    }

    @Test
    void discountReducesJobTotalAndTransaction() {
        JobOrder job = orders.placeWalkIn("Student", Service.BW, PaperSize.A4, 10, 1, false,
                new Discount("Student 10%", Discount.Kind.PERCENT, 10), PaymentMethod.CASH, 20, null);
        assertEquals(18, job.getTotal(), 0.0001);
        assertEquals(2, job.getDiscountAmount(), 0.0001);
        assertEquals("Student 10%", job.getDiscountName());
        assertEquals(18, payments.transactionFor(job).getAmount(), 0.0001);
        assertEquals(2, payments.transactionFor(job).getDiscount(), 0.0001);
    }

    @Test
    void insufficientStockRefusesAndCreatesNothing() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> cashOrder(PaperSize.LONG, 25, 10, false, 1000));
        assertEquals("Not enough stock for this order.", e.getMessage());
        assertTrue(queue.all().isEmpty());
        assertTrue(payments.transactions().isEmpty());
    }

    @Test
    void secondOrderRefusedWhenFirstReservesTheStock() {
        cashOrder(PaperSize.LONG, 10, 10, false, 1000); // 100 of 180 Long sheets claimed
        assertThrows(IllegalStateException.class, () -> cashOrder(PaperSize.LONG, 10, 10, false, 1000));
        cashOrder(PaperSize.LONG, 8, 10, false, 1000); // 80 left, exactly enough
        assertEquals(2, queue.all().size());
        StockEstimate est = orders.estimate(Service.BW, PaperSize.LONG, 1);
        assertFalse(est.isSufficient());
    }

    @Test
    void invalidPaymentOrQuantityCreatesNoJob() {
        assertThrows(IllegalArgumentException.class, () -> cashOrder(PaperSize.A4, 0, 1, false, 100));
        assertThrows(IllegalArgumentException.class, () -> cashOrder(PaperSize.SHORT, 10, 2, false, 39.99));
        assertThrows(IllegalArgumentException.class, () -> orders.placeWalkIn("X", Service.BW, PaperSize.SHORT, 1, 1,
                false, null, PaymentMethod.GCASH, 0, "  "));
        assertTrue(queue.all().isEmpty());
        assertTrue(payments.transactions().isEmpty());
    }

    @Test
    void markPaidCreatesTransactionAndMakesJobReady() {
        JobOrder online = queue.submit("Online", Service.BW, PaperSize.SHORT, 10, 1, false, JobSource.ONLINE,
                PaymentStatus.UNPAID, "uploads/a.pdf", 20, null);
        assertNull(queue.peekNext());
        Transaction t = orders.markPaid(online, PaymentMethod.GCASH, 0, "REF-123");
        assertEquals(20, t.getAmount(), 0.0001);
        assertEquals(PaymentStatus.PAID, online.getPaymentStatus());
        assertSame(online, queue.peekNext());
    }

    @Test
    void markPaidOnCancelledOrPaidJobThrowsAndRecordsNothing() {
        JobOrder cancelled = queue.submit("A", Service.BW, PaperSize.SHORT, 1, 1, false, JobSource.ONLINE,
                PaymentStatus.UNPAID, null, 2, null);
        queue.cancel(cancelled);
        assertThrows(IllegalStateException.class, () -> orders.markPaid(cancelled, PaymentMethod.CASH, 2, null));
        JobOrder paidJob = cashOrder(PaperSize.SHORT, 1, 1, false, 2);
        assertThrows(IllegalStateException.class, () -> orders.markPaid(paidJob, PaymentMethod.CASH, 2, null));
        assertEquals(1, payments.transactions().size());
    }

    @Test
    void markPaidWithBadPaymentLeavesJobUnpaid() {
        JobOrder online = queue.submit("Online", Service.BW, PaperSize.SHORT, 10, 1, false, JobSource.ONLINE,
                PaymentStatus.UNPAID, null, 20, null);
        assertThrows(IllegalArgumentException.class, () -> orders.markPaid(online, PaymentMethod.CASH, 5, null));
        assertEquals(PaymentStatus.UNPAID, online.getPaymentStatus());
        assertNull(queue.peekNext());
        assertTrue(payments.transactions().isEmpty());
    }

    @Test
    void cancelPaidJobVoidsItsTransaction() {
        JobOrder job = cashOrder(PaperSize.SHORT, 10, 2, false, 40);
        orders.cancel(job);
        assertEquals(JobStatus.CANCELLED, job.getStatus());
        assertEquals(PaymentStatus.VOID, payments.transactionFor(job).getStatus());
        assertEquals(1000, shortPaper.getStock());
    }

    @Test
    void cancelUnpaidJobRecordsNoTransaction() {
        JobOrder online = queue.submit("Online", Service.BW, PaperSize.SHORT, 1, 1, false, JobSource.ONLINE,
                PaymentStatus.UNPAID, null, 2, null);
        orders.cancel(online);
        assertNull(payments.transactionFor(online));
    }

    @Test
    void cancelTwiceThrowsAndVoidsOnce() {
        JobOrder job = cashOrder(PaperSize.SHORT, 1, 1, false, 2);
        orders.cancel(job);
        assertThrows(IllegalStateException.class, () -> orders.cancel(job));
    }

    @Test
    void completedWalkInJobDeductsStockAndFreesTheReservation() {
        JobOrder job = cashOrder(PaperSize.LONG, 10, 10, false, 1000);
        queue.startNext();
        queue.complete(job);
        assertEquals(80, longPaper.getStock());
        assertTrue(orders.estimate(Service.BW, PaperSize.LONG, 80).isSufficient());
    }
}
