package com.printhub.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.Collections;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * PrintHub design tokens. Call PrintHubTheme.install() once, before creating any Swing component
 * (first line of main), then use the constants anywhere in the UI code.
 */
public final class PrintHubTheme {

    private PrintHubTheme() { }

    // ---- Colors ----------------------------------------------------------------------------
    public static final Color ACCENT         = new Color(0x0F766E);
    public static final Color ACCENT_PRESSED = new Color(0x115E59);
    public static final Color ACCENT_TINT    = new Color(0xE6F4F1); // selected nav item, selected row, banner
    public static final Color ACCENT_TINT_BORDER = new Color(0xB7DDD8);
    public static final Color ACCENT_LIGHT   = new Color(0x7CC4BB); // GCash bar

    public static final Color BACKGROUND     = new Color(0xF3F4F6); // frame, table header
    public static final Color PANEL          = Color.WHITE;
    public static final Color BORDER         = new Color(0xE5E7EB);
    public static final Color FIELD_BORDER   = new Color(0xD1D5DB);

    public static final Color TEXT           = new Color(0x111827);
    public static final Color TEXT_SECONDARY = new Color(0x4B5563);
    public static final Color TEXT_DISABLED  = new Color(0x6B7280);

    // Status: {background, text}
    public static final Color[] PENDING      = { new Color(0xE5E7EB), new Color(0x374151) };
    public static final Color[] PROCESSING   = { new Color(0xDBEAFE), new Color(0x1D4ED8) };
    public static final Color[] COMPLETED    = { new Color(0xDCFCE7), new Color(0x15803D) }; // also "OK"
    public static final Color[] RUSH         = { new Color(0xC2410C), Color.WHITE };
    public static final Color[] LOW          = { new Color(0xFEF3C7), new Color(0x92400E) };
    public static final Color[] INSUFFICIENT = { new Color(0xFEE2E2), new Color(0xB91C1C) }; // also "Out"

    // Icon tile backgrounds on the dashboard cards
    public static final Color RUSH_TILE      = new Color(0xFFEDD5);

    // ---- Fonts (Segoe UI) ------------------------------------------------------------------
    public static final String FAMILY = "Segoe UI";
    public static final Font FONT_BRAND      = new Font(FAMILY, Font.BOLD, 32);  // login brand
    public static final Font FONT_CARD_VALUE = new Font(FAMILY, Font.BOLD, 28);  // summary cards, grand total
    public static final Font FONT_TITLE      = new Font(FAMILY, Font.BOLD, 20);  // page title
    public static final Font FONT_SIDEBAR    = new Font(FAMILY, Font.BOLD, 16);  // sidebar brand
    public static final Font FONT_HEADING    = new Font(FAMILY, Font.BOLD, 14);  // panel heading
    public static final Font FONT_BODY       = new Font(FAMILY, Font.PLAIN, 13); // inputs, table cells
    public static final Font FONT_BODY_BOLD  = new Font(FAMILY, Font.BOLD, 13);  // buttons, nav, tabs
    public static final Font FONT_SMALL      = new Font(FAMILY, Font.PLAIN, 12); // labels, captions
    public static final Font FONT_SMALL_BOLD = new Font(FAMILY, Font.BOLD, 12);  // table header, badges

    // ---- Spacing and sizes (px) ------------------------------------------------------------
    public static final int GRID = 8;
    public static final int GAP = 16;               // between panels
    public static final int PANEL_PADDING = 16;
    public static final int CONTENT_PADDING = 24;
    public static final int SIDEBAR_WIDTH = 220;
    public static final int NAV_ITEM_HEIGHT = 40;
    public static final int FIELD_HEIGHT = 32;
    public static final int BUTTON_HEIGHT = 36;
    public static final int ROW_HEIGHT = 32;
    public static final int PRICE_ROW_HEIGHT = 48;
    public static final int CARD_HEIGHT = 96;
    public static final int ARC = 4;                // corner radius; FlatLaf arcs are diameters (ARC * 2)

