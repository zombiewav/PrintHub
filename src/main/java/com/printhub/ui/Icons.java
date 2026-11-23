package com.printhub.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Icon;

/**
 * The PrintHub icon set, drawn as vector line art (24 x 24 grid, 2 unit round stroke) so every icon stays
 * sharp at any size and on scaled displays. The bitmap files in /com/printhub/icons are no longer used for drawing.
 * Names: alert, bolt, cash, check, clock, close, dashboard, edit, gcash, inventory, job-queue,
 * logout, new-order, paper-jam, peso, plus, price-tag, printer, restock, sales, search, trash, user.
 */
public final class Icons {

    private static final Map<String, Consumer<Graphics2D>> PAINTERS = new HashMap<>();
    private static final Map<String, Icon> CACHE = new HashMap<>();

    static {
        PAINTERS.put("alert", g -> {
            poly(g, true, 12, 4, 21.4, 19.8, 2.6, 19.8);
            line(g, 12, 9.5, 12, 13.5);
            dot(g, 12, 16.6);
        });
        PAINTERS.put("bolt", g -> poly(g, true, 13, 3, 13, 10, 19, 10, 11, 21, 11, 14, 5, 14));
        PAINTERS.put("cash", g -> {
            rect(g, 2.5, 6, 19, 12, 2);
            circle(g, 12, 12, 2.6);
        });
        PAINTERS.put("check", g -> poly(g, false, 5, 12.5, 10, 17.5, 19, 7));
        PAINTERS.put("clock", g -> {
            circle(g, 12, 12, 9);
            poly(g, false, 12, 7, 12, 12, 15, 14);
        });
        PAINTERS.put("close", g -> {
            line(g, 6, 6, 18, 18);
            line(g, 18, 6, 6, 18);
        });
        PAINTERS.put("dashboard", g -> {
            rect(g, 4, 4, 6, 6, 1);
            rect(g, 14, 4, 6, 6, 1);
            rect(g, 4, 14, 6, 6, 1);
            rect(g, 14, 14, 6, 6, 1);
        });
        PAINTERS.put("edit", g -> {
            poly(g, true, 4, 20, 8, 20, 19, 9, 15, 5, 4, 16);
            line(g, 13, 7, 17, 11);
            line(g, 14, 20, 21, 20);
        });
        PAINTERS.put("gcash", g -> {
            rect(g, 6.5, 3, 11, 18, 2.5);
            line(g, 11, 17.5, 13, 17.5);
        });
        PAINTERS.put("inventory", g -> {
            poly(g, true, 12, 3, 20, 7.5, 20, 16.5, 12, 21, 4, 16.5, 4, 7.5);
            poly(g, false, 4, 7.5, 12, 12, 20, 7.5);
            line(g, 12, 12, 12, 21);
        });
        PAINTERS.put("job-queue", g -> {
            line(g, 9, 6, 20, 6);
            line(g, 9, 12, 20, 12);
            line(g, 9, 18, 20, 18);
            dot(g, 4, 6);
            dot(g, 4, 12);
            dot(g, 4, 18);
        });
        PAINTERS.put("logout", g -> {
            Path2D p = new Path2D.Double();
            p.moveTo(14, 8);
            p.lineTo(14, 6);
            p.quadTo(14, 4, 12, 4);
            p.lineTo(6, 4);
            p.quadTo(4, 4, 4, 6);
            p.lineTo(4, 18);
            p.quadTo(4, 20, 6, 20);
            p.lineTo(12, 20);
            p.quadTo(14, 20, 14, 18);
            p.lineTo(14, 16);
            g.draw(p);
            line(g, 9, 12, 21, 12);
            poly(g, false, 18, 9, 21, 12, 18, 15);
        });
        PAINTERS.put("new-order", g -> {
            file(g);
            line(g, 12, 11, 12, 17);
            line(g, 9, 14, 15, 14);
        });
        PAINTERS.put("paper-jam", g -> {
            file(g);
            line(g, 9.5, 12, 14.5, 17);
            line(g, 14.5, 12, 9.5, 17);
        });
        PAINTERS.put("peso", g -> {
            Path2D p = new Path2D.Double();
            p.moveTo(8, 19);
            p.lineTo(8, 5);
            p.lineTo(11.5, 5);
            p.append(new Arc2D.Double(7, 5, 9, 9, 90, -180, Arc2D.OPEN), true);
            p.lineTo(8, 14);
            g.draw(p);
            line(g, 5.5, 8, 18, 8);
            line(g, 5.5, 11, 18, 11);
        });
        PAINTERS.put("plus", g -> {
            line(g, 12, 5, 12, 19);
            line(g, 5, 12, 19, 12);
        });
        PAINTERS.put("price-tag", g -> {
            poly(g, true, 4, 3.5, 11.5, 3.5, 20.5, 12.5, 12.5, 20.5, 3.5, 11.5);
            circle(g, 7.5, 7.5, 1);
        });
        PAINTERS.put("printer", g -> {
            Path2D body = new Path2D.Double();
            body.moveTo(17, 17);
            body.lineTo(19, 17);
            body.quadTo(21, 17, 21, 15);
            body.lineTo(21, 11);
            body.quadTo(21, 9, 19, 9);
            body.lineTo(5, 9);
            body.quadTo(3, 9, 3, 11);
            body.lineTo(3, 15);
            body.quadTo(3, 17, 5, 17);
            body.lineTo(7, 17);
            g.draw(body);
            Path2D paper = new Path2D.Double();
            paper.moveTo(17, 9);
            paper.lineTo(17, 5);
            paper.quadTo(17, 3, 15, 3);
            paper.lineTo(9, 3);
            paper.quadTo(7, 3, 7, 5);
            paper.lineTo(7, 9);
            g.draw(paper);
            rect(g, 7, 13, 10, 8, 2);
        });
        PAINTERS.put("restock", g -> {
            g.draw(new Arc2D.Double(4, 4, 16, 16, 20, 290, Arc2D.OPEN));
            poly(g, false, 21.15, 4.95, 19.52, 9.26, 15.51, 6.99);
        });
        PAINTERS.put("sales", g -> {
            rect(g, 3, 5, 18, 14, 2);
            line(g, 3, 10, 21, 10);
            line(g, 7, 15, 11, 15);
        });
        PAINTERS.put("search", g -> {
            circle(g, 10.5, 10.5, 6.5);
            line(g, 15.5, 15.5, 21, 21);
        });
        PAINTERS.put("trash", g -> {
            line(g, 4, 7, 20, 7);
            Path2D bin = new Path2D.Double();
            bin.moveTo(5.5, 7);
            bin.lineTo(6.5, 18.5);
            bin.quadTo(6.7, 21, 9, 21);
            bin.lineTo(15, 21);
            bin.quadTo(17.3, 21, 17.5, 18.5);
            bin.lineTo(18.5, 7);
            g.draw(bin);
            Path2D handle = new Path2D.Double();
            handle.moveTo(9, 7);
            handle.lineTo(9, 4.5);
            handle.quadTo(9, 3.5, 10, 3.5);
            handle.lineTo(14, 3.5);
            handle.quadTo(15, 3.5, 15, 4.5);
            handle.lineTo(15, 7);
            g.draw(handle);
        });
        PAINTERS.put("user", g -> {
            circle(g, 12, 8, 4);
            Path2D body = new Path2D.Double();
            body.moveTo(6, 21);
            body.lineTo(6, 19);
            body.quadTo(6, 15, 10, 15);
            body.lineTo(14, 15);
            body.quadTo(18, 15, 18, 19);
            body.lineTo(18, 21);
            g.draw(body);
        });
    }

