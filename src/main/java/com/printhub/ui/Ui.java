package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.JTextComponent;

/**
 * Small helpers shared by the screens. The forms call these from their "post-init code"
 * (Code tab of the Properties window) so the FlatLaf look is applied after NetBeans builds the component.
 */
public final class Ui {

    public static final String PESO = "₱";
    public static final String TIMES = "×";
    public static final String DOT = "·";
    public static final String DASH = "—";
    public static final String ENDASH = "–";

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    public static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("MMM d · h:mm a", Locale.ENGLISH);
    public static final DateTimeFormatter FULL_STAMP = DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.ENGLISH);

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat PLAIN = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private Ui() { }

    // ---- Formatting -------------------------------------------------------------------------

    public static String money(double value) {
        return PESO + MONEY.format(value);
    }

    /** 20.00, no peso sign, for editable fields. */
    public static String plain(double value) {
        return PLAIN.format(value);
    }

    public static String count(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    /** Reads 1,000 or 1,000.50 or a peso amount; returns -1 when the text is not a number. */
    public static double parseAmount(String text) {
        String clean = text == null ? "" : text.replaceAll("[^0-9.]", "");
        if (clean.isEmpty() || clean.equals(".")) {
            return -1;
        }
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String hex(Color c) {
        return String.format("#%06X", c.getRGB() & 0xFFFFFF);
    }

    // ---- Panels and labels (used by code-built parts) ---------------------------------------

    /** Transparent panel, so it shows the white card or gray frame underneath. */
    public static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    /** Left or right aligned row of components with a gap between them and no padding at the ends. */
    public static JPanel flow(int align, int gap) {
        JPanel p = panel(new FlowLayout(align, gap, 0));
        p.setBorder(new EmptyBorder(0, -gap, 0, -gap));
        return p;
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JLabel caption(String text) {
        return label(text, FONT_SMALL, TEXT_SECONDARY);
    }

    public static JLabel body(String text) {
        return label(text, FONT_BODY, TEXT);
    }

    public static JLabel bold(String text) {
        return label(text, FONT_BODY_BOLD, TEXT);
    }

    // ---- Buttons ----------------------------------------------------------------------------

    private static JButton button(JButton b, Color bg, Color fg, Color border, Color hover, Color pressed) {
        b.putClientProperty("FlatLaf.style",
                "background:" + hex(bg) + "; foreground:" + hex(fg) + "; borderColor:" + hex(border)
                + "; hoverBackground:" + hex(hover) + "; pressedBackground:" + hex(pressed)
                + "; focusedBorderColor:" + hex(border) + "; hoverBorderColor:" + hex(border)
                + "; disabledBackground:" + hex(BORDER) + "; disabledBorderColor:" + hex(BORDER)
                + "; disabledText:" + hex(TEXT_DISABLED) + "; margin:3,16,3,16");
        b.setFont(FONT_BODY_BOLD);
        b.setFocusable(false);
        return sized(b, BUTTON_HEIGHT);
    }

    /** Fixes the height at 36 (or 32 for small toolbar buttons) once text and icon are set. */
    public static JButton sized(JButton b, int height) {
        b.setPreferredSize(null);
        Dimension d = b.getPreferredSize();
        b.setPreferredSize(new Dimension(Math.max(d.width, 72), height));
        return b;
    }

    public static JButton primary(JButton b) {
        return button(b, ACCENT, Color.WHITE, ACCENT, new Color(0x0D6B64), ACCENT_PRESSED);
    }

    public static JButton secondary(JButton b) {
        return button(b, PANEL, TEXT, FIELD_BORDER, new Color(0xF9FAFB), BACKGROUND);
    }

    public static JButton danger(JButton b) {
        Color red = INSUFFICIENT[1];
        return button(b, PANEL, red, red, new Color(0xFEF2F2), INSUFFICIENT[0]);
    }

    public static JButton warning(JButton b) {
        return button(b, PANEL, LOW[1], RUSH[0], new Color(0xFFF7ED), RUSH_TILE);
    }

    public static JButton primary(String text) {
        return primary(new JButton(text));
    }

    public static JButton secondary(String text) {
        return secondary(new JButton(text));
    }

    // ---- Inputs -----------------------------------------------------------------------------

    public static <T extends JTextComponent> T field(T f) {
        f.setFont(FONT_BODY);
        f.setMargin(new Insets(4, 8, 4, 8));
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, FIELD_HEIGHT));
        return f;
    }

    public static <T> JComboBox<T> combo(JComboBox<T> c) {
        c.setFont(FONT_BODY);
        c.setPreferredSize(new Dimension(c.getPreferredSize().width, FIELD_HEIGHT));
        return c;
    }

    public static JSpinner spinner(JSpinner s) {
        s.setFont(FONT_BODY);
        s.setPreferredSize(new Dimension(s.getPreferredSize().width, FIELD_HEIGHT));
        if (s.getEditor() instanceof JSpinner.DefaultEditor) {
            JFormattedTextField field = ((JSpinner.DefaultEditor) s.getEditor()).getTextField();
            field.setHorizontalAlignment(SwingConstants.RIGHT);
            // Buttons here never take focus, so a typed number would stay uncommitted (and be ignored) when the
            // user clicks a button straight after typing. Take each valid number as it is typed instead.
            if (field.getFormatter() instanceof DefaultFormatter) {
                ((DefaultFormatter) field.getFormatter()).setCommitsOnValidEdit(true);
            }
        }
        return s;
    }

    // ---- Tables and tabs --------------------------------------------------------------------

    /** Table inside a scroll pane: no border, white background. */
    public static JScrollPane scroll(JScrollPane sp) {
        // an empty border (not null), otherwise the table UI puts its own outline back
        sp.setBorder(new EmptyBorder(0, 0, 0, 0));
        sp.setViewportBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setBackground(PANEL);
        return sp;
    }

    public static JScrollPane scroll(JTable table) {
        return scroll(new JScrollPane(table));
    }

    /** Gives every tab a title with a count chip and keeps the selected one highlighted. */
    public static void syncTabs(JTabbedPane tabs) {
        Runnable restyle = () -> {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                Component c = tabs.getTabComponentAt(i);
                if (c instanceof TabHeader) {
                    ((TabHeader) c).setSelected(i == tabs.getSelectedIndex());
                }
            }
        };
        tabs.addChangeListener(e -> restyle.run());
        restyle.run();
    }

    public static TabHeader addTab(JTabbedPane tabs, String title, JComponent content, int count) {
        tabs.addTab(null, content);
        TabHeader header = new TabHeader(title, count);
        tabs.setTabComponentAt(tabs.getTabCount() - 1, header);
        return header;
    }

    // ---- Misc -------------------------------------------------------------------------------

    /** Small right-pointing play triangle (Segoe UI has no glyph for it). */
    public static JComponent triangle(Color color) {
        JComponent c = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                int h = getHeight();
                g2.fillPolygon(new int[] {0, 0, 9}, new int[] {(h - 10) / 2, (h + 10) / 2, h / 2}, 3);
                g2.dispose();
            }
        };
        c.setPreferredSize(new Dimension(10, 14));
        return c;
    }

    // ---- Logo ---------------------------------------------------------------------------------

    /** The sizes of the PrintHub logo in /com/printhub/logo (made from the original artwork, square, transparent). */
    private static final int[] LOGO_SIZES = {16, 24, 32, 48, 64, 128, 256};
    private static final java.util.Map<Integer, java.awt.image.BufferedImage> LOGOS = new java.util.HashMap<>();

    /** The logo image of exactly one of {@link #LOGO_SIZES}. */
    private static synchronized java.awt.image.BufferedImage logoFile(int size) {
        return LOGOS.computeIfAbsent(size, s -> {
            try (java.io.InputStream in = Ui.class.getResourceAsStream("/com/printhub/logo/logo-" + s + ".png")) {
                return javax.imageio.ImageIO.read(in);
            } catch (java.io.IOException | IllegalArgumentException e) {
                throw new IllegalStateException("The PrintHub logo is missing from the app (logo-" + s + ".png).", e);
            }
        });
    }

    /** The smallest logo image at least {@code px} pixels wide (the largest one when none is that big). */
    public static java.awt.image.BufferedImage logo(int px) {
        for (int s : LOGO_SIZES) {
            if (s >= px) {
                return logoFile(s);
            }
        }
        return logoFile(LOGO_SIZES[LOGO_SIZES.length - 1]);
    }

    /** Every logo size, for window icons: Windows picks the right one for the title bar, taskbar and Alt+Tab. */
    public static java.util.List<java.awt.Image> appIcons() {
        java.util.List<java.awt.Image> all = new java.util.ArrayList<>();
        for (int s : LOGO_SIZES) {
            all.add(logoFile(s));
        }
        return all;
    }
}
