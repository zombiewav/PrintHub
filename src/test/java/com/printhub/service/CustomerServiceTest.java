package com.printhub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.db.Settings;
import com.printhub.model.JobOrder;
import com.printhub.model.JobSource;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Service;
import com.printhub.model.Supply;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerServiceTest {

    private JobQueueService queue;
    private PaymentService payments;
    private CustomerService customers;

    @BeforeEach
    void setUp() {
        Database db = Database.inMemory();
        Settings settings = new Settings(db);
        settings.set("autoDispatch", "false");
        InventoryService inv = new InventoryService(db);
        inv.add(new Supply("SUP-002", "Bond Paper", "Paper", "Short", "sheets", 1000, 100));
        queue = new JobQueueService(db, inv, new StockEstimator(inv, settings), settings);
        payments = new PaymentService(db);
        customers = new CustomerService(queue, payments);
    }

    private JobOrder job(String customer, double total, boolean pay) {
        JobOrder j = queue.submit(customer, Service.BW, PaperSize.SHORT, 1, 1, false, JobSource.WALK_IN,
                pay ? PaymentStatus.PAID : PaymentStatus.UNPAID, null, total, null);
        if (pay) {
            payments.record(j, PaymentMethod.CASH, total, null);
        }
        return j;
    }

    @Test
    void namesAreGroupedIgnoringCaseWithJobCountAndPaidTotal() {
        job("Maria Santos", 100, true);
        job("maria santos", 50, true);
        job("Pedro Bautista", 30, true);
        List<CustomerService.CustomerSummary> list = customers.summaries("");
        assertEquals(2, list.size());
        CustomerService.CustomerSummary maria = list.stream().filter(c -> c.name().equalsIgnoreCase("Maria Santos"))
                .findFirst().get();
        assertEquals(2, maria.jobs());
        assertEquals(150, maria.spent(), 0.0001);
    }

    @Test
    void unpaidAndCancelledJobsAreNotCountedAsSpent() {
        job("Maria", 100, true);
        job("Maria", 70, false); // unpaid online order
        JobOrder cancelled = job("Maria", 40, true);
        payments.voidFor(cancelled);
        queue.cancel(cancelled);
        CustomerService.CustomerSummary maria = customers.summaries("").get(0);
        assertEquals(2, maria.jobs()); // paid + unpaid, the cancelled one is not a visit
        assertEquals(100, maria.spent(), 0.0001);
    }

    @Test
    void mostRecentCustomerComesFirstAndSearchFilters() {
        job("Pedro Bautista", 30, true);
        job("Maria Santos", 100, true);
        assertEquals("Maria Santos", customers.summaries("").get(0).name());
        List<CustomerService.CustomerSummary> found = customers.summaries("pedro");
        assertEquals(1, found.size());
        assertEquals("Pedro Bautista", found.get(0).name());
        assertTrue(customers.summaries("nobody").isEmpty());
    }

    @Test
    void jobsOfListsEveryJobOfThatCustomerNewestFirstIncludingCancelled() {
        JobOrder a = job("Maria", 10, true);
        JobOrder b = job("MARIA", 20, true);
        job("Pedro", 30, true);
        queue.cancel(a);
        assertEquals(List.of(b, a), customers.jobsOf("maria"));
    }
}
