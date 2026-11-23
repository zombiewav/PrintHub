package com.printhub.web;

import com.printhub.model.JobOrder;
import com.printhub.model.JobStatus;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Service;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

/** The pages customers see on their phones. Everything that comes from outside goes through {@link #escape}. */
final class PortalPages {

    private static final String STYLE = """
            body{font-family:Segoe UI,Roboto,Arial,sans-serif;background:#f3f4f6;margin:0;color:#111827}
            main{max-width:480px;margin:0 auto;padding:20px}
            h1{font-size:22px;margin:8px 0 4px}
            p.sub{color:#4b5563;margin:0 0 16px}
            .card{background:#fff;border:1px solid #e5e7eb;border-radius:12px;padding:16px;margin-bottom:16px}
            label{display:block;font-size:13px;color:#4b5563;margin:12px 0 4px}
            input,select{width:100%;box-sizing:border-box;font-size:16px;padding:10px;border:1px solid #d1d5db;border-radius:8px;background:#fff}
            input[type=checkbox]{width:auto;margin-right:8px}
            button{width:100%;font-size:17px;font-weight:600;color:#fff;background:#0f766e;border:0;border-radius:8px;padding:12px;margin-top:16px}
            .price{font-size:14px;color:#0f766e;margin-top:6px}
            .big{font-size:30px;font-weight:700;color:#0f766e;margin:4px 0}
            .err{color:#b91c1c} .ok{color:#0f766e}
            """;

    private PortalPages() {
    }

    /** Makes text safe to put inside HTML. */
    static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String money(double amount) {
        return String.format(Locale.US, "₱%,.2f", amount);
    }

    private static String page(String title, String refreshSeconds, String body) {
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + (refreshSeconds == null ? "" : "<meta http-equiv=\"refresh\" content=\"" + refreshSeconds + "\">")
                + "<title>" + escape(title) + " - PrintHub</title><style>" + STYLE + "</style></head><body><main>"
                + body + "</main></body></html>";
    }

    /** The order form. unitPrice gives the price per page for a service and size. */
    static String form(List<Service> services, BiFunction<Service, PaperSize, Double> unitPrice, double rushFee) {
        StringBuilder serviceOptions = new StringBuilder();
        for (Service s : services) {
            serviceOptions.append("<option value=\"").append(s.name()).append("\">").append(escape(s.label()))
                    .append("</option>");
        }
        StringBuilder sizeOptions = new StringBuilder();
        for (PaperSize z : PaperSize.values()) {
            sizeOptions.append("<option value=\"").append(z.name()).append("\">").append(escape(z.label()))
                    .append("</option>");
        }
        StringBuilder prices = new StringBuilder("{");
        for (Service s : services) {
            prices.append('"').append(s.name()).append("\":{");
            for (PaperSize z : PaperSize.values()) {
                prices.append('"').append(z.name()).append("\":").append(String.format(Locale.US, "%.2f", unitPrice.apply(s, z)))
                        .append(',');
            }
            prices.append("},");
        }
        prices.append('}');
        String body = "<h1>PrintHub</h1><p class=\"sub\">Send your PDF to the shop. Pay at the counter and we print it.</p>"
                + "<form class=\"card\" method=\"post\" action=\"/upload\" enctype=\"multipart/form-data\">"
                + "<label for=\"name\">Your name (optional)</label><input id=\"name\" name=\"name\" maxlength=\"40\" autocomplete=\"name\">"
                + "<label for=\"service\">Service</label><select id=\"service\" name=\"service\">" + serviceOptions + "</select>"
                + "<label for=\"size\">Paper size</label><select id=\"size\" name=\"size\">" + sizeOptions + "</select>"
                + "<label for=\"copies\">Copies</label><input id=\"copies\" name=\"copies\" type=\"number\" min=\"1\" max=\"100\" value=\"1\">"
                + "<label><input type=\"checkbox\" name=\"rush\" value=\"on\">Rush order (+" + money(rushFee) + ", goes first)</label>"
                + "<label for=\"file\">Your PDF</label><input id=\"file\" name=\"file\" type=\"file\" accept=\"application/pdf,.pdf\" required>"
                + "<div class=\"price\" id=\"price\"></div>"
                + "<button type=\"submit\">Send to the shop</button></form>"
                + "<p class=\"sub\">The shop counts the pages in your file and works out the total. "
                + "You will get a ticket number to follow your order.</p>"
                + "<script>var P=" + prices + ";function u(){var s=document.getElementById('service').value,"
                + "z=document.getElementById('size').value;document.getElementById('price').textContent="
                + "'Price: ₱'+P[s][z].toFixed(2)+' per page';}"
                + "document.getElementById('service').onchange=u;document.getElementById('size').onchange=u;u();</script>";
        return page("Send a PDF", null, body);
    }

    /** A plain message page, used for refusals and errors. */
    static String message(String title, String text) {
        return page(title, null, "<h1>" + escape(title) + "</h1><div class=\"card err\">" + escape(text)
                + "</div><p><a href=\"/\">Back to the order form</a></p>");
    }

    /** The ticket page: where the order is now. It refreshes itself until the order is finished. */
    static String ticket(JobOrder job, int queuePosition) {
        String title;
        String text;
        boolean finished = false;
        if (job.getStatus() == JobStatus.CANCELLED) {
            title = "Order cancelled";
            text = "This order was cancelled. If you already paid, please ask the staff.";
            finished = true;
        } else if (job.getStatus() == JobStatus.COMPLETED) {
            title = "Done";
            text = "Your prints are ready. Please pick them up at the counter.";
            finished = true;
        } else if (job.getStatus() == JobStatus.PROCESSING) {
            title = "Printing now";
            text = "Your order is being printed.";
        } else if (job.getPaymentStatus() == PaymentStatus.UNPAID) {
            title = "Waiting for payment";
            text = "Please pay " + money(job.getTotal()) + " at the counter, in cash or GCash. Your order is printed "
                    + "once the staff confirms your payment. Unpaid orders are cancelled after a while.";
        } else {
            title = "In the queue";
            text = "Paid, thank you. Position " + queuePosition + " in the queue. Rush orders go first.";
        }
        String body = "<h1>" + escape(job.getId()) + "</h1><p class=\"sub\">" + escape(job.getService().label()) + " &middot; "
                + escape(job.getSize().label()) + " &middot; " + job.getPages() + " pages &times; " + job.getCopies()
                + " copies" + (job.isRush() ? " &middot; rush" : "") + "</p>"
                + "<div class=\"card\"><div class=\"big\">" + escape(title) + "</div><p>" + escape(text) + "</p>"
                + "<p>Total: <b>" + money(job.getTotal()) + "</b></p></div>"
                + (finished ? "" : "<p class=\"sub\">This page updates by itself.</p>")
                + "<p><a href=\"/\">Send another file</a></p>";
        return page(job.getId(), finished ? null : "8", body);
    }
}
