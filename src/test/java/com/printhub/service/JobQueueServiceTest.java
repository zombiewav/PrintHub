package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.db.Settings;
import com.printhub.model.JobOrder;
import com.printhub.model.JobSource;
import com.printhub.model.JobStatus;
import com.printhub.model.MovementType;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Service;
import com.printhub.model.StockMovement;
import com.printhub.model.Supply;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobQueueServiceTest {

    private Database db;
    private Settings settings;
    private InventoryService inv;
    private JobQueueService queue;
    private Supply shortPaper;
    private Supply black;
    private Supply color;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        settings = new Settings(db);
        settings.set("autoDispatch", "false");
        inv = new InventoryService(db);
        shortPaper = inv.add(new Supply("SUP-002", "Bond Paper 70gsm", "Paper", "Short", "sheets", 1000, 500));
        inv.add(new Supply("SUP-003", "Bond Paper 70gsm", "Paper", "Long", "sheets", 1000, 300));
        black = inv.add(new Supply("SUP-004", "Black Ink (Epson 003)", "Ink", Supply.NO_SIZE, "pages", 5000, 1000));
        color = inv.add(new Supply("SUP-005", "Color Ink Set (Epson 003)", "Ink", Supply.NO_SIZE, "pages", 5000, 500));
        queue = new JobQueueService(db, inv, new StockEstimator(inv, settings), settings);
    }

    private JobOrder paid(String customer, boolean rush) {
        return queue.submit(customer, Service.BW, PaperSize.SHORT, 1, 1, rush, JobSource.WALK_IN,
                PaymentStatus.PAID, null, 2, null);
    }

    private JobOrder unpaid(String customer, boolean rush) {
        return queue.submit(customer, Service.BW, PaperSize.SHORT, 1, 1, rush, JobSource.ONLINE,
                PaymentStatus.UNPAID, "uploads/a.pdf", 2, null);
    }

    @Test
    void rushJobIsNextBeforeEarlierNormalJob() {
        JobOrder normal = paid("A", false);
        JobOrder rush = paid("B", true);
        assertSame(rush, queue.peekNext());
        assertSame(rush, queue.startNext());
        assertSame(normal, queue.startNext());
        assertNull(queue.startNext());
    }

    @Test
    void unpaidJobIsSkippedAndNextPaidJobStartsImmediately() {
        JobOrder first = unpaid("Online", false);
        JobOrder second = paid("Walk-in", false);
        assertSame(second, queue.peekNext());
        assertSame(second, queue.startNext());
        assertEquals(JobStatus.PROCESSING, second.getStatus());
        assertEquals(JobStatus.PENDING, first.getStatus());
        assertNull(queue.startNext());
    }

    @Test
    void markPaidKeepsOriginalPriorityPlace() {
        JobOrder unpaidRush = unpaid("Rusher", true);
        JobOrder normal = paid("Normal", false);
        assertSame(normal, queue.peekNext());
        queue.markPaid(unpaidRush);
        assertEquals(PaymentStatus.PAID, unpaidRush.getPaymentStatus());
        assertSame(unpaidRush, queue.peekNext());
    }

    @Test
    void pendingListIncludesUnpaidInQueueOrder() {
        JobOrder a = unpaid("A", false);
        JobOrder b = paid("B", true);
        JobOrder c = paid("C", false);
        assertEquals(List.of(b, a, c), queue.pending());
    }

    @Test
    void completeDeductsPaperAndInkAndLogsJobId() {
        JobOrder job = queue.submit("A", Service.BW, PaperSize.SHORT, 40, 2, false, JobSource.WALK_IN,
                PaymentStatus.PAID, null, 160, null);
        queue.startNext();
        queue.complete(job);
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals(920, shortPaper.getStock());
        assertEquals(4920, black.getStock());
        assertEquals(5000, color.getStock());
        List<StockMovement> history = inv.movements(shortPaper);
        assertEquals(MovementType.OUT, history.get(0).getType());
        assertEquals(job.getId(), history.get(0).getJobId());
        assertEquals(List.of(job), queue.completed());
        assertTrue(queue.processing().isEmpty());
    }

    @Test
    void completeFailsWhenStockIsGoneAndChangesNothing() {
        JobOrder job = queue.submit("A", Service.COLORED, PaperSize.SHORT, 40, 2, false, JobSource.WALK_IN,
                PaymentStatus.PAID, null, 800, null);
        queue.startNext();
        inv.recordWaste(color, 4950, com.printhub.model.WasteReason.DAMAGED, null); // 50 color ink left, need 80
        assertThrows(IllegalStateException.class, () -> queue.complete(job));
        assertEquals(JobStatus.PROCESSING, job.getStatus());
        assertEquals(1000, shortPaper.getStock());
        assertEquals(50, color.getStock());
    }

    @Test
    void cancelPendingWaitingAndProcessingJobsLeavesStockUntouched() {
        JobOrder pendingJob = paid("A", false);
        JobOrder waiting = unpaid("B", false);
        JobOrder running = paid("C", true);
        queue.startNext(); // C starts
        queue.cancel(pendingJob);
        queue.cancel(waiting);
        queue.cancel(running);
        assertEquals(JobStatus.CANCELLED, pendingJob.getStatus());
        assertNotNull(waiting.getCancelledAt());
        assertEquals(3, queue.cancelled().size());
        assertTrue(queue.pending().isEmpty());
        assertTrue(queue.processing().isEmpty());
        assertNull(queue.peekNext());
        assertEquals(1000, shortPaper.getStock());
    }

    @Test
    void cancelTwiceOrCancelCompletedOrCompletePendingThrows() {
        JobOrder job = paid("A", false);
        assertThrows(IllegalStateException.class, () -> queue.complete(job)); // still pending
        queue.startNext();
        queue.complete(job);
        assertThrows(IllegalStateException.class, () -> queue.cancel(job));
        JobOrder other = paid("B", false);
        queue.cancel(other);
        assertThrows(IllegalStateException.class, () -> queue.cancel(other));
    }

    @Test
    void markPaidTwiceOrOnCancelledJobThrows() {
        JobOrder job = unpaid("A", false);
        queue.markPaid(job);
        assertThrows(IllegalStateException.class, () -> queue.markPaid(job));
        JobOrder cancelled = unpaid("B", false);
        queue.cancel(cancelled);
        assertThrows(IllegalStateException.class, () -> queue.markPaid(cancelled));
    }

    @Test
    void autoDispatchStartsPaidJobWhenSlotFree() {
        settings.set("autoDispatch", "true");
        JobOrder first = paid("A", false);
        JobOrder second = paid("B", false);
        assertEquals(JobStatus.PROCESSING, first.getStatus());
        assertEquals(JobStatus.PENDING, second.getStatus());
        queue.complete(first);
        assertEquals(JobStatus.PROCESSING, second.getStatus());
    }

    @Test
    void autoDispatchNeverStartsUnpaidJobAndStartsItOncePaid() {
        settings.set("autoDispatch", "true");
        JobOrder online = unpaid("Online", false);
        assertEquals(JobStatus.PENDING, online.getStatus());
        queue.markPaid(online);
        assertEquals(JobStatus.PROCESSING, online.getStatus());
    }

    @Test
    void printerSlotsSettingAllowsMoreThanOneProcessingJob() {
        settings.set("autoDispatch", "true");
        settings.set("printerSlots", "2");
        paid("A", false);
        paid("B", false);
        JobOrder third = paid("C", false);
        assertEquals(2, queue.processing().size());
        assertEquals(JobStatus.PENDING, third.getStatus());
    }

    @Test
    void manualStartIgnoresSlots() {
        JobOrder a = paid("A", false);
        JobOrder b = paid("B", false);
        assertSame(a, queue.startNext());
        assertSame(b, queue.startNext());
        assertEquals(2, queue.processing().size());
    }

    @Test
    void expireUnpaidCancelsOnlyOldUnpaidOnlineJobs() {
        JobOrder oldOnline = unpaid("Online", false);
        JobOrder paidOnline = queue.submit("P", Service.BW, PaperSize.SHORT, 1, 1, false, JobSource.ONLINE,
                PaymentStatus.PAID, "uploads/b.pdf", 2, null);
        JobOrder walkInUnpaid = queue.submit("W", Service.BW, PaperSize.SHORT, 1, 1, false, JobSource.WALK_IN,
                PaymentStatus.UNPAID, null, 2, null);
        assertEquals(0, queue.expireUnpaid(LocalDateTime.now().plusMinutes(29)));
        assertEquals(1, queue.expireUnpaid(LocalDateTime.now().plusMinutes(31)));
        assertEquals(JobStatus.CANCELLED, oldOnline.getStatus());
        assertEquals(JobStatus.PENDING, paidOnline.getStatus());
        assertEquals(JobStatus.PENDING, walkInUnpaid.getStatus());
    }

    @Test
    void invalidQuantitiesRejected() {
        assertThrows(IllegalArgumentException.class, () -> queue.submit("A", Service.BW, PaperSize.A4, 0, 1, false,
                JobSource.WALK_IN, PaymentStatus.PAID, null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> queue.submit("A", Service.BW, PaperSize.A4, 1, -1, false,
                JobSource.WALK_IN, PaymentStatus.PAID, null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> queue.submit("A", Service.BW, PaperSize.A4, 100000, 100000,
                false, JobSource.WALK_IN, PaymentStatus.PAID, null, 0, null));
        assertTrue(queue.pending().isEmpty());
    }

    @Test
    void blankCustomerBecomesWalkIn() {
        assertEquals("Walk-in", paid("  ", false).getCustomer());
        assertEquals("Walk-in", queue.submit(null, Service.BW, PaperSize.A4, 1, 1, false, JobSource.WALK_IN,
                PaymentStatus.PAID, null, 2, null).getCustomer());
    }

    @Test
    void jobNumbersAndIdsCountUp() {
        assertEquals("JOB-0001", queue.nextId());
        JobOrder a = paid("A", false);
        assertEquals("JOB-0001", a.getId());
        assertEquals("JOB-0002", queue.nextId());
    }

    @Test
    void queueSurvivesRestartWithSameOrderStatusAndPayment() {
        JobOrder normal = paid("Normal", false);
        JobOrder rush = paid("Rush", true);
        JobOrder online = unpaid("Online", false);
        queue.startNext(); // rush starts
        JobOrder done = paid("Done", false);
        queue.startNext(); // normal starts
        queue.startNext(); // done starts
        queue.complete(done);
        JobOrder reopenedSource = paid("Next", false);

        JobQueueService reopened = new JobQueueService(db, inv, new StockEstimator(inv, settings), settings);
        assertEquals(2, reopened.processing().size());
        assertEquals(List.of(done.getNumber()), reopened.completed().stream().map(JobOrder::getNumber).toList());
        assertEquals(List.of(online.getNumber(), reopenedSource.getNumber()),
                reopened.pending().stream().map(JobOrder::getNumber).toList());
        assertEquals(reopenedSource.getNumber(), reopened.peekNext().getNumber());
        JobOrder loadedOnline = reopened.pending().get(0);
        assertEquals(PaymentStatus.UNPAID, loadedOnline.getPaymentStatus());
        assertEquals(JobSource.ONLINE, loadedOnline.getSource());
        assertEquals("uploads/a.pdf", loadedOnline.getFilePath());
        assertEquals("JOB-0006", reopened.nextId());
        assertTrue(normal.getNumber() > 0 && rush.getNumber() > 0);
    }

    @Test
    void discountAndTotalSurviveRestart() {
        JobOrder job = queue.submit("A", Service.BW, PaperSize.SHORT, 10, 1, false, JobSource.ONLINE,
                PaymentStatus.UNPAID, null, 18, "Student 10%", 2);
        JobQueueService reopened = new JobQueueService(db, inv, new StockEstimator(inv, settings), settings);
        JobOrder loaded = reopened.pending().get(0);
        assertEquals(job.getNumber(), loaded.getNumber());
        assertEquals(18, loaded.getTotal(), 0.0001);
        assertEquals(2, loaded.getDiscountAmount(), 0.0001);
        assertEquals("Student 10%", loaded.getDiscountName());
    }

    @Test
    void allListsNewestNumberFirst() {
        JobOrder a = paid("A", false);
        JobOrder b = paid("B", false);
        queue.cancel(a);
        assertEquals(List.of(b, a), queue.all());
        assertEquals(List.of(b), queue.activeJobs().stream().toList());
    }
}
