package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import com.printhub.data.PrintHubData;
import com.printhub.model.Supply;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/** Inventory > Supplies: search, filter, Add / Edit / Restock / Delete. */
public class SuppliesTab extends javax.swing.JPanel {

    private final PrintHubData data = PrintHubData.get();
    private final DefaultTableModel model = Tables.model("Item ID", "Name", "Category", "Size", "Stock", "Reorder Level",
            "Unit Cost", "Status");
    private final javax.swing.JButton costButton = Ui.secondary(new javax.swing.JButton("Set Unit Cost"));
    private List<Supply> shown = new ArrayList<>();

    public SuppliesTab() {
        initComponents();
        table.setModel(model);
        Tables.prepare(table, new int[] {4, 5, 6}, new int[] {7}, false);
        Tables.widths(table, 90, 230, 130, 70, 110, 110, 100, 90);

        Ui.sized(costButton, 32);
        java.awt.GridBagConstraints costAt = new java.awt.GridBagConstraints();
        costAt.gridx = 7;
        costAt.gridy = 0;
        costAt.insets = new java.awt.Insets(0, 8, 0, 0);
        toolbar.add(costButton, costAt);
        costButton.addActionListener(e -> setUnitCost());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
        categoryBox.addActionListener(e -> refresh());
        addButton.addActionListener(e -> new SupplyDialog(owner(), null).setVisible(true));
        editButton.addActionListener(e -> {
            Supply s = selected();
            if (s != null) {
                new SupplyDialog(owner(), s).setVisible(true);
            }
        });
        restockButton.addActionListener(e -> restock());
        deleteButton.addActionListener(e -> delete());
    }

