package com.printhub.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.printhub.db.Database;
import com.printhub.db.Settings;
import com.printhub.model.JobOrder;
import com.printhub.model.JobSource;
import com.printhub.model.JobStatus;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentMethod;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Service;
import com.printhub.model.Supply;
import com.printhub.service.InventoryService;
import com.printhub.service.JobQueueService;
import com.printhub.service.OrderService;
import com.printhub.service.PaymentService;
import com.printhub.service.PricingService;
import com.printhub.service.StockEstimator;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PortalServerTest {

    private static final String BOUNDARY = "----PortalTestBoundary";

    private JobQueueService queue;
    private OrderService orders;
    private InventoryService inventory;
    private PricingService pricing;
    private Path uploads;
    private PortalServer server;
    private HttpClient http;
    private String base;
    private final AtomicInteger changes = new AtomicInteger();

    @BeforeEach
    void setUp() throws Exception {
        Database db = Database.inMemory();
        Settings settings = new Settings(db);
        settings.set("autoDispatch", "false");
        inventory = new InventoryService(db);
        inventory.add(new Supply("SUP-001", "Bond Paper", "Paper", "A4", "sheets", 1250, 500));
        inventory.add(new Supply("SUP-002", "Bond Paper", "Paper", "Short", "sheets", 1000, 500));
        inventory.add(new Supply("SUP-004", "Black Ink (Epson 003)", "Ink", Supply.NO_SIZE, "pages", 2400, 1000));
        inventory.add(new Supply("SUP-005", "Color Ink Set (Epson 003)", "Ink", Supply.NO_SIZE, "pages", 420, 500));
        StockEstimator estimator = new StockEstimator(inventory, settings);
        queue = new JobQueueService(db, inventory, estimator, settings);
        PaymentService payments = new PaymentService(db);
        pricing = new PricingService(db);
        pricing.prices().set(Service.BW, PaperSize.A4, 2);
        pricing.prices().set(Service.BW, PaperSize.SHORT, 2);
        pricing.prices().set(Service.COLORED, PaperSize.A4, 10);
        orders = new OrderService(db, queue, payments, pricing, estimator);
        uploads = Files.createTempDirectory("printhub-portal-test");
        http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        start(new PortalServer.Limits(5_000_000, 300, 50, 600_000));
    }

    private void start(PortalServer.Limits limits) throws Exception {
        if (server != null) {
            server.stop();
        }
        server = new PortalServer(orders, queue, pricing, uploads, limits, changes::incrementAndGet);
        server.start(0);
        base = "http://localhost:" + server.port();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
        try (Stream<Path> files = Files.walk(uploads)) {
            files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }

    // ---- helpers ----------------------------------------------------------------------------

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private Map<String, String> form(String service, String size, String copies) {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("name", "Kim Lopez");
        f.put("service", service);
        f.put("size", size);
        f.put("copies", copies);
        return f;
    }

    private HttpResponse<String> upload(Map<String, String> fields, String filename, byte[] file) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            out.write(("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n"
                    + e.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        if (file != null) {
            out.write(("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename
                    + "\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(file);
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build();
        return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private long storedFiles() throws Exception {
        try (Stream<Path> files = Files.list(uploads)) {
            return files.count();
        }
    }

    // ---- tests ------------------------------------------------------------------------------

    @Test
    void theFormPageListsTheServicesSizesAndPricesAndIsSafelyServed() throws Exception {
        HttpResponse<String> r = get("/");
        assertEquals(200, r.statusCode());
        assertEquals("text/html; charset=utf-8", r.headers().firstValue("Content-Type").get());
        assertEquals("nosniff", r.headers().firstValue("X-Content-Type-Options").get());
        String page = r.body();
        assertTrue(page.contains("B&amp;W Printing"), page);
        assertTrue(page.contains("Colored Printing"));
        assertTrue(page.contains("A4") && page.contains("Short") && page.contains("Long"));
        assertTrue(page.contains("multipart/form-data"));
        assertFalse(page.contains("Lamination"));
    }

    @Test
    void uploadingAPdfCreatesAnUnpaidOnlineJobAndRedirectsToItsTicket() throws Exception {
        HttpResponse<String> r = upload(form("BW", "A4", "2"), "thesis.pdf", PdfSupport.pdf(3));
        assertEquals(303, r.statusCode());
        assertEquals("/ticket?no=JOB-0001", r.headers().firstValue("Location").get());

        JobOrder job = queue.all().get(0);
        assertEquals(JobSource.ONLINE, job.getSource());
        assertEquals(PaymentStatus.UNPAID, job.getPaymentStatus());
        assertEquals("Kim Lopez", job.getCustomer());
        assertEquals(3, job.getPages()); // counted from the file, not typed by the customer
        assertEquals(2, job.getCopies());
        assertEquals(12, job.getTotal(), 0.0001);
        Path saved = Path.of(job.getFilePath());
        assertTrue(Files.exists(saved));
        assertEquals(uploads.toRealPath(), saved.toRealPath().getParent());
        assertTrue(saved.getFileName().toString().endsWith(".pdf"));
        assertFalse(saved.getFileName().toString().contains("thesis"));
        assertEquals(1, changes.get());

        HttpResponse<String> ticket = get("/ticket?no=JOB-0001");
        assertEquals(200, ticket.statusCode());
        assertTrue(ticket.body().contains("JOB-0001"), ticket.body());
        assertTrue(ticket.body().contains("₱12.00"), ticket.body());
        assertTrue(ticket.body().contains("Please pay"), ticket.body());
        assertFalse(ticket.body().contains("Kim Lopez"));
    }

    @Test
    void theRushOptionAddsTheRushFeeOnTheServer() throws Exception {
        Map<String, String> f = form("BW", "A4", "1");
        f.put("rush", "on");
        assertEquals(303, upload(f, "a.pdf", PdfSupport.pdf(3)).statusCode());
        JobOrder job = queue.all().get(0);
        assertTrue(job.isRush());
        assertEquals(3 * 2 + 20, job.getTotal(), 0.0001);
    }

    @Test
    void aBlankNameBecomesOnlineCustomer() throws Exception {
        Map<String, String> f = form("BW", "A4", "1");
        f.put("name", "   ");
        assertEquals(303, upload(f, "a.pdf", PdfSupport.pdf(1)).statusCode());
        assertEquals("Online customer", queue.all().get(0).getCustomer());
    }

    @Test
    void theTicketShowsTheQueuePositionOncePaidAndThenFollowsTheJob() throws Exception {
        upload(form("BW", "A4", "1"), "a.pdf", PdfSupport.pdf(1));
        JobOrder job = queue.all().get(0);

        orders.markPaid(job, PaymentMethod.CASH, 2, null);
        String queued = get("/ticket?no=" + job.getId()).body();
        assertTrue(queued.contains("Position 1"), queued);

        // a walk-in rush order goes ahead of it
        orders.placeWalkIn("Walk-in", Service.BW, PaperSize.A4, 1, 1, true, null, PaymentMethod.CASH, 100, null);
        assertTrue(get("/ticket?no=" + job.getId()).body().contains("Position 2"));

        queue.startNext(); // the rush walk-in starts
        queue.startNext(); // then the online job
        assertTrue(get("/ticket?no=" + job.getId()).body().contains("Printing now"));

        queue.complete(job);
        assertTrue(get("/ticket?no=" + job.getId()).body().contains("Done"));
    }

    @Test
    void aCancelledOrderSaysSoAndItsFileIsRemoved() throws Exception {
        upload(form("BW", "A4", "1"), "a.pdf", PdfSupport.pdf(1));
        JobOrder job = queue.all().get(0);
        orders.cancel(job);
        assertTrue(get("/ticket?no=" + job.getId()).body().contains("cancelled"));
        assertEquals(0, storedFiles());
    }

    @Test
    void aFileThatIsNotAPdfIsRefusedAndNothingIsKept() throws Exception {
        HttpResponse<String> r = upload(form("BW", "A4", "1"), "notes.pdf", "just some text".getBytes(StandardCharsets.UTF_8));
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("not a valid PDF"), r.body());
        assertTrue(queue.all().isEmpty());
        assertEquals(0, storedFiles());
    }

    @Test
    void noFileAtAllIsRefused() throws Exception {
        HttpResponse<String> r = upload(form("BW", "A4", "1"), null, null);
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("Choose a PDF"), r.body());
    }

    @Test
    void aPdfWithMoreThanTheAllowedPagesIsRefused() throws Exception {
        start(new PortalServer.Limits(5_000_000, 5, 50, 600_000));
        HttpResponse<String> r = upload(form("BW", "A4", "1"), "big.pdf", PdfSupport.pdf(6));
        assertEquals(400, r.statusCode());
        assertTrue(r.body().contains("6 pages") && r.body().contains("5"), r.body());
        assertEquals(0, storedFiles());
        assertTrue(queue.all().isEmpty());
    }

    @Test
    void anUploadOverTheSizeLimitIsRefused() throws Exception {
        start(new PortalServer.Limits(20_000, 300, 50, 600_000));
        byte[] big = new byte[60_000];
        System.arraycopy("%PDF-1.4".getBytes(StandardCharsets.US_ASCII), 0, big, 0, 8);
        HttpResponse<String> r = upload(form("BW", "A4", "1"), "big.pdf", big);
        assertEquals(413, r.statusCode());
        assertTrue(queue.all().isEmpty());
        assertEquals(0, storedFiles());
    }

    @Test
    void servicesSizesAndCopiesAreCheckedOnTheServer() throws Exception {
        byte[] pdf = PdfSupport.pdf(1);
        assertEquals(400, upload(form("LAMINATION", "A4", "1"), "a.pdf", pdf).statusCode());
        assertEquals(400, upload(form("BW", "HUGE", "1"), "a.pdf", pdf).statusCode());
        assertEquals(400, upload(form("BW", "A4", "0"), "a.pdf", pdf).statusCode());
        assertEquals(400, upload(form("BW", "A4", "101"), "a.pdf", pdf).statusCode());
        assertEquals(400, upload(form("BW", "A4", "lots"), "a.pdf", pdf).statusCode());
        assertTrue(queue.all().isEmpty());
        assertEquals(0, storedFiles());
    }

    @Test
    void whenStockIsShortTheCustomerIsToldAndNothingIsKept() throws Exception {
        // 5 pages x 100 copies = 500 pages of color ink, but only 420 are left
        HttpResponse<String> r = upload(form("COLORED", "A4", "100"), "photos.pdf", PdfSupport.pdf(5));
        assertEquals(422, r.statusCode());
        assertTrue(r.body().contains("Not enough stock"), r.body());
        assertTrue(queue.all().isEmpty());
        assertEquals(0, storedFiles());
    }

    @Test
    void aPhoneCanOnlyPlaceSoManyOrdersInAWhile() throws Exception {
        start(new PortalServer.Limits(5_000_000, 300, 2, 600_000));
        byte[] pdf = PdfSupport.pdf(1);
        assertEquals(303, upload(form("BW", "A4", "1"), "a.pdf", pdf).statusCode());
        assertEquals(303, upload(form("BW", "A4", "1"), "a.pdf", pdf).statusCode());
        HttpResponse<String> third = upload(form("BW", "A4", "1"), "a.pdf", pdf);
        assertEquals(429, third.statusCode());
        assertEquals(2, queue.all().size());
    }

    @Test
    void aFilenameCannotEscapeTheUploadFolder() throws Exception {
        assertEquals(303, upload(form("BW", "A4", "1"), "../../evil.pdf", PdfSupport.pdf(1)).statusCode());
        assertEquals(1, storedFiles());
        assertFalse(Files.exists(uploads.getParent().resolve("evil.pdf")));
    }

    @Test
    void unknownOrBadTicketsAndPagesAreNotFound() throws Exception {
        assertEquals(404, get("/ticket?no=JOB-9999").statusCode());
        assertEquals(404, get("/ticket?no=../../etc/passwd").statusCode());
        assertEquals(404, get("/ticket").statusCode());
        assertEquals(404, get("/nothing-here").statusCode());
    }

    @Test
    void ticketsOfCounterOrdersAreNotShownToCustomers() throws Exception {
        JobOrder walkIn = orders.placeWalkIn("Maria", Service.BW, PaperSize.A4, 1, 1, false, null, PaymentMethod.CASH, 100, null);
        assertEquals(404, get("/ticket?no=" + walkIn.getId()).statusCode());
    }

    @Test
    void wrongMethodsAreRefused() throws Exception {
        assertEquals(405, get("/upload").statusCode());
        HttpResponse<String> post = http.send(HttpRequest.newBuilder(URI.create(base + "/"))
                .POST(HttpRequest.BodyPublishers.ofString("x")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(405, post.statusCode());
    }

    @Test
    void textIsEscapedBeforeItGoesIntoAPage() {
        assertEquals("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt; &amp; &#39;", PortalPages.escape("<script>alert(\"x\")</script> & '"));
        assertEquals("", PortalPages.escape(null));
    }

    @Test
    void theServerStopsCleanly() throws Exception {
        assertTrue(server.isRunning());
        server.stop();
        assertFalse(server.isRunning());
        assertThrows(java.io.IOException.class, () -> get("/"));
        start(new PortalServer.Limits(5_000_000, 300, 50, 600_000)); // so tearDown has a server to stop
    }

    @Test
    void anOrderStillWorksAfterAnEarlierRefusal() throws Exception {
        assertEquals(400, upload(form("BW", "A4", "1"), "a.pdf", "nope".getBytes(StandardCharsets.UTF_8)).statusCode());
        assertEquals(303, upload(form("BW", "A4", "1"), "a.pdf", PdfSupport.pdf(2)).statusCode());
        assertEquals(1, queue.all().size());
        assertEquals(JobStatus.PENDING, queue.all().get(0).getStatus());
    }
}
