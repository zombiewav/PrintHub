package com.printhub.web;

import com.printhub.model.JobOrder;
import com.printhub.model.JobStatus;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentStatus;
import com.printhub.model.Service;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

/**
 * The pages customers see on their phones. Built for small touch screens first, and self-contained: no fonts,
 * scripts or images from the internet, since phones on the shop's hotspot may have none (the logo comes from the
 * portal itself). Always light, even when the phone is set to dark mode. The order form works
 * without JavaScript too; with it, the customer gets a live price, file checks and an upload progress bar.
 * Everything that comes from outside goes through {@link #escape}.
 */
final class PortalPages {

    private static final String STYLE = """
            :root{--bg:#f3f4f6;--card:#fff;--text:#111827;--muted:#4b5563;--line:#e5e7eb;--field:#f9fafb;
            --accent:#0f766e;--accent-soft:#ccfbf1;--accent-text:#fff;--err:#b91c1c;--err-soft:#fee2e2;--warn:#b45309;
            --warn-soft:#fef3c7;color-scheme:light only}
            *{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
            html{-webkit-text-size-adjust:100%}
            body{margin:0;font-family:system-ui,-apple-system,"Segoe UI",Roboto,Arial,sans-serif;background:var(--bg);
            color:var(--text);font-size:16px;line-height:1.45}
            header{background:var(--accent);color:var(--accent-text);padding:18px 16px 22px}
            header .in,main{max-width:520px;margin:0 auto}
            .brand{display:flex;align-items:center;gap:12px}
            .logo{width:48px;height:48px;border-radius:50%;background:#fff;padding:3px;flex:none;display:block;
            box-shadow:0 1px 3px rgba(0,0,0,.2)}
            .shop{font-size:19px;font-weight:700;margin:0;overflow-wrap:anywhere}
            .tag{margin:2px 0 0;font-size:14px;opacity:.9}
            main{padding:0 14px 120px;margin-top:-10px}
            .card{background:var(--card);border:1px solid var(--line);border-radius:16px;padding:16px;margin:0 0 14px;
            box-shadow:0 1px 2px rgba(0,0,0,.04)}
            h1{font-size:22px;margin:4px 0 6px}
            h2{font-size:13px;letter-spacing:.06em;text-transform:uppercase;color:var(--muted);margin:0 0 12px}
            p{margin:0 0 10px}
            .muted{color:var(--muted);font-size:14px}
            .sr{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0)}
            .drop{display:flex;align-items:center;gap:14px;border:2px dashed var(--line);border-radius:14px;padding:18px 14px;
            cursor:pointer;min-height:84px;background:var(--field)}
            .drop:focus-within{border-color:var(--accent)}
            .drop.has{border-style:solid;border-color:var(--accent);background:var(--accent-soft)}
            .drop.bad{border-color:var(--err);background:var(--err-soft)}
            .drop svg{flex:none}
            .drop b{display:block;font-size:16px;overflow-wrap:anywhere}
            .drop span{font-size:14px;color:var(--muted)}
            .opts{display:grid;gap:10px}
            .opts.two{grid-template-columns:1fr 1fr}
            .opts.three{grid-template-columns:repeat(3,1fr)}
            .opt input{position:absolute;opacity:0}
            .opt .box{display:block;border:2px solid var(--line);border-radius:12px;padding:12px;min-height:48px;cursor:pointer;
            background:var(--field);text-align:center}
            .opt .box b{display:block;font-size:15px}
            .opt .box small{color:var(--muted);font-size:13px}
            .opt input:checked+.box{border-color:var(--accent);background:var(--accent-soft)}
            .opt input:focus-visible+.box{outline:3px solid var(--accent);outline-offset:2px}
            .row{display:flex;align-items:center;justify-content:space-between;gap:12px}
            .field{margin-top:16px}
            .field>label{display:block;font-size:14px;color:var(--muted);margin:0 0 6px}
            input[type=text]{width:100%;font-size:16px;padding:12px;border:2px solid var(--line);border-radius:12px;
            background:var(--field);color:var(--text)}
            input[type=text]:focus{outline:none;border-color:var(--accent)}
            .step{display:flex;align-items:center;border:2px solid var(--line);border-radius:12px;overflow:hidden;
            background:var(--field)}
            .step button{width:52px;height:48px;border:0;background:transparent;color:var(--accent);font-size:24px;
            font-weight:700;cursor:pointer}
            .step input{width:64px;height:48px;border:0;text-align:center;font-size:18px;font-weight:600;background:transparent;
            color:var(--text);-moz-appearance:textfield}
            .step input::-webkit-inner-spin-button{-webkit-appearance:none}
            .switch{position:relative;width:52px;height:32px;flex:none}
            .switch input{opacity:0;width:100%;height:100%;margin:0;position:absolute;cursor:pointer;z-index:1}
            .switch i{position:absolute;inset:0;border-radius:99px;background:var(--line);transition:background .15s}
            .switch i:after{content:"";position:absolute;top:3px;left:3px;width:26px;height:26px;border-radius:50%;
            background:#fff;box-shadow:0 1px 3px rgba(0,0,0,.3);transition:transform .15s}
            .switch input:checked+i{background:var(--accent)}
            .switch input:checked+i:after{transform:translateX(20px)}
            .switch input:focus-visible+i{outline:3px solid var(--accent);outline-offset:2px}
            .bar{position:fixed;left:0;right:0;bottom:0;background:var(--card);border-top:1px solid var(--line);
            padding:12px 14px calc(12px + env(safe-area-inset-bottom));box-shadow:0 -4px 16px rgba(0,0,0,.06)}
            .bar .in{max-width:520px;margin:0 auto;display:flex;align-items:center;gap:12px}
            .est{flex:1;min-width:0}
            .est b{display:block;font-size:18px}
            .est span{font-size:13px;color:var(--muted)}
            .btn{display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:52px;padding:0 22px;
            border:0;border-radius:14px;background:var(--accent);color:var(--accent-text);font-size:17px;font-weight:700;
            text-decoration:none;cursor:pointer}
            .btn[disabled]{opacity:.6}
            .btn.wide{width:100%}
            .btn.ghost{background:transparent;color:var(--accent);border:2px solid var(--accent)}
            .prog{height:6px;border-radius:99px;background:var(--line);overflow:hidden;margin-top:8px;display:none}
            .prog i{display:block;height:100%;width:0;background:var(--accent);transition:width .2s}
            .msg{border-radius:12px;padding:12px 14px;margin:0 0 14px;font-size:15px}
            .msg.err{background:var(--err-soft);color:var(--err)}
            .ticket{text-align:center}
            .ticket .no{font-size:40px;font-weight:800;letter-spacing:.02em;margin:2px 0}
            .status{display:inline-block;border-radius:99px;padding:6px 14px;font-weight:700;font-size:15px;
            background:var(--accent-soft);color:var(--accent);margin-top:6px}
            .status.warn{background:var(--warn-soft);color:var(--warn)}
            .status.err{background:var(--err-soft);color:var(--err)}
            ol.steps{list-style:none;margin:0;padding:0}
            ol.steps li{position:relative;padding:0 0 18px 40px;min-height:28px}
            ol.steps li:last-child{padding-bottom:0}
            ol.steps li:before{content:"";position:absolute;left:13px;top:28px;bottom:0;width:2px;background:var(--line)}
            ol.steps li:last-child:before{display:none}
            ol.steps li i{position:absolute;left:0;top:0;width:28px;height:28px;border-radius:50%;border:2px solid var(--line);
            background:var(--card);display:grid;place-items:center;font-style:normal;font-size:14px;font-weight:700;
            color:var(--accent-text)}
            ol.steps li.done i{background:var(--accent);border-color:var(--accent)}
            ol.steps li.now i{border-color:var(--accent);box-shadow:0 0 0 4px var(--accent-soft)}
            ol.steps li.done:before{background:var(--accent)}
            ol.steps b{display:block}
            ol.steps span{font-size:14px;color:var(--muted)}
            dl{display:grid;grid-template-columns:auto 1fr;gap:8px 16px;margin:0}
            dt{color:var(--muted)} dd{margin:0;text-align:right;font-weight:600}
            dd.total{font-size:20px;color:var(--accent)}
            .live{display:flex;align-items:center;justify-content:center;gap:8px;font-size:14px;color:var(--muted);margin:4px 0 14px}
            .live:before{content:"";width:8px;height:8px;border-radius:50%;background:var(--accent);animation:p 1.6s infinite}
            @keyframes p{50%{opacity:.25}}
            @media (prefers-reduced-motion:reduce){.live:before{animation:none}*{transition:none!important}}
            a{color:var(--accent)}
            """;