    private Icons() { }

    // ---- Shapes -----------------------------------------------------------------------------

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    /** A dot: a zero length line with round ends. */
    private static void dot(Graphics2D g, double x, double y) {
        g.draw(new Line2D.Double(x, y, x + 0.01, y));
    }

    private static void rect(Graphics2D g, double x, double y, double w, double h, double radius) {
        g.draw(new RoundRectangle2D.Double(x, y, w, h, radius * 2, radius * 2));
    }

    private static void circle(Graphics2D g, double cx, double cy, double r) {
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static void poly(Graphics2D g, boolean closed, double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        if (closed) {
            p.closePath();
        }
        g.draw(p);
    }

    /** A page with a folded top-right corner. */
    private static void file(Graphics2D g) {
        Path2D page = new Path2D.Double();
        page.moveTo(14, 3);
        page.lineTo(7, 3);
        page.quadTo(5, 3, 5, 5);
        page.lineTo(5, 19);
        page.quadTo(5, 21, 7, 21);
        page.lineTo(17, 21);
        page.quadTo(19, 21, 19, 19);
        page.lineTo(19, 8);
        page.closePath();
        g.draw(page);
        Path2D fold = new Path2D.Double();
        fold.moveTo(14, 3);
        fold.lineTo(14, 7);
        fold.quadTo(14, 8, 15, 8);
        fold.lineTo(19, 8);
        g.draw(fold);
    }

    // ---- Public API -------------------------------------------------------------------------

    /** A square icon of the given size (pixels at 100% scale) in the given color. Drawn fresh at any display scale. */
    public static Icon get(String name, int size, Color color) {
        Consumer<Graphics2D> painter = PAINTERS.get(name);
        if (painter == null) {
            throw new IllegalArgumentException("Unknown icon: " + name);
        }
        String key = name + "|" + size + "|" + color.getRGB();
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(key, k -> new VectorIcon(painter, size, color));
        }
    }

    /** The icon rendered into an image, for places that need a picture (the window icon). */
    public static BufferedImage image(String name, int size, Color color) {
        Icon icon = get(name, size, color);
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return img;
    }

    private static final class VectorIcon implements Icon {
        private final Consumer<Graphics2D> painter;
        private final int size;
        private final Color color;

        VectorIcon(Consumer<Graphics2D> painter, int size, Color color) {
            this.painter = painter;
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 24.0, size / 24.0);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            painter.accept(g2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
