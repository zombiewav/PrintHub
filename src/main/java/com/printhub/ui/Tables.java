package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Shared table setup: renderers, header, column widths. */
public final class Tables {

    private Tables() { }

    public static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * @param right          columns drawn right aligned (numbers, amounts)
     * @param badges         columns drawn as status badges
     * @param highlightFirst true to tint row 0 (the next job in the queue)
     */
    public static void prepare(JTable table, int[] right, int[] badges, boolean highlightFirst) {
        styleTable(table);
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        table.setDefaultRenderer(Object.class, new Cell(false, highlightFirst));
        for (int c : right) {
            table.getColumnModel().getColumn(c).setCellRenderer(new Cell(true, highlightFirst));
        }
        for (int c : badges) {
            table.getColumnModel().getColumn(c).setCellRenderer(new BadgeCell(highlightFirst));
        }

        table.getTableHeader().setDefaultRenderer(new Header(right));
    }

    public static void widths(JTable table, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** A plain text cell; the first column of a selected row gets the 3 px teal bar. */
    private static final class Cell extends DefaultTableCellRenderer {
        private final boolean right;
        private final boolean highlightFirst;

        Cell(boolean right, boolean highlightFirst) {
            this.right = right;
            this.highlightFirst = highlightFirst;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean selected,
                boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            boolean marked = selected || (highlightFirst && row == 0);
            setOpaque(true);
            setBackground(marked ? ACCENT_TINT : t.getBackground());
            setForeground(TEXT);
            setFont((highlightFirst && row == 0 && col <= 1) ? FONT_BODY_BOLD : FONT_BODY);
            setHorizontalAlignment(right ? SwingConstants.RIGHT : SwingConstants.LEFT);
            if (col == 0 && marked) {
                setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, ACCENT), new EmptyBorder(0, 5, 0, 8)));
            } else {
                setBorder(new EmptyBorder(0, 8, 0, 8));
            }
            return this;
        }
    }

    private static final class BadgeCell extends BadgeRenderer {
        private final boolean highlightFirst;

        BadgeCell(boolean highlightFirst) {
            this.highlightFirst = highlightFirst;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean selected,
                boolean focus, int row, int col) {
            return super.getTableCellRendererComponent(t, v, selected || (highlightFirst && row == 0), focus, row, col);
        }
    }

    private static final class Header extends DefaultTableCellRenderer {
        private final int[] right;

        Header(int[] right) {
            this.right = right;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean selected,
                boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, false, false, row, col);
            setOpaque(true);
            setBackground(BACKGROUND);
            setForeground(TEXT_SECONDARY);
            setFont(FONT_SMALL_BOLD);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            boolean isRight = false;
            for (int c : right) {
                if (c == col) {
                    isRight = true;
                }
            }
            setHorizontalAlignment(isRight ? JLabel.RIGHT : JLabel.LEFT);
            return this;
        }
    }
}
