package com.printhub.data;

import com.printhub.db.Database;
import com.printhub.db.Settings;
import com.printhub.model.Account;
import com.printhub.model.Discount;
import com.printhub.model.JobOrder;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PriceTable;
import com.printhub.model.Receipt;
import com.printhub.model.Restock;
import com.printhub.model.Service;
import com.printhub.model.StockEstimate;
import com.printhub.model.Supplier;
import com.printhub.model.Supply;
import com.printhub.model.Transaction;
import com.printhub.model.WasteReason;
import com.printhub.model.WasteRecord;
import com.printhub.service.AccountService;
import com.printhub.service.InventoryService;
import com.printhub.service.JobQueueService;
import com.printhub.service.OrderService;
import com.printhub.service.PaymentService;
import com.printhub.service.PricingService;
import com.printhub.service.ReportService;
import com.printhub.service.StockEstimator;
import com.printhub.service.SupplierService;
import java.awt.EventQueue;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.Timer;

/**
 * The one door the screens use to reach the shop's data. It owns the SQLite database and the services
 * (inventory, queue, pricing, payments, suppliers, reports, accounts) and tells the screens when anything
 * changes so they can redraw.
 */
public final class PrintHubData {

    private static PrintHubData instance;

    /** The app-wide instance, backed by printhub.db in the folder the app was started from. */
    public static synchronized PrintHubData get() {
        if (instance == null) {
            String path = System.getProperty("user.dir") + File.separator + "printhub.db";
            instance = new PrintHubData(Database.open("jdbc:sqlite:" + path));
            instance.startBackgroundTasks();
        }
        return instance;
    }

    private final AccountService accounts;
    private final InventoryService inventory;
    private final PricingService pricing;
    private final SupplierService suppliers;
    private final JobQueueService queue;
    private final PaymentService payments;
    private final OrderService orders;
    private final ReportService reports;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    /** Opens the services on a database, loading the sample shop if it is brand new. */
    PrintHubData(Database db) {
        Seeder.seed(db);
        Settings settings = new Settings(db);
        this.accounts = new AccountService(db);
        this.inventory = new InventoryService(db);
        this.pricing = new PricingService(db);
        this.suppliers = new SupplierService(db, inventory);
        StockEstimator estimator = new StockEstimator(inventory, settings);
        this.queue = new JobQueueService(db, inventory, estimator, settings);
        this.payments = new PaymentService(db);
        this.orders = new OrderService(db, queue, payments, pricing, estimator);
        this.reports = new ReportService(payments, inventory, suppliers, queue);
    }

    /** Cancels unpaid online jobs that waited too long. Checked once a minute. */
    private void startBackgroundTasks() {
        new Timer(60_000, e -> {
            if (queue.expireUnpaid(LocalDateTime.now()) > 0) {
                changed();
            }
        }).start();
    }

    // ---- Change notification ----------------------------------------------------------------

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    /** Tells every screen to redraw. Always runs on the Swing thread. */
    public void changed() {
        if (EventQueue.isDispatchThread()) {
            fire();
        } else {
            EventQueue.invokeLater(this::fire);
        }
    }

    private void fire() {
        for (Runnable r : new ArrayList<>(listeners)) {
            r.run();
        }
    }

    // ---- Services for screens that need more than the shortcuts below -----------------------

    public ReportService reports() {
        return reports;
    }

    public AccountService accounts() {
        return accounts;
    }

    public PaymentService payments() {
        return payments;
    }

    // ---- Login ------------------------------------------------------------------------------

    public Account authenticate(String username, String password) {
        return accounts.authenticate(username, password);
    }

    public void logout() {
        accounts.logout();
    }

    // ---- Prices and discounts ---------------------------------------------------------------

    public PriceTable prices() {
        return pricing.prices();
    }

    /** Saves the price table after a screen has edited it. */
    public void savePrices() {
        pricing.save();
        changed();
    }

    public List<Discount> discounts() {
        return pricing.discounts();
    }

    // ---- Supplies ---------------------------------------------------------------------------

    public List<Supply> supplies() {
        return inventory.supplies();
    }

    public Supply findSupply(String id) {
        return inventory.find(id);
    }

    public String newSupplyId() {
        return inventory.newId();
    }

    public void addSupply(Supply supply) {
        inventory.add(supply);
        changed();
    }

    /** Saves a supply a screen has edited. */
    public void updateSupply(Supply supply) {
        inventory.update(supply);
        changed();
    }

    public void removeSupply(String id) {
        inventory.delete(id);
        changed();
    }

