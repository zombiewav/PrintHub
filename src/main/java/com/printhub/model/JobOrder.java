package com.printhub.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * One print job. Natural order is queue order: rush jobs first, then first come, first served
 * (the job number grows with every order), so a heap of JobOrder always hands out
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
    private final LocalDateTime created;
    private JobStatus status = JobStatus.PENDING;
    private PaymentStatus paymentStatus = PaymentStatus.PAID;
    private JobSource source = JobSource.WALK_IN;
    private String filePath;
    private double total;
    private String discountName;
    private double discountAmount;
    private String detail;
    private LocalDateTime cancelledAt;

    /** A job received today at the given time. */
    public JobOrder(int number, String customer, Service service, PaperSize size,
            int pages, int copies, boolean rush, LocalTime received) {
        this(number, customer, service, size, pages, copies, rush, LocalDate.now().atTime(received));
    }

    public JobOrder(int number, String customer, Service service, PaperSize size,
            int pages, int copies, boolean rush, LocalDateTime created) {
        this.number = number;
        this.customer = customer;
        this.service = service;
        this.size = size;
        this.pages = pages;
        this.copies = copies;
        this.rush = rush;
        this.created = created;
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
        return created.toLocalTime();
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public JobSource getSource() {
        return source;
    }

    public void setSource(JobSource source) {
        this.source = source;
    }

    /** Path of the customer's uploaded file, or null for walk-in jobs. */
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /** What the customer owes (or paid) in pesos, after any discount. */
    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    /** Pesos taken off the subtotal by the discount (0 when there is none). */
    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getDiscountName() {
        return discountName;
    }

    public void setDiscountName(String discountName) {
        this.discountName = discountName;
    }

    /** Extra detail the service needs: ID picture size and count, or the binding type. Null for other services. */
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    @Override
    public String toString() {
        return getId() + " · " + customer;
    }
}
