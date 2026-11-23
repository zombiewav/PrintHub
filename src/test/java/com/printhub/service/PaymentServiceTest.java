package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.model.Account;
import com.printhub.model.JobOrder;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Role;
import com.printhub.model.Service;
import com.printhub.model.Transaction;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private Database db;
    private PaymentService payments;
    private int jobNo = 1;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        payments = new PaymentService(db);
        Session.set(new Account("lmendoza", "x", "Liza Mendoza", Role.STAFF));
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    private JobOrder job(double total) {
        JobOrder j = new JobOrder(jobNo++, "Customer " + jobNo, Service.BW, PaperSize.A4, 1, 1, false, LocalTime.of(9, 0));
        j.setTotal(total);
        return j;
    }

    @Test
    void cashChangeIsExactToCentavo() {
        Transaction t = payments.record(job(0.30), PaymentMethod.CASH, 1.00, null);
        assertEquals(0.30, t.getAmount(), 0.00001);
        assertEquals(1.00, t.getTendered(), 0.00001);
        assertEquals(0.70, t.getChange(), 0.00001);
        assertEquals(PaymentStatus.PAID, t.getStatus());
    }

    @Test
    void exactCashGivesNoChange() {
        assertEquals(0, payments.record(job(620), PaymentMethod.CASH, 620, null).getChange(), 0.00001);
    }

    @Test
    void cashBelowTotalRejectedAndNothingRecorded() {
        assertThrows(IllegalArgumentException.class, () -> payments.record(job(620), PaymentMethod.CASH, 619.99, null));
        assertTrue(payments.transactions().isEmpty());
    }

    @Test
    void gcashNeedsAReferenceAndKeepsIt() {
        assertThrows(IllegalArgumentException.class, () -> payments.record(job(180), PaymentMethod.GCASH, 0, " "));
        assertThrows(IllegalArgumentException.class, () -> payments.record(job(180), PaymentMethod.GCASH, 0, null));
        Transaction t = payments.record(job(180), PaymentMethod.GCASH, 0, "4021 553 981 204");
        assertEquals("4021 553 981 204", t.getReference());
        assertEquals(0, t.getChange(), 0.00001);
    }

    @Test
    void recordingTheSameJobTwiceIsRejected() {
        JobOrder j = job(100);
        payments.record(j, PaymentMethod.CASH, 100, null);
        assertThrows(IllegalStateException.class, () -> payments.record(j, PaymentMethod.CASH, 100, null));
        assertEquals(1, payments.transactions().size());
    }

    @Test
    void transactionRecordsHandlingStaffAndDiscount() {
        JobOrder j = job(18);
        j.setDiscountAmount(2);
        Transaction t = payments.record(j, PaymentMethod.CASH, 20, null);
        assertEquals("lmendoza", t.getHandledBy());
        assertEquals(2, t.getDiscount(), 0.00001);
        assertSame(t, payments.transactionFor(j));
    }

    @Test
    void idsCountUpAndSurviveRestart() {
        assertEquals("TXN-0001", payments.record(job(10), PaymentMethod.CASH, 10, null).getId());
        assertEquals("TXN-0002", payments.record(job(10), PaymentMethod.CASH, 10, null).getId());
        PaymentService reopened = new PaymentService(db);
        assertEquals(2, reopened.transactions().size());
        assertEquals("TXN-0003", reopened.record(job(10), PaymentMethod.CASH, 10, null).getId());
        assertEquals("TXN-0003", reopened.transactions().get(0).getId()); // newest first
    }

    @Test
    void dailySummarySeparatesCashAndGcash() {
        payments.record(job(1250), PaymentMethod.CASH, 1250, null);
        payments.record(job(850), PaymentMethod.GCASH, 0, "REF-1");
        PaymentService.DailySummary s = payments.dailySummary(LocalDate.now());
        assertEquals(1250, s.cash(), 0.00001);
        assertEquals(850, s.gcash(), 0.00001);
        assertEquals(2100, s.total(), 0.00001);
        assertEquals(2, s.count());
    }

    @Test
    void voidedTransactionStaysListedButIsExcludedFromSummary() {
        JobOrder j = job(500);
        payments.record(j, PaymentMethod.CASH, 500, null);
        payments.record(job(100), PaymentMethod.CASH, 100, null);
        payments.voidFor(j);
        assertEquals(PaymentStatus.VOID, payments.transactionFor(j).getStatus());
        assertEquals(2, payments.on(LocalDate.now()).size());
        PaymentService.DailySummary s = payments.dailySummary(LocalDate.now());
        assertEquals(100, s.total(), 0.00001);
        assertEquals(1, s.count());
        assertEquals(PaymentStatus.VOID, new PaymentService(db).transactionFor(j).getStatus());
    }

    @Test
    void voidForUnpaidJobDoesNothing() {
        payments.voidFor(job(10));
        assertTrue(payments.transactions().isEmpty());
        assertNull(payments.transactionFor(job(10)));
    }

    @Test
    void dailyHistoryOnlyHasThatDay() {
        db.update("INSERT INTO payment(id,time,customer,job_id,amount_centavos,method,reference,status,handled_by)"
                + " VALUES('TXN-0410',?,'Old','JOB-0100',10000,'CASH',NULL,'PAID','system')",
                LocalDate.now().minusDays(1).atTime(9, 0).toString());
        PaymentService reopened = new PaymentService(db);
        reopened.record(job(10), PaymentMethod.CASH, 10, null);
        assertEquals(1, reopened.on(LocalDate.now()).size());
        assertEquals(1, reopened.on(LocalDate.now().minusDays(1)).size());
        assertEquals(10000 / 100.0, reopened.dailySummary(LocalDate.now().minusDays(1)).total(), 0.00001);
        assertEquals("TXN-0412", reopened.record(job(10), PaymentMethod.CASH, 10, null).getId());
    }

    @Test
    void receiptShowsTotalAndChange() {
        JobOrder j = job(620);
        Transaction t = payments.record(j, PaymentMethod.CASH, 700, null);
        String text = payments.receipt(t, j).text();
        assertTrue(text.contains("₱620.00") && text.contains("₱80.00"), text);
    }
}
