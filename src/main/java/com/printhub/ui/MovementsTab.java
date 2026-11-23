package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.model.MovementType;
import com.printhub.model.StockMovement;
import com.printhub.model.Supply;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/** Inventory > Stock Movements: every time stock came in, was used by a job, wasted, damaged or adjusted. */
public class MovementsTab extends JPanel {

    private static final String ALL_ITEMS = "All items";
    private static final String ALL_TYPES = "All types";

    private final PrintHubData data = PrintHubData.get();
    private final DefaultTableModel model = Tables.model("Date / Time", "Item", "Type", "Quantity", "Reason", "Job",
            "Recorded by");
    private final JTable table = Pages.table(model, new int[] {3}, new int[] {}, 150, 260, 90, 100, 120, 100, 120);
    private final JComboBox<Object> itemBox = Ui.combo(new JComboBox<>());
    private final JComboBox<Object> typeBox = Ui.combo(new JComboBox<>());
    private final JLabel note = Pages.note();
    private int total;
    private boolean loading;

    public MovementsTab() {
        setOpaque(false);
        setLayout(new BorderLayout());
        JPanel footer = Pages.footer();
        footer.add(Ui.caption("Item"));
        footer.add(itemBox);
        footer.add(Ui.caption("Type"));
        footer.add(typeBox);
        footer.add(note);
        add(Pages.tablePage(table, footer), BorderLayout.CENTER);

        typeBox.addItem(ALL_TYPES);
        for (MovementType t : MovementType.values()) {
            typeBox.addItem(t);
        }
        itemBox.addActionListener(e -> {
            if (!loading) {
                refresh();
            }
        });
        typeBox.addActionListener(e -> {
            if (!loading) {
                refresh();
            }
        });
    }

    /** Number of rows after the filters, for the tab chip. */
    public int getTotal() {
        return total;
    }

    /** "+50" when stock went up, "-5" when it went down. An adjustment keeps the sign it was saved with. */
    static String signed(StockMovement m) {
        int qty = m.getQty();
        boolean up = m.getType() == MovementType.IN || (m.getType() == MovementType.ADJUST && qty > 0);
        return (up ? "+" : "-") + Ui.count(Math.abs(qty));
    }

    public void refresh() {
        loading = true;
        Object keepItem = itemBox.getSelectedItem();
        itemBox.removeAllItems();
        itemBox.addItem(ALL_ITEMS);
        for (Supply s : data.supplies()) {
            itemBox.addItem(s);
            if (keepItem == s) {
                itemBox.setSelectedItem(s);
            }
        }
        loading = false;

        Object itemChoice = itemBox.getSelectedItem();
        Object typeChoice = typeBox.getSelectedItem();
        List<StockMovement> rows = data.stockMovements(itemChoice instanceof Supply ? (Supply) itemChoice : null);
        model.setRowCount(0);
        total = 0;
        for (StockMovement m : rows) {
            if (typeChoice instanceof MovementType && m.getType() != typeChoice) {
                continue;
            }
            total++;
            model.addRow(new Object[] {m.getTime().format(Ui.FULL_STAMP), m.getSupply().displayName(),
                m.getType().label(), signed(m) + " " + m.getSupply().getUnit(),
                m.getReason() == null ? Ui.DASH : m.getReason().label(), m.getJobId() == null ? Ui.DASH : m.getJobId(),
                m.getStaff() == null ? Ui.DASH : m.getStaff()});
        }
        note.setText(total + " movements");
    }
}
