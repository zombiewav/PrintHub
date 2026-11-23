package com.printhub.model;

import java.time.LocalTime;

/**
 * One print job. Natural order is queue order: rush jobs first, then first come, first served
 * (the job number grows with every order), so a PriorityQueue of JobOrder always hands out
 * the right job next.
 */
public class JobOrder implements Comparable<JobOrder> {

    private final int number;
    private final String customer;
    private final Service service;
    private final PaperSize size;
    private final int pages;
    private final int copies;
    private final boolean rush;
    private final LocalTime received;
    private JobStatus status = JobStatus.PENDING;

    public JobOrder(int number, String customer, Service service, PaperSize size,
            int pages, int copies, boolean rush, LocalTime received) {
        this.number = number;
        this.customer = customer;
        this.service = service;
        this.size = size;
        this.pages = pages;
        this.copies = copies;
        this.rush = rush;
        this.received = received;
    }

    @Override
    public int compareTo(JobOrder other) {
        if (rush != other.rush) {
            return rush ? -1 : 1;
        }
        return Integer.compare(number, other.number);
    }

    public String getId() {
        return String.format("JOB-%04d", number);
    }

    public int sheets() {
        return pages * copies;
    }

    public String pagesCopies() {
        return pages + " × " + copies;
    }

    public int getNumber() {
        return number;
    }

    public String getCustomer() {
        return customer;
    }

    public Service getService() {
        return service;
    }

    public PaperSize getSize() {
        return size;
    }

    public int getPages() {
        return pages;
    }

    public int getCopies() {
        return copies;
    }

    public boolean isRush() {
        return rush;
    }

    public LocalTime getReceived() {
        return received;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return getId() + " · " + customer;
    }
}
