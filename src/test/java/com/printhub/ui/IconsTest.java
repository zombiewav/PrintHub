package com.printhub.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.junit.jupiter.api.Test;

class IconsTest {

    private static final String[] NAMES = {"alert", "bolt", "cash", "check", "clock", "close", "dashboard", "edit",
        "gcash", "inventory", "job-queue", "logout", "new-order", "paper-jam", "peso", "plus", "price-tag", "printer",
        "restock", "sales", "search", "trash", "user"};

    /** Paints the icon the way Swing does on a display scaled by the given factor. */
    private static BufferedImage paint(Icon icon, int scale) {
        BufferedImage img = new BufferedImage(icon.getIconWidth() * scale, icon.getIconHeight() * scale,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.scale(scale, scale);
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        return img;
    }

    private static int alpha(BufferedImage img, int x, int y) {
        return img.getRGB(x, y) >>> 24;
    }

    @Test
    void everyIconIsDrawnInTheRequestedSizeAndColor() {
        Color teal = new Color(0x0F766E);
        for (String name : NAMES) {
            Icon icon = Icons.get(name, 20, teal);
            assertEquals(20, icon.getIconWidth(), name);
            assertEquals(20, icon.getIconHeight(), name);
            BufferedImage img = paint(icon, 4);
            int ink = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    if (alpha(img, x, y) > 200) {
                        ink++;
                        int rgb = img.getRGB(x, y);
                        assertTrue(Math.abs(((rgb >> 16) & 0xFF) - teal.getRed()) <= 2
                                && Math.abs(((rgb >> 8) & 0xFF) - teal.getGreen()) <= 2
                                && Math.abs((rgb & 0xFF) - teal.getBlue()) <= 2, name + " has the wrong color");
                    }
                }
            }
            assertTrue(ink > 40, name + " draws almost nothing");
        }
    }

    @Test
    void iconsAreVectorsSoTheyStaySharpOnScaledDisplays() {
        for (String name : NAMES) {
            Icon icon = Icons.get(name, 20, Color.BLACK);
            assertFalse(icon instanceof ImageIcon, name + " is still a bitmap");
            BufferedImage img = paint(icon, 8);
            int solid = 0;
            int soft = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int a = alpha(img, x, y);
                    if (a > 235) {
                        solid++;
                    } else if (a > 20) {
                        soft++;
                    }
                }
            }
            assertTrue(soft < solid * 0.35, name + " has blurry edges: " + soft + " soft vs " + solid + " solid pixels");
        }
    }

    @Test
    void nothingIsClippedAtTheEdgesOfTheIconBox() {
        for (String name : NAMES) {
            BufferedImage img = paint(Icons.get(name, 24, Color.BLACK), 4);
            int last = img.getWidth() - 1;
            for (int i = 0; i <= last; i++) {
                assertEquals(0, alpha(img, i, 0), name + " touches the top edge");
                assertEquals(0, alpha(img, i, last), name + " touches the bottom edge");
                assertEquals(0, alpha(img, 0, i), name + " touches the left edge");
                assertEquals(0, alpha(img, last, i), name + " touches the right edge");
            }
        }
    }

    @Test
    void imageMatchesTheRequestedSize() {
        BufferedImage img = Icons.image("printer", 40, Color.WHITE);
        assertEquals(40, img.getWidth());
        assertEquals(40, img.getHeight());
        boolean anyInk = false;
        for (int y = 0; y < 40 && !anyInk; y++) {
            for (int x = 0; x < 40; x++) {
                if (alpha(img, x, y) > 200) {
                    anyInk = true;
                    break;
                }
            }
        }
        assertTrue(anyInk);
    }

    @Test
    void unknownIconNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Icons.get("no-such-icon", 20, Color.BLACK));
    }
}
