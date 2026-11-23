package com.printhub.service;

import com.printhub.model.JobOrder;
import com.printhub.model.JobStatus;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Transaction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Customer history worked out from the jobs and payments already on record (there is no separate customer
 * table): a customer is a name, matched ignoring capital letters.
 */
public class CustomerService {

    /** One customer: how many jobs they have placed, what they actually paid, and when they last came. */
    public record CustomerSummary(String name, int jobs, double spent, LocalDateTime lastVisit) {
    }

    private final JobQueueService queue;
    private final PaymentService payments;

    public CustomerService(JobQueueService queue, PaymentService payments) {
        this.queue = queue;
        this.payments = payments;
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    /** Customers whose name contains the text (blank for everybody), most recent visit first. Cancelled jobs do not count. */
    public List<CustomerSummary> summaries(String query) {
        String q = key(query);
        Map<String, CustomerSummary> byName = new LinkedHashMap<>();
        for (JobOrder j : queue.all()) { // newest job first
            if (j.getStatus() == JobStatus.CANCELLED || (!q.isEmpty() && !key(j.getCustomer()).contains(q))) {
                continue;
            }
            Transaction t = payments.transactionFor(j);
            double paid = t != null && t.getStatus() == PaymentStatus.PAID ? t.getAmount() : 0;
            CustomerSummary old = byName.get(key(j.getCustomer()));
            if (old == null) {
                byName.put(key(j.getCustomer()), new CustomerSummary(j.getCustomer(), 1, paid, j.getCreated()));
            } else {
                byName.put(key(j.getCustomer()), new CustomerSummary(old.name(), old.jobs() + 1, old.spent() + paid,
                        old.lastVisit()));
            }
        }
        return new ArrayList<>(byName.values());
    }

    /** Every job of this customer (any status), newest first. */
    public List<JobOrder> jobsOf(String name) {
        List<JobOrder> list = new ArrayList<>();
        for (JobOrder j : queue.all()) {
            if (key(j.getCustomer()).equals(key(name))) {
                list.add(j);
            }
        }
        return list;
    }
}
