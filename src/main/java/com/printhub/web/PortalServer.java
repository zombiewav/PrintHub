package com.printhub.web;

import com.printhub.model.JobOrder;
import com.printhub.model.JobSource;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Service;
import com.printhub.service.JobQueueService;
import com.printhub.service.OrderService;
import com.printhub.service.PricingService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * The customer portal: customers on the shop's Wi-Fi hotspot open a page on their phone, send a PDF and pick the
 * options, and the order joins the queue unpaid. Staff mark it paid at the counter and it prints.
 * Uses the JDK's built-in HTTP server, so there is nothing extra to install.
 */
public final class PortalServer {

    /** Guard rails for strangers on the hotspot. */
    public record Limits(int maxUploadBytes, int maxPages, int uploadsPerWindow, long windowMillis) {
        public static Limits defaults() {
            return new Limits(15 * 1024 * 1024, 300, 6, 10 * 60 * 1000L);
        }
    }

    private static final List<Service> OFFERED = List.of(Service.BW, Service.COLORED);
    private static final Pattern TICKET = Pattern.compile("JOB-\\d{1,9}");
    private static final int MAX_COPIES = 100;
    private static final int MAX_NAME = 40;

    private final OrderService orders;
    private final JobQueueService queue;
    private final PricingService pricing;
    private final Path uploadDir;
    private final Limits limits;
    private final Runnable onChange;
    private final RateLimiter limiter;
    private HttpServer server;
    private ExecutorService executor;

    /** @param onChange called after a customer's order joins the queue, so the staff screens can refresh */
    public PortalServer(OrderService orders, JobQueueService queue, PricingService pricing, Path uploadDir,
            Limits limits, Runnable onChange) {
        this.orders = orders;
        this.queue = queue;
        this.pricing = pricing;
        this.uploadDir = uploadDir;
        this.limits = limits;
        this.onChange = onChange;
        this.limiter = new RateLimiter(limits.uploadsPerWindow(), limits.windowMillis(), System::currentTimeMillis);
    }

