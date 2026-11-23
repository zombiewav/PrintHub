package com.printhub.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.model.Account;
import com.printhub.model.JobOrder;
import com.printhub.model.JobStatus;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Service;
import com.printhub.model.StockEstimate;
import com.printhub.model.Supply;
import com.printhub.model.Transaction;
import com.printhub.service.Session;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrintHubDataTest {

    private Database db;
    private PrintHubData data;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        data = new PrintHubData(db);
        assertNotNull(data.authenticate("lmendoza", "printhub"));
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    private int available(PaperSize size) {
        return data.estimate(Service.BW, size, 1).getPaper().stream()
                .filter(l -> l.getLabel().equals(size.label())).findFirst().get().getAvailable();
    }

    private List<Integer> numbers(List<JobOrder> jobs) {
        return jobs.stream().map(JobOrder::getNumber).toList();
    }

    @Test
    void seedLoadsSampleDataOnce() {
        assertEquals(10, data.supplies().size());
        assertEquals(8, data.pendingJobs().size());
        assertEquals(1, data.processingJobs().size());
        assertEquals(5, data.completedJobs().size());
        assertEquals(12, data.transactions().size());
        assertEquals(7, data.wasteLog().size());
        PrintHubData reopened = new PrintHubData(db);
        assertEquals(10, reopened.supplies().size());
        assertEquals(8, reopened.pendingJobs().size());
        assertEquals(12, reopened.transactions().size());
    }

    @Test
    void availableStockMatchesTodaysSeedNumbers() {
        assertEquals(1250, available(PaperSize.A4));
        assertEquals(980, available(PaperSize.SHORT));
        assertEquals(180, available(PaperSize.LONG));
        StockEstimate est = data.estimate(Service.BW, PaperSize.A4, 1);
        assertEquals(2400, est.getInk().get(0).getAvailable());
        assertEquals(420, est.getInk().get(1).getAvailable());
    }

    @Test
    void pendingOrderMatchesTheQueueRules() {
        assertEquals(List.of(231, 233, 229, 230, 232, 234, 235, 236), numbers(data.pendingJobs()));
        assertEquals(231, data.peekNext().getNumber());
    }

    @Test
    void seededOnlineUnpaidJobIsNeverStartedUntilPaid() {
        JobOrder online = data.pendingJobs().get(7);
        assertEquals(236, online.getNumber());
        assertEquals(PaymentStatus.UNPAID, online.getPaymentStatus());
        int started = 0;
        while (data.startNextJob() != null) {
            started++;
        }
        assertEquals(7, started);
        assertEquals(JobStatus.PENDING, online.getStatus());
        data.markPaid(online, PaymentMethod.CASH, 40, null);
        assertEquals(236, data.startNextJob().getNumber());
    }

    @Test
    void completingAJobDeductsStockButKeepsAvailableUnchanged() {
        JobOrder running = data.processingJobs().get(0); // JOB-0228: colored A4, 30 x 3 = 90 sheets
        Supply a4 = data.findSupply("SUP-001");
        int stockBefore = a4.getStock();
        int availableBefore = available(PaperSize.A4);
        data.completeJob(running);
        assertEquals(stockBefore - 90, a4.getStock());
        assertEquals(availableBefore, available(PaperSize.A4));
        assertEquals(JobStatus.COMPLETED, running.getStatus());
        assertEquals(6, data.completedJobs().size());
    }

    @Test
    void placeOrderTakesStockImmediatelyIntoAccountAndRecordsPayment() {
        int before = available(PaperSize.SHORT);
        JobOrder job = data.placeOrder("Walk-in Customer", Service.BW, PaperSize.SHORT, 10, 2, false,
                PaymentMethod.CASH, null);
        assertEquals("JOB-0237", job.getId());
        assertEquals(before - 20, available(PaperSize.SHORT));
        Transaction t = data.transactions().get(0);
        assertEquals(job.getId(), t.getJobId());
        assertEquals(40, t.getAmount(), 0.0001);
        assertEquals("lmendoza", t.getHandledBy());
        assertEquals("TXN-0422", t.getId());
        assertEquals(9, data.pendingJobs().size());
    }

    @Test
    void orderThatDoesNotFitIsRefused() {
        try {
            data.placeOrder("Big", Service.BW, PaperSize.LONG, 25, 10, false, PaymentMethod.CASH, null);
            throw new AssertionError("expected the order to be refused");
        } catch (IllegalStateException expected) {
            assertEquals("Not enough stock for this order.", expected.getMessage());
        }
        assertEquals(8, data.pendingJobs().size());
    }

    @Test
    void loginWorksForBothSeedAccounts() {
        Account staff = data.authenticate("lmendoza", "printhub");
        assertEquals("Staff", staff.getRole());
        Account admin = data.authenticate("admin", "admin123");
        assertEquals("Admin", admin.getRole());
        assertNull(data.authenticate("admin", "wrong"));
    }

    @Test
    void stateSurvivesReopen() {
        data.placeOrder("Walk-in Customer", Service.BW, PaperSize.SHORT, 1, 1, false, PaymentMethod.CASH, null);
        PrintHubData reopened = new PrintHubData(db);
        assertEquals(9, reopened.pendingJobs().size());
        assertEquals("JOB-0238", reopened.nextOrderId());
    }

    @Test
    void dailySummaryIncludesSeedCashAndGcash() {
        var summary = data.payments().dailySummary(LocalDate.now());
        assertEquals(12, summary.count());
        assertNotEquals(0, summary.cash());
        assertNotEquals(0, summary.gcash());
        assertEquals(summary.cash() + summary.gcash(), summary.total(), 0.0001);
    }

    @Test
    void cancelledPaidJobIsLeftOutOfTheSalesList() {
        JobOrder ramon = data.pendingJobs().stream().filter(j -> j.getNumber() == 235).findFirst().get();
        assertEquals(12, data.transactions().size());
        data.cancelJob(ramon);
        assertEquals(11, data.transactions().size());
        assertTrue(data.transactions().stream().noneMatch(t -> t.getJobId().equals("JOB-0235")));
        // the void payment is still on record for audits and reports
        assertEquals(12, data.payments().transactions().size());
        assertEquals(PaymentStatus.VOID, data.transactionFor(ramon).getStatus());
    }

    @Test
    void listenersAreNotifiedOnChange() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        data.addListener(latch::countDown);
        data.startNextJob();
        assertTrue(latch.await(3, TimeUnit.SECONDS));
    }
}