    public void restock(Supply supply, int qty) {
        inventory.restock(supply, qty);
        changed();
    }

    public List<Supply> lowStock() {
        return inventory.lowStock();
    }

    // ---- Suppliers --------------------------------------------------------------------------

    public List<Supplier> supplierList() {
        return suppliers.all();
    }

    public Supplier addSupplier(String name, String address, String phone, Collection<Supply> provides) {
        Supplier s = suppliers.add(name, address, phone, provides);
        changed();
        return s;
    }

    public void updateSupplier(Supplier supplier) {
        suppliers.update(supplier);
        changed();
    }

    public void deleteSupplier(String id) {
        suppliers.delete(id);
        changed();
    }

    /** Buys stock from a supplier and records the purchase. */
    public Restock restockFrom(Supplier supplier, Supply supply, int qty, double totalCost) {
        Restock r = suppliers.restock(supplier, supply, qty, totalCost);
        changed();
        return r;
    }

    public List<Restock> supplierHistory(Supplier supplierOrNull) {
        return suppliers.history(supplierOrNull);
    }

    public double supplierExpenses(Supplier supplierOrNull) {
        return suppliers.totalExpenses(supplierOrNull);
    }

    /** Suppliers that provide an item, for the reorder reminder. */
    public List<Supplier> suppliersFor(Supply supply) {
        return suppliers.suppliersFor(supply);
    }

    // ---- Orders and the job queue -----------------------------------------------------------

    public String nextOrderId() {
        return queue.nextId();
    }

    /** Checks paper (by size) and ink against what is on the shelf, minus what queued jobs have claimed. */
    public StockEstimate estimate(Service service, PaperSize size, int quantity) {
        return orders.estimate(service, size, quantity);
    }

    /** Takes a counter order with no discount; cash is taken as exactly the total. */
    public JobOrder placeOrder(String customer, Service service, PaperSize size, int pages, int copies,
            boolean rush, PaymentMethod method, String reference) {
        double total = orders.quote(service, size, pages, copies, rush, null);
        return placeOrder(customer, service, size, pages, copies, rush, null, method, total, reference);
    }

    public JobOrder placeOrder(String customer, Service service, PaperSize size, int pages, int copies,
            boolean rush, Discount discount, PaymentMethod method, double tendered, String reference) {
        JobOrder job = orders.placeWalkIn(customer, service, size, pages, copies, rush, discount, method, tendered,
                reference);
        changed();
        return job;
    }

    /** Staff confirms a waiting job has been paid so it can be printed. */
    public Transaction markPaid(JobOrder job, PaymentMethod method, double tendered, String reference) {
        Transaction t = orders.markPaid(job, method, tendered, reference);
        changed();
        return t;
    }

    public void cancelJob(JobOrder job) {
        orders.cancel(job);
        changed();
    }

    /** Pending jobs in queue order (rush first, then first come first served); unpaid ones are included. */
    public List<JobOrder> pendingJobs() {
        return queue.pending();
    }

    /** The job that would start next: the first paid one. */
    public JobOrder peekNext() {
        return queue.peekNext();
    }

    public List<JobOrder> processingJobs() {
        return queue.processing();
    }

    public List<JobOrder> completedJobs() {
        return queue.completed();
    }

    public List<JobOrder> cancelledJobs() {
        return queue.cancelled();
    }

    public List<JobOrder> allJobs() {
        return queue.all();
    }

    public JobOrder startNextJob() {
        JobOrder job = queue.startNext();
        if (job != null) {
            changed();
        }
        return job;
    }

    public void completeJob(JobOrder job) {
        queue.complete(job);
        changed();
    }

    // ---- Waste ------------------------------------------------------------------------------

    public List<WasteRecord> wasteLog() {
        return inventory.wasteLog();
    }

    /** Deducts the wasted quantity from stock and writes it to the waste log. */
    public void recordWaste(Supply supply, int qty, WasteReason reason, String jobId) {
        inventory.recordWaste(supply, qty, reason, jobId);
        changed();
    }

    // ---- Sales ------------------------------------------------------------------------------

    public List<Transaction> transactions() {
        return payments.transactions();
    }

    public Transaction transactionFor(JobOrder job) {
        return payments.transactionFor(job);
    }

    /** The receipt for a transaction, or null if its job cannot be found. */
    public Receipt receipt(Transaction transaction) {
        for (JobOrder j : queue.all()) {
            if (j.getId().equals(transaction.getJobId())) {
                return payments.receipt(transaction, j);
            }
        }
        return null;
    }
}