    private static final String FILE_ICON = "<svg width=\"36\" height=\"36\" viewBox=\"0 0 24 24\" fill=\"none\" "
            + "stroke=\"currentColor\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\" "
            + "aria-hidden=\"true\"><path d=\"M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z\"/>"
            + "<path d=\"M14 3v5h5\"/><path d=\"M12 18v-6\"/><path d=\"m9 15 3-3 3 3\"/></svg>";

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

    private static String page(String shop, String title, String tagline, String refreshSeconds, String body) {
        String name = shop == null || shop.isBlank() ? "PrintHub" : shop.trim();
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">"
                + "<meta name=\"theme-color\" content=\"#0f766e\">"
                + "<meta name=\"color-scheme\" content=\"light only\">" // keep phones' forced dark mode off the page
                + "<meta name=\"format-detection\" content=\"telephone=no\">"
                + "<link rel=\"icon\" type=\"image/png\" href=\"/favicon.png\">"
                + "<link rel=\"apple-touch-icon\" href=\"/apple-touch-icon.png\">"
                + (refreshSeconds == null ? "" : "<meta http-equiv=\"refresh\" content=\"" + refreshSeconds + "\">")
                + "<title>" + escape(title) + " - " + escape(name) + "</title><style>" + STYLE + "</style></head><body>"
                + "<header><div class=\"in brand\"><img class=\"logo\" src=\"/logo.png\" width=\"48\" height=\"48\" "
                + "alt=\"PrintHub logo\"><div>"
                + "<p class=\"shop\">" + escape(name) + "</p><p class=\"tag\">" + escape(tagline) + "</p></div></div></header>"
                + "<main>" + body + "</main></body></html>";
    }