    /** Starts listening on every network address of this computer. Port 0 picks a free port. */
    public synchronized void start(int port) throws IOException {
        if (server != null) {
            return;
        }
        Files.createDirectories(uploadDir);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        executor = Executors.newFixedThreadPool(6, r -> {
            Thread t = new Thread(r, "printhub-portal");
            t.setDaemon(true);
            return t;
        });
        server.setExecutor(executor);
        server.createContext("/", this::route);
        server.start();
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            executor.shutdownNow();
            server = null;
            executor = null;
        }
    }

    public synchronized boolean isRunning() {
        return server != null;
    }

    /** The port in use (useful when started on port 0). */
    public synchronized int port() {
        return server == null ? -1 : server.getAddress().getPort();
    }

    // ---- Routing ----------------------------------------------------------------------------

    private void route(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            switch (path) {
                case "/", "/index.html" -> {
                    if (!"GET".equals(ex.getRequestMethod())) {
                        send(ex, 405, PortalPages.message("Not allowed", "Please use the order form."));
                    } else {
                        send(ex, 200, PortalPages.form(OFFERED, (s, z) -> pricing.prices().get(s, z),
                                pricing.prices().getRushFee()));
                    }
                }
                case "/upload" -> upload(ex);
                case "/ticket" -> ticket(ex);
                default -> send(ex, 404, PortalPages.message("Page not found", "There is nothing at this address."));
            }
        } catch (RuntimeException e) {
            send(ex, 500, PortalPages.message("Something went wrong", "Please try again, or ask the staff for help."));
        } finally {
            ex.close();
        }
    }

    private void upload(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            send(ex, 405, PortalPages.message("Not allowed", "Please use the order form."));
            return;
        }
        String phone = ex.getRemoteAddress().getAddress().getHostAddress();
        if (!limiter.allow(phone)) {
            drain(ex.getRequestBody());
            send(ex, 429, PortalPages.message("Too many orders",
                    "Too many orders were sent from this phone. Please wait a few minutes, or ask the staff."));
            return;
        }
        InputStream in = ex.getRequestBody();
        byte[] body = in.readNBytes(limits.maxUploadBytes() + 1);
        if (body.length > limits.maxUploadBytes()) {
            drain(in);
            send(ex, 413, PortalPages.message("File too big", "That file is too large. The limit is "
                    + limits.maxUploadBytes() / (1024 * 1024) + " MB."));
            return;
        }

        Multipart form;
        try {
            form = Multipart.parse(ex.getRequestHeaders().getFirst("Content-Type"), body);
        } catch (IllegalArgumentException e) {
            send(ex, 400, PortalPages.message("Could not read the form", e.getMessage()));
            return;
        }
        try {
            Service service = parseService(form.field("service"));
            PaperSize size = parseSize(form.field("size"));
            int copies = parseCopies(form.field("copies"));
            boolean rush = "on".equals(form.field("rush"));
            String name = cleanName(form.field("name"));
            if (form.fileBytes() == null || form.fileBytes().length == 0) {
                throw new IllegalArgumentException("Choose a PDF file to print.");
            }
            int pages = PdfInfo.countPages(form.fileBytes());
            if (pages > limits.maxPages()) {
                throw new IllegalArgumentException("That PDF has " + pages + " pages. The limit is "
                        + limits.maxPages() + " pages per order.");
            }
            Path saved = uploadDir.resolve(UUID.randomUUID() + ".pdf");
            Files.write(saved, form.fileBytes());
            JobOrder job;
            try {
                job = orders.submitOnline(name, service, size, pages, copies, rush, saved.toString());
            } catch (RuntimeException e) {
                Files.deleteIfExists(saved);
                throw e;
            }
            onChange.run();
            ex.getResponseHeaders().add("Location", "/ticket?no=" + job.getId());
            ex.getResponseHeaders().add("Cache-Control", "no-store");
            ex.sendResponseHeaders(303, -1);
        } catch (IllegalArgumentException e) {
            send(ex, 400, PortalPages.message("Cannot take this order", e.getMessage()));
        } catch (IllegalStateException e) {
            send(ex, 422, PortalPages.message("Cannot take this order", e.getMessage()));
        }
    }

    private void ticket(HttpExchange ex) throws IOException {
        String id = queryParam(ex, "no");
        JobOrder job = id != null && TICKET.matcher(id).matches() ? find(id) : null;
        if (job == null) {
            send(ex, 404, PortalPages.message("Ticket not found", "Check the ticket number and try again."));
            return;
        }
        int position = 0;
        if (job.getPaymentStatus() == PaymentStatus.PAID) {
            for (JobOrder j : queue.pending()) {
                if (j.getPaymentStatus() == PaymentStatus.PAID) {
                    position++;
                }
                if (j == job) {
                    break;
                }
            }
        }
        send(ex, 200, PortalPages.ticket(job, position));
    }

    /** Only orders that came through the portal can be looked up here. */
    private JobOrder find(String id) {
        for (JobOrder j : queue.all()) {
            if (j.getId().equals(id) && j.getSource() == JobSource.ONLINE) {
                return j;
            }
        }
        return null;
    }

    // ---- Reading the form -------------------------------------------------------------------

    private static Service parseService(String value) {
        for (Service s : OFFERED) {
            if (s.name().equals(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Choose black and white or colored printing.");
    }

    private static PaperSize parseSize(String value) {
        for (PaperSize z : PaperSize.values()) {
            if (z.name().equals(value)) {
                return z;
            }
        }
        throw new IllegalArgumentException("Choose a paper size.");
    }

    private static int parseCopies(String value) {
        try {
            int copies = Integer.parseInt(value == null ? "" : value.trim());
            if (copies >= 1 && copies <= MAX_COPIES) {
                return copies;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the message below
        }
        throw new IllegalArgumentException("Copies must be a number from 1 to " + MAX_COPIES + ".");
    }

    private static String cleanName(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replaceAll("\\p{Cntrl}", " ").trim();
        return clean.length() > MAX_NAME ? clean.substring(0, MAX_NAME).trim() : clean;
    }

    private static String queryParam(HttpExchange ex, String name) {
        String query = ex.getRequestURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8).equals(name)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    // ---- Sending ----------------------------------------------------------------------------

    private static void drain(InputStream in) {
        try {
            in.readNBytes(256 * 1024);
        } catch (IOException ignored) {
            // the client went away; nothing to do
        }
    }

    private static void send(HttpExchange ex, int status, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }
}