    public static EmptyBorder padding(int px) { return new EmptyBorder(px, px, px, px); }

    /** Call first in main(), before any component is created. */
    public static void install() {
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#0F766E"));
        FlatLightLaf.setup();

        int arc = ARC * 2;
        UIManager.put("defaultFont", FONT_BODY);
        UIManager.put("Component.arc", arc);
        UIManager.put("Button.arc", arc);
        UIManager.put("TextComponent.arc", arc);
        UIManager.put("ProgressBar.arc", 999);
        UIManager.put("Panel.background", BACKGROUND);

        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Component.borderColor", FIELD_BORDER);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("TextField.disabledBackground", BACKGROUND);
        UIManager.put("TextField.inactiveBackground", BACKGROUND);
        UIManager.put("TextField.inactiveForeground", TEXT_DISABLED);
        UIManager.put("ProgressBar.background", BORDER);

        // Tabs: teal underline, 36 px high, 16 px side padding
        UIManager.put("TabbedPane.background", PANEL);
        UIManager.put("TabbedPane.foreground", TEXT_SECONDARY);
        UIManager.put("TabbedPane.selectedForeground", ACCENT);
        UIManager.put("TabbedPane.underlineColor", ACCENT);
        UIManager.put("TabbedPane.inactiveUnderlineColor", ACCENT);
        UIManager.put("TabbedPane.contentAreaColor", BORDER);
        UIManager.put("TabbedPane.hoverColor", PANEL);
        UIManager.put("TabbedPane.focusColor", PANEL);
        UIManager.put("TabbedPane.tabHeight", 36);
        UIManager.put("TabbedPane.tabInsets", new Insets(0, 16, 0, 16));
        UIManager.put("TabbedPane.tabSelectionHeight", 2);
        UIManager.put("TabbedPane.font", FONT_BODY_BOLD);

        // Dialogs get the flat title bar (white, close button only)
        JDialog.setDefaultLookAndFeelDecorated(true);
    }

    /** Standard look for every JTable: 32 px rows, no vertical lines, gray header, teal-tint selection. */
    public static void styleTable(JTable table) {
        table.setRowHeight(ROW_HEIGHT);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setFont(FONT_BODY);
        table.setForeground(TEXT);
        table.setSelectionBackground(ACCENT_TINT);
        table.setSelectionForeground(TEXT);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setPreferredSize(new Dimension(0, ROW_HEIGHT));
        table.getTableHeader().setFont(FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(BACKGROUND);
        table.getTableHeader().setReorderingAllowed(false);
    }

    /** Colors for a status word: Pending, Processing, Completed, OK, Rush, Low, Out, Insufficient. */
    public static Color[] statusColors(String status) {
        switch (status) {
            case "Processing":   return PROCESSING;
            case "Completed":
            case "Paid":
            case "OK":           return COMPLETED;
            case "Rush":         return RUSH;
            case "Low":
            case "Unpaid":       return LOW;
            case "Out":
            case "Cancelled":
            case "Void":
            case "Insufficient": return INSUFFICIENT;
            default:             return PENDING;
        }
    }

    /** A status pill as a standalone label (20 px high). */
    public static JLabel badge(String text) {
        Color[] c = statusColors(text);
        return new Badge(text, c[0], c[1]);
    }

    /** Table cell renderer that draws the cell value as a status badge. Use on Status and Priority columns. */
    public static class BadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean selected,
                boolean focus, int row, int col) {
            JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, GRID, 6));
            cell.setOpaque(true);
            cell.setBackground(selected ? t.getSelectionBackground() : t.getBackground());
            if (v != null && !"Normal".equals(String.valueOf(v))) {
                cell.add(badge(String.valueOf(v)));
            } else if (v != null) {
                JLabel plain = new JLabel(String.valueOf(v));
                plain.setForeground(TEXT_SECONDARY);
                cell.add(plain);
            }
            return cell;
        }
    }
}
