package com.printhub.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Loads the PrintHub icon set (white PNGs in 16, 20, 24 and 32 px) and tints them to any color.
 * Names: alert, bolt, cash, check, clock, close, dashboard, edit, gcash, inventory, job-queue,
 * logout, new-order, paper-jam, peso, plus, price-tag, printer, restock, sales, search, trash, user.
 */
public final class Icons {

    private static final int[] SIZES = {16, 20, 24, 32};
    private static final Map<String, ImageIcon> CACHE = new HashMap<>();

    private Icons() { }

    public static ImageIcon get(String name, int size, Color color) {
        String key = name + "|" + size + "|" + color.getRGB();
        ImageIcon icon = CACHE.get(key);
        if (icon == null) {
            icon = new ImageIcon(image(name, size, color));
            CACHE.put(key, icon);
        }
        return icon;
    }

    public static BufferedImage image(String name, int size, Color color) {
        int source = 32;
        for (int s : SIZES) {
            if (s == size) {
                source = s;
            }
        }
        BufferedImage white = read("/com/printhub/icons/white-" + source + "/" + name + ".png");
        BufferedImage tinted = new BufferedImage(source, source, BufferedImage.TYPE_INT_ARGB);
        int rgb = color.getRGB() & 0xFFFFFF;
        for (int y = 0; y < source; y++) {
            for (int x = 0; x < source; x++) {
                int alpha = white.getRGB(x, y) >>> 24;
                tinted.setRGB(x, y, (alpha << 24) | rgb);
            }
        }
        if (source == size) {
            return tinted;
        }
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(tinted, 0, 0, size, size, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage read(String path) {
        try (InputStream in = Icons.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing icon resource: " + path);
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read icon " + path, e);
        }
    }
}
