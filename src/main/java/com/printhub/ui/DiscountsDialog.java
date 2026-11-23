package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.model.Discount;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Admin: the discounts staff can pick on New Order. Add a percentage or a fixed peso amount, or delete one. */
public class DiscountsDialog extends FormDialog {

    private static final String PERCENT = "Percent off (%)";
    private static final String FIXED = "Peso amount off (" + Ui.PESO + ")";

    private final PrintHubData data = PrintHubData.get();
    private final DefaultTableModel model = Tables.model("Name", "Discount");
    private final JTable table = Pages.table(model, new int[] {1}, new int[] {}, 220, 140);
    private final JTextField nameField = Ui.field(new JTextField(12));
    private final JComboBox<String> kindBox = Ui.combo(new JComboBox<>(new String[] {PERCENT, FIXED}));
    private final JTextField valueField = Ui.field(new JTextField(6));
    private List<Discount> shown = List.of();

    public DiscountsDialog(Window owner) {
        super(owner, "Manage Discounts");
        JScrollPane scroll = Ui.scroll(table);
        scroll.setPreferredSize(new Dimension(420, 160));
        addWide(scroll);

        JPanel form = Ui.panel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        form.add(nameField);
        form.add(kindBox);
        form.add(valueField);
        JButton add = Ui.primary("Add");
        add.addActionListener(e -> add());
        form.add(add);
        JPanel wrap = Ui.panel(new BorderLayout(0, 4));
        wrap.add(Ui.caption("New discount: name, type and amount"), BorderLayout.NORTH);
        wrap.add(form, BorderLayout.CENTER);
        addWide(wrap);

        JButton delete = Ui.danger(new JButton("Delete selected"));
        delete.addActionListener(e -> delete());
        addWide(delete);
        setCloseOnly("Close");
        reload();
    }

    private void reload() {
        shown = data.discounts();
        model.setRowCount(0);
        for (Discount d : shown) {
            model.addRow(new Object[] {d.getName(), d.getKind() == Discount.Kind.PERCENT
                    ? Ui.plain(d.getValue()).replaceAll("\\.00$", "") + "%" : Ui.money(d.getValue())});
        }
    }

    private void add() {
        double value = Ui.parseAmount(valueField.getText());
        if (value < 0) {
            warn("Enter the discount amount as a number.");
            return;
        }
        Discount.Kind kind = kindBox.getSelectedItem() == PERCENT ? Discount.Kind.PERCENT : Discount.Kind.FIXED;
        if (kind == Discount.Kind.PERCENT && value > 100) {
            warn("A percentage cannot be more than 100.");
            return;
        }
        try {
            data.addDiscount(new Discount(nameField.getText().trim(), kind, value));
            nameField.setText("");
            valueField.setText("");
            reload();
        } catch (IllegalArgumentException | SecurityException ex) {
            warn(ex.getMessage());
        }
    }

    private void delete() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) {
            JOptionPane.showMessageDialog(this, "Select a discount in the table first.", getTitle(),
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            data.deleteDiscount(shown.get(row).getName());
            reload();
        } catch (SecurityException ex) {
            warn(ex.getMessage());
        }
    }
}
