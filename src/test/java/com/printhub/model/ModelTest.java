package com.printhub.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ModelTest {

    @Test
    void rushJobSortsBeforeEarlierNormalJob() {
        JobOrder normal = new JobOrder(1, "A", Service.BW, PaperSize.A4, 1, 1, false, LocalTime.of(9, 0));
        JobOrder rush = new JobOrder(2, "B", Service.BW, PaperSize.A4, 1, 1, true, LocalTime.of(9, 5));
        assertTrue(rush.compareTo(normal) < 0);
        assertTrue(normal.compareTo(rush) > 0);
    }

    @Test
    void newJobDefaultsToPaidWalkInPending() {
        JobOrder job = new JobOrder(3, "C", Service.BW, PaperSize.A4, 1, 1, false, LocalTime.of(9, 0));
        assertEquals(PaymentStatus.PAID, job.getPaymentStatus());
        assertEquals(JobSource.WALK_IN, job.getSource());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(LocalTime.of(9, 0), job.getReceived());
    }

    @Test
    void discountNeverExceedsSubtotal() {
        assertEquals(120, new Discount("Big", Discount.Kind.FIXED, 500).amountOff(120), 0.0001);
        assertEquals(20, new Discount("Student", Discount.Kind.PERCENT, 10).amountOff(200), 0.0001);
        assertEquals(0, new Discount("Zero", Discount.Kind.FIXED, -5).amountOff(100), 0.0001);
    }

    @Test
    void staffHasNoAdminPermissions() {
        for (Permission p : Permission.values()) {
            assertFalse(Role.STAFF.allows(p));
        }
    }

    @Test
    void adminHasAllPermissions() {
        for (Permission p : Permission.values()) {
            assertTrue(Role.ADMIN.allows(p));
        }
    }

    @Test
    void supplyStatusUnchanged() {
        assertEquals("Low", new Supply("S1", "Bond", "Paper", "Short", "sheets", 20, 50).status());
        assertEquals("Out", new Supply("S2", "Bond", "Paper", "Short", "sheets", 0, 50).status());
        assertEquals("OK", new Supply("S3", "Bond", "Paper", "Short", "sheets", 60, 50).status());
    }

    @Test
    void receiptContainsTotalAndChange() {
        JobOrder job = new JobOrder(231, "Juan Dela Cruz", Service.COLORED, PaperSize.A4, 12, 5, true,
                LocalTime.of(10, 42));
        job.setTotal(620);
        Transaction t = new Transaction("TXN-0417", LocalDateTime.of(2026, 11, 24, 10, 42), "Juan Dela Cruz",
                job.getId(), 620, PaymentMethod.CASH, null, PaymentStatus.PAID, 0, 700, 80, "lmendoza");
        String text = Receipt.of(t, job).text();
        assertTrue(text.contains("₱620.00"), text);
        assertTrue(text.contains("₱80.00"), text);
        assertTrue(text.contains("lmendoza"), text);
        assertTrue(text.contains("JOB-0231"), text);
    }
}