    /**
     * The order form.
     *
     * @param unitPrice the price per page for a service and size
     * @param maxBytes  the largest file the shop takes, checked on the phone before sending
     */
    static String form(String shop, List<Service> services, BiFunction<Service, PaperSize, Double> unitPrice,
            double rushFee, int maxBytes, int maxPages, int maxCopies) {
        PaperSize[] sizes = PaperSize.values();
        StringBuilder serviceOptions = new StringBuilder();
        for (int i = 0; i < services.size(); i++) {
            Service s = services.get(i);
            serviceOptions.append("<label class=\"opt\"><input type=\"radio\" name=\"service\" value=\"").append(s.name())
                    .append('"').append(i == 0 ? " checked" : "").append("><span class=\"box\"><b>")
                    .append(escape(s.label())).append("</b><small>from ").append(money(cheapest(s, sizes, unitPrice)))
                    .append(" ").append(escape(s.charged())).append("</small></span></label>");
        }
        StringBuilder sizeOptions = new StringBuilder();
        for (int i = 0; i < sizes.length; i++) {
            sizeOptions.append("<label class=\"opt\"><input type=\"radio\" name=\"size\" value=\"").append(sizes[i].name())
                    .append('"').append(i == 0 ? " checked" : "").append("><span class=\"box\"><b>")
                    .append(escape(sizes[i].label())).append("</b></span></label>");
        }
        StringBuilder prices = new StringBuilder("{");
        for (Service s : services) {
            prices.append('"').append(s.name()).append("\":{");
            for (PaperSize z : sizes) {
                prices.append('"').append(z.name()).append("\":")
                        .append(String.format(Locale.US, "%.2f", unitPrice.apply(s, z))).append(',');
            }
            prices.append("},");
        }
        prices.append('}');
        int maxMb = Math.max(1, maxBytes / (1024 * 1024));

        String body = "<form id=\"f\" method=\"post\" action=\"/upload\" enctype=\"multipart/form-data\">"
                + "<div class=\"card\"><h1>Send a file to print</h1>"
                + "<p class=\"muted\">Choose your PDF and how to print it. Pay at the counter, then we print it.</p>"
                + "<div id=\"err\" class=\"msg err\" role=\"alert\" hidden></div>"
                + "<label class=\"drop\" id=\"drop\">" + FILE_ICON
                + "<input class=\"sr\" id=\"file\" name=\"file\" type=\"file\" accept=\"application/pdf,.pdf\" required>"
                + "<div><b id=\"fname\">Choose your PDF</b><span id=\"finfo\">Up to " + maxMb + " MB and " + maxPages
                + " pages</span></div></label></div>"

                + "<div class=\"card\"><h2>Print</h2><div class=\"opts two\" role=\"radiogroup\" aria-label=\"Service\">"
                + serviceOptions + "</div>"
                + "<div class=\"field\"><label>Paper size</label><div class=\"opts three\" role=\"radiogroup\" "
                + "aria-label=\"Paper size\">" + sizeOptions + "</div></div>"
                + "<div class=\"field row\"><label for=\"copies\" style=\"margin:0\">Copies</label><div class=\"step\">"
                + "<button type=\"button\" id=\"minus\" aria-label=\"One copy less\">&minus;</button>"
                + "<input id=\"copies\" name=\"copies\" type=\"number\" inputmode=\"numeric\" min=\"1\" max=\"" + maxCopies
                + "\" value=\"1\" aria-label=\"Copies\">"
                + "<button type=\"button\" id=\"plus\" aria-label=\"One copy more\">+</button></div></div>"
                + "<div class=\"field row\"><div><b>Rush order</b><div class=\"muted\">+" + money(rushFee)
                + ", printed before regular orders</div></div><span class=\"switch\"><input type=\"checkbox\" id=\"rush\" "
                + "name=\"rush\" value=\"on\" aria-label=\"Rush order\"><i></i></span></div></div>"

                + "<div class=\"card\"><div class=\"field\" style=\"margin:0\"><label for=\"name\">Your name "
                + "(optional, so we can call you)</label><input type=\"text\" id=\"name\" name=\"name\" maxlength=\"40\" "
                + "autocomplete=\"name\" placeholder=\"e.g. Juan\"></div></div>"
                + "<p class=\"muted\" style=\"text-align:center\">The shop counts the pages in your file. "
                + "You get a ticket number to follow your order.</p>"

                + "<div class=\"bar\"><div class=\"in\"><div class=\"est\" aria-live=\"polite\"><b id=\"price\">&nbsp;</b>"
                + "<span id=\"priceNote\">per page</span><div class=\"prog\" id=\"prog\"><i id=\"progBar\"></i></div></div>"
                + "<button class=\"btn\" type=\"submit\" id=\"send\">Send</button></div></div></form>"

                + "<script>(function(){var P=" + prices + ",R=" + String.format(Locale.US, "%.2f", rushFee)
                + ",MAX=" + maxBytes + ",MAXC=" + maxCopies + ";"
                + "function $(i){return document.getElementById(i)}"
                + "function v(n){var e=document.querySelector('input[name='+n+']:checked');return e?e.value:null}"
                + "function peso(x){return '\\u20b1'+x.toFixed(2)}"
                + "function err(t){var e=$('err');e.textContent=t||'';e.hidden=!t;if(t)e.scrollIntoView({block:'center'})}"
                + "function copies(){var c=parseInt($('copies').value,10);return isNaN(c)?1:Math.min(MAXC,Math.max(1,c))}"
                + "function u(){var s=v('service'),z=v('size');if(!s||!z)return;var c=copies(),r=$('rush').checked;"
                + "$('price').textContent=peso(P[s][z])+' / page';"
                + "$('priceNote').textContent='\\u00d7 pages \\u00d7 '+c+(c==1?' copy':' copies')+(r?' + '+peso(R)+' rush':'');}"
                + "document.querySelectorAll('input[type=radio]').forEach(function(e){e.onchange=u});"
                + "$('rush').onchange=u;$('copies').oninput=u;"
                + "$('copies').onblur=function(){this.value=copies();u()};"
                + "$('minus').onclick=function(){$('copies').value=copies()-1<1?1:copies()-1;u()};"
                + "$('plus').onclick=function(){$('copies').value=Math.min(MAXC,copies()+1);u()};"
                + "function mb(b){return b<1048576?Math.max(1,Math.round(b/1024))+' KB':(b/1048576).toFixed(1)+' MB'}"
                + "$('file').onchange=function(){var f=this.files[0],d=$('drop');err('');d.className='drop';"
                + "if(!f){$('fname').textContent='Choose your PDF';return}"
                + "$('fname').textContent=f.name;$('finfo').textContent=mb(f.size)+' \\u00b7 tap to change';"
                + "if(!/\\.pdf$/i.test(f.name)&&f.type!='application/pdf'){d.className='drop bad';err('Please choose a PDF file.')}"
                + "else if(f.size>MAX){d.className='drop bad';err('That file is '+mb(f.size)+'. The limit is '+mb(MAX)+'.')}"
                + "else d.className='drop has'};"
                + "$('f').onsubmit=function(ev){var f=$('file').files[0];"
                + "if(!f){ev.preventDefault();err('Choose a PDF file to send.');return}"
                + "if($('drop').className.indexOf('bad')>=0){ev.preventDefault();return}"
                + "$('copies').value=copies();"
                + "if(!window.FormData||!window.XMLHttpRequest)return;"
                + "ev.preventDefault();var b=$('send'),x=new XMLHttpRequest();b.disabled=true;b.textContent='Sending\\u2026';"
                + "$('prog').style.display='block';"
                + "x.upload.onprogress=function(e){if(e.lengthComputable)$('progBar').style.width=(e.loaded*100/e.total)+'%'};"
                + "x.onload=function(){if(x.status<400&&x.responseURL&&x.responseURL.indexOf('/ticket')>=0)"
                + "{location.replace(x.responseURL)}else{document.open();document.write(x.responseText);document.close()}};"
                + "x.onerror=function(){b.disabled=false;b.textContent='Send';$('prog').style.display='none';"
                + "err('Could not reach the shop. Check that you are still on its Wi-Fi, then try again.')};"
                + "x.open('POST','/upload');x.send(new FormData($('f')))};"
                + "u()})();</script>";
        return page(shop, "Send a file", "Send a file to print", null, body);
    }

