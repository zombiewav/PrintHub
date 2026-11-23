package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.model.Restock;
import com.printhub.model.Supplier;
import com.printhub.model.Supply;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Suppliers: who the shop buys from, restocking purchases and their cost, and the reorder reminder. */
public class SuppliersPanel extends JPanel implements Refreshable {

    private static final String ALL = "All suppliers";

    private final PrintHubData data = PrintHubData.get();

    private final DefaultTableModel supplierModel = Tables.model("ID", "Supplier", "Address", "Contact", "Supplies");
    private final JTable supplierTable = Pages.table(supplierModel, new int[] {}, new int[] {}, 80, 200, 240, 140, 300);
    private final JLabel supplierNote = Pages.note();

    private final DefaultTableModel historyModel = Tables.model("Date", "Supplier", "Item", "Quantity", "Cost per unit",
            "Total cost", "Recorded by");
    private final JTable historyTable = Pages.table(historyModel, new int[] {3, 4, 5}, new int[] {}, 150, 190, 220, 90, 110,
            110, 120);
    private final JComboBox<Object> historyFilter = Ui.combo(new JComboBox<>());
    private final JLabel historyNote = Pages.note();

    private final DefaultTableModel reorderModel = Tables.model("Item", "In stock", "Reorder level", "Status", "Buy from");
    private final JTable reorderTable = Pages.table(reorderModel, new int[] {1, 2}, new int[] {3}, 260, 120, 120, 90, 320);
    private final JLabel reorderNote = Pages.note();

    private final TabHeader suppliersHeader;
    private final TabHeader historyHeader;
    private final TabHeader reorderHeader;

    private List<Supplier> shown = new ArrayList<>();
    private boolean loadingFilter;

    public SuppliersPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        add(Pages.header("Suppliers", "Restocking from a supplier adds to stock and is recorded as an expense."),
                BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        suppliersHeader = Ui.addTab(tabs, "Suppliers", suppliersPage(), 0);
        historyHeader = Ui.addTab(tabs, "Purchase History", historyPage(), 0);
        reorderHeader = Ui.addTab(tabs, "Reorder Reminder", reorderPage(), 0);
        Ui.syncTabs(tabs);
        CardPanel card = new CardPanel(new BorderLayout());
        card.add(tabs, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        historyFilter.addActionListener(e -> {
            if (!loadingFilter) {
                refreshHistory();
            }
        });
        refresh();
    }

    // ---- Tabs -------------------------------------------------------------------------------

    private JPanel suppliersPage() {
        JButton add = Ui.primary("Add Supplier");
        JButton edit = Ui.secondary("Edit");
        JButton restock = Ui.primary("Record Restock");
        JButton delete = Ui.danger(new JButton("Delete"));
        add.addActionListener(e -> new SupplierDialog(owner(), null).open());
        edit.addActionListener(e -> {
            Supplier s = selected();
            if (s != null) {
                new SupplierDialog(owner(), s).open();
            }
        });
        restock.addActionListener(e -> {
            Supplier s = selected();
            if (s != null) {
                new RestockDialog(owner(), s).open();
            }
        });
        delete.addActionListener(e -> delete());
        JPanel footer = Pages.footer();
        footer.add(add);
        footer.add(edit);
        footer.add(restock);
        footer.add(delete);
        footer.add(supplierNote);
        return Pages.tablePage(supplierTable, footer);
    }

    private JPanel historyPage() {
        JPanel footer = Pages.footer();
        footer.add(Ui.caption("Show"));
        footer.add(historyFilter);
        footer.add(historyNote);
        return Pages.tablePage(historyTable, footer);
    }

    private JPanel reorderPage() {
        JPanel footer = Pages.footer();
        footer.add(reorderNote);
        return Pages.tablePage(reorderTable, footer);
    }

    // ---- Actions ----------------------------------------------------------------------------

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private Supplier selected() {
        int row = supplierTable.getSelectedRow();
        if (row < 0 || row >= shown.size()) {
            JOptionPane.showMessageDialog(this, "Select a supplier in the table first.", "No supplier selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return shown.get(row);
    }

    private void delete() {
        Supplier s = selected();
        if (s == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete " + s.getName() + "?\nTheir purchase history is kept.",
                "Delete supplier", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            try {
                data.deleteSupplier(s.getId());
            } catch (SecurityException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot delete", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    // ---- Showing the data -------------------------------------------------------------------

    @Override
    public void refresh() {
        refreshSuppliers();
        refreshFilter();
        refreshHistory();
        refreshReorder();
    }

    private void refreshSuppliers() {
        shown = data.supplierList();
        supplierModel.setRowCount(0);
        for (Supplier s : shown) {
            List<String> names = new ArrayList<>();
            for (String id : s.getSupplyIds()) {
                Supply supply = data.findSupply(id);
                if (supply != null) {
                    names.add(supply.shortName());
                }
            }
            supplierModel.addRow(new Object[] {s.getId(), s.getName(), s.getAddress(), s.getPhone(),
                String.join(", ", names)});
        }
        suppliersHeader.setCount(shown.size());
        supplierNote.setText(shown.size() + " suppliers " + Ui.DOT + " total restocking expenses "
                + Ui.money(data.supplierExpenses(null)));
    }

    private void refreshFilter() {
        Object keep = historyFilter.getSelectedItem();
        loadingFilter = true;
        historyFilter.removeAllItems();
        historyFilter.addItem(ALL);
        for (Supplier s : shown) {
            historyFilter.addItem(s);
            if (keep instanceof Supplier && ((Supplier) keep).getId().equals(s.getId())) {
                historyFilter.setSelectedItem(s);
            }
        }
        loadingFilter = false;
    }

    private void refreshHistory() {
        Object choice = historyFilter.getSelectedItem();
        Supplier filter = choice instanceof Supplier ? (Supplier) choice : null;
        List<Restock> rows = data.supplierHistory(filter);
        historyModel.setRowCount(0);
        for (Restock r : rows) {
            historyModel.addRow(new Object[] {r.getTime().format(Ui.FULL_STAMP), r.getSupplier().getName(),
                r.getSupply() == null ? Ui.DASH : r.getSupply().displayName(), Ui.count(r.getQty()),
                Ui.money(r.getTotalCost() / r.getQty()), Ui.money(r.getTotalCost()), r.getStaff()});
        }
        historyHeader.setCount(rows.size());
        historyNote.setText("Total restocking expenses: " + Ui.money(data.supplierExpenses(filter)));
    }

    private void refreshReorder() {
        List<Supply> low = data.lowStock();
        reorderModel.setRowCount(0);
        for (Supply s : low) {
            List<String> names = new ArrayList<>();
            for (Supplier supplier : data.suppliersFor(s)) {
                names.add(supplier.getName() + (supplier.getPhone() == null || supplier.getPhone().isEmpty()
                        ? "" : " (" + supplier.getPhone() + ")"));
            }
            reorderModel.addRow(new Object[] {s.displayName(), Ui.count(s.getStock()) + " " + s.getUnit(),
                Ui.count(s.getReorderLevel()), s.status(), names.isEmpty() ? "No supplier listed" : String.join(", ", names)});
        }
        reorderHeader.setCount(low.size());
        reorderNote.setText(low.isEmpty() ? "Everything is above its reorder level."
                : low.size() + " items need restocking.");
    }
}