    /** Number of supplies in stock, for the tab chip. */
    public int getTotal() {
        return data.supplies().size();
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private Supply selected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) {
            JOptionPane.showMessageDialog(this, "Select a supply in the table first.", "No supply selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return shown.get(row);
    }

    private void restock() {
        Supply s = selected();
        if (s == null) {
            return;
        }
        String answer = JOptionPane.showInputDialog(this, "How many " + s.getUnit() + " are you adding to\n"
                + s.displayName() + "?", "Restock", JOptionPane.PLAIN_MESSAGE);
        if (answer == null) {
            return;
        }
        try {
            data.restock(s, Integer.parseInt(answer.trim()));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Enter a whole number greater than zero.", "Cannot restock",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /** What one unit cost the shop. Used for the inventory valuation in Reports. */
    private void setUnitCost() {
        Supply s = selected();
        if (s == null) {
            return;
        }
        String answer = JOptionPane.showInputDialog(this, "Cost of one " + s.getUnit().replaceAll("s$", "") + " of\n"
                + s.displayName() + " (" + Ui.PESO + "):", Ui.plain(s.getUnitCost()), JOptionPane.PLAIN_MESSAGE);
        if (answer == null) {
            return;
        }
        double cost = Ui.parseAmount(answer);
        if (cost < 0) {
            JOptionPane.showMessageDialog(this, "Enter the cost as a number.", "Cannot save", JOptionPane.WARNING_MESSAGE);
            return;
        }
        s.setUnitCost(cost);
        data.updateSupply(s);
    }

    private void delete() {
        Supply s = selected();
        if (s == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete " + s.getId() + " " + Ui.DOT + " " + s.displayName() + "?",
                "Delete supply", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            try {
                data.removeSupply(s.getId());
            } catch (SecurityException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot delete", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    public void refresh() {
        String query = searchField.getText().trim().toLowerCase();
        String cat = (String) categoryBox.getSelectedItem();
        shown = new ArrayList<>();
        model.setRowCount(0);
        int low = 0;
        int out = 0;
        for (Supply s : data.supplies()) {
            boolean matchesText = query.isEmpty() || s.getName().toLowerCase().contains(query)
                    || s.getId().toLowerCase().contains(query);
            boolean matchesCat = cat == null || cat.equals("All categories") || s.getCategory().equals(cat);
            if (!matchesText || !matchesCat) {
                continue;
            }
            shown.add(s);
            model.addRow(new Object[] {s.getId(), s.getName(), s.getCategory(), s.getSize(),
                Ui.count(s.getStock()) + " " + s.getUnit(), Ui.count(s.getReorderLevel()),
                Ui.money(s.getUnitCost()), s.status()});
            if (s.status().equals("Low")) {
                low++;
            } else if (s.status().equals("Out")) {
                out++;
            }
        }
        footerLabel.setText(shown.size() + " items " + Ui.DOT + " " + low + " low " + Ui.DOT + " " + out + " out of stock");
    }

    /** This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        toolbar = new javax.swing.JPanel();
        searchField = new javax.swing.JTextField();
        categoryBox = new javax.swing.JComboBox<>();
        toolbarSpacer = new javax.swing.JPanel();
        addButton = new javax.swing.JButton();
        editButton = new javax.swing.JButton();
        restockButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        footer = new javax.swing.JPanel();
        footerLabel = new javax.swing.JLabel();

        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 0, 0, 0));
        setLayout(new java.awt.BorderLayout());

        toolbar.setOpaque(false);
        toolbar.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 12, 0));
        toolbar.setLayout(new java.awt.GridBagLayout());

        searchField.setPreferredSize(new java.awt.Dimension(280, 32));
        com.printhub.ui.Ui.field(searchField);
        searchField.setPreferredSize(new java.awt.Dimension(280, 32));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name or item ID");
        searchField.putClientProperty("JTextField.leadingIcon", com.printhub.ui.Icons.get("search", 16, com.printhub.ui.PrintHubTheme.TEXT_DISABLED));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        toolbar.add(searchField, gridBagConstraints);

        categoryBox.setPreferredSize(new java.awt.Dimension(176, 32));
        categoryBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] {"All categories", "Paper", "Ink", "Lamination Film", "Photo Paper"}));
        com.printhub.ui.Ui.combo(categoryBox);
        categoryBox.setPreferredSize(new java.awt.Dimension(176, 32));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        toolbar.add(categoryBox, gridBagConstraints);

        toolbarSpacer.setOpaque(false);
        toolbarSpacer.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        toolbar.add(toolbarSpacer, gridBagConstraints);

        addButton.setText("Add");
        com.printhub.ui.Ui.primary(addButton);
        com.printhub.ui.Ui.sized(addButton, 32);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        toolbar.add(addButton, gridBagConstraints);

        editButton.setText("Edit");
        com.printhub.ui.Ui.secondary(editButton);
        com.printhub.ui.Ui.sized(editButton, 32);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        toolbar.add(editButton, gridBagConstraints);

        restockButton.setText("Restock");
        com.printhub.ui.Ui.secondary(restockButton);
        com.printhub.ui.Ui.sized(restockButton, 32);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        toolbar.add(restockButton, gridBagConstraints);

        deleteButton.setText("Delete");
        com.printhub.ui.Ui.danger(deleteButton);
        com.printhub.ui.Ui.sized(deleteButton, 32);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        toolbar.add(deleteButton, gridBagConstraints);
        add(toolbar, java.awt.BorderLayout.NORTH);

        com.printhub.ui.Ui.scroll(scroll);

        scroll.setViewportView(table);
        add(scroll, java.awt.BorderLayout.CENTER);

        footer.setOpaque(false);
        footer.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(229, 231, 235)));
        footer.setLayout(new java.awt.GridBagLayout());

        footerLabel.setText(" ");
        footerLabel.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        footerLabel.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        footer.add(footerLabel, gridBagConstraints);
        add(footer, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JComboBox<String> categoryBox;
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton editButton;
    private javax.swing.JPanel footer;
    private javax.swing.JLabel footerLabel;
    private javax.swing.JButton restockButton;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JTextField searchField;
    private javax.swing.JTable table;
    private javax.swing.JPanel toolbar;
    private javax.swing.JPanel toolbarSpacer;
    // End of variables declaration//GEN-END:variables
}