    private static double cheapest(Service s, PaperSize[] sizes, BiFunction<Service, PaperSize, Double> unitPrice) {
        double min = Double.MAX_VALUE;
        for (PaperSize z : sizes) {
            min = Math.min(min, unitPrice.apply(s, z));
        }
        return min;
    }

    /** A plain message page, used for refusals and errors. */
    static String message(String shop, String title, String text) {
        String body = "<div class=\"card\"><h1>" + escape(title) + "</h1><div class=\"msg err\" role=\"alert\">"
                + escape(text) + "</div><a class=\"btn wide\" href=\"/\">Back to the order form</a></div>";
        return page(shop, title, "Send a file to print", null, body);
    }

    /** The ticket page: where the order is now. It refreshes itself until the order is finished. */
    static String ticket(String shop, JobOrder job, int queuePosition) {
        String status;
        String statusClass = "";
        String text;
        int step; // the step happening now: 1 paying, 2 waiting for or on the printer, 3 ready (all done)
        boolean finished = false;
        boolean cancelled = job.getStatus() == JobStatus.CANCELLED;
        if (cancelled) {
            status = "Order cancelled";
            statusClass = " err";
            text = "This order was cancelled. If you already paid, please ask the staff.";
            step = -1;
            finished = true;
        } else if (job.getStatus() == JobStatus.COMPLETED) {
            status = "Done";
            text = "Your prints are ready. Please pick them up at the counter.";
            step = 3;
            finished = true;
        } else if (job.getStatus() == JobStatus.PROCESSING) {
            status = "Printing now";
            text = "Your order is being printed.";
            step = 2;
        } else if (job.getPaymentStatus() == PaymentStatus.UNPAID) {
            status = "Waiting for payment";
            statusClass = " warn";
            text = "Please pay " + money(job.getTotal()) + " at the counter, in cash or GCash. Your order is printed "
                    + "once the staff confirms your payment. Unpaid orders are cancelled after a while.";
            step = 1;
        } else {
            status = "In the queue";
            text = "Paid, thank you. Position " + queuePosition + " in the queue. Rush orders go first.";
            step = 2;
        }

        StringBuilder body = new StringBuilder();
        body.append("<div class=\"card ticket\"><div class=\"muted\">Your ticket</div><div class=\"no\">")
                .append(escape(job.getId())).append("</div><div class=\"status").append(statusClass).append("\">")
                .append(escape(status)).append("</div><p style=\"margin-top:12px\">").append(escape(text))
                .append("</p><p class=\"muted\" style=\"margin:0\">Show this number at the counter.</p></div>");
        if (!finished) {
            body.append("<div class=\"live\" aria-hidden=\"true\">This page updates by itself</div>");
        }
        if (!cancelled) {
            body.append("<div class=\"card\"><h2>Progress</h2><ol class=\"steps\">")
                    .append(stepItem("Sent", "The shop has your file", step, 0))
                    .append(stepItem("Paid", job.getPaymentStatus() == PaymentStatus.PAID ? "Payment confirmed"
                            : "Pay " + money(job.getTotal()) + " at the counter", step, 1))
                    .append(stepItem("Printing", job.getStatus() == JobStatus.PROCESSING ? "On the printer now"
                            : step == 3 ? "Printed" : "Waits for its turn after payment", step, 2))
                    .append(stepItem("Ready", "Pick it up at the counter", step, 3))
                    .append("</ol></div>");
        }
        body.append("<div class=\"card\"><h2>Order</h2><dl>")
                .append("<dt>Service</dt><dd>").append(escape(job.getService().label())).append("</dd>")
                .append("<dt>Paper</dt><dd>").append(escape(job.getSize().label())).append("</dd>")
                .append("<dt>Pages</dt><dd>").append(job.getPages()).append("</dd>")
                .append("<dt>Copies</dt><dd>").append(job.getCopies()).append("</dd>")
                .append(job.isRush() ? "<dt>Rush</dt><dd>Yes</dd>" : "")
                .append("<dt>Total</dt><dd class=\"total\">").append(money(job.getTotal())).append("</dd></dl></div>")
                .append("<a class=\"btn ghost wide\" href=\"/\">Send another file</a>");
        return page(shop, job.getId(), "Order " + job.getId(), finished ? null : "8", body.toString());
    }

    /** One line of the progress list: done, happening now, or still to come. */
    private static String stepItem(String title, String note, int current, int index) {
        String cls = index < current || (index == current && index == 3) ? "done" : index == current ? "now" : "";
        String mark = "done".equals(cls) ? "&#10003;" : "";
        return "<li class=\"" + cls + "\"><i>" + mark + "</i><b>" + escape(title) + "</b><span>" + escape(note)
                + "</span></li>";
    }
}
