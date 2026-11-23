package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;

/** Building blocks for the hand-written screens, so they match the ones built in the form editor. */
final class Pages {

    private Pages() {
    }

    /** Screen title on the left, a gray one-line subtitle on the right. */
    static JPanel header(String title, String subtitle) {
        JPanel header = Ui.panel(new GridBagLayout());
        header.setBorder(new EmptyBorder(0, 0, 16, 0));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;
        header.add(Ui.label(title, new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20), TEXT), g);
        g = new GridBagConstraints();
        g.gridx = 1;
        g.anchor = GridBagConstraints.EAST;
        header.add(Ui.label(subtitle, FONT_BODY, TEXT_SECONDARY), g);
        return header;
    }

    /** A table set up like the others (badge and right-aligned columns, widths). */
    static JTable table(DefaultTableModel model, int[] right, int[] badges, int... widths) {
        JTable table = new JTable(model);
        Tables.prepare(table, right, badges, false);
        Tables.widths(table, widths);
        return table;
    }

    /** Button row under a table: thin rule on top, buttons on the left. */
    static JPanel footer() {
        JPanel footer = Ui.panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        footer.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, BORDER), new EmptyBorder(16, 0, 0, 0)));
        return footer;
    }

    /** A table filling the page with an optional footer underneath. */
    static JPanel tablePage(JTable table, JComponent footerOrNull) {
        JPanel page = Ui.panel(new BorderLayout());
        page.setBorder(new EmptyBorder(16, 0, 0, 0));
        page.add(Ui.scroll(table), BorderLayout.CENTER);
        if (footerOrNull != null) {
            page.add(footerOrNull, BorderLayout.SOUTH);
        }
        return page;
    }

    /** A small gray summary label for a footer. */
    static JLabel note() {
        return Ui.label(" ", FONT_BODY, TEXT_SECONDARY);
    }
}
