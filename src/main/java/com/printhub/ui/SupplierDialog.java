package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.model.Supplier;
import com.printhub.model.Supply;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/** Add or edit a supplier: name, address, phone, and which supplies they provide. */
public class SupplierDialog extends FormDialog {

    private final PrintHubData data = PrintHubData.get();
    private final Supplier editing;
    private final JTextField nameField = Ui.field(new JTextField(24));
    private final JTextField addressField = Ui.field(new JTextField());
    private final JTextField phoneField = Ui.field(new JTextField());
    private final List<JCheckBox> boxes = new ArrayList<>();
    private final List<Supply> choices = new ArrayList<>();

    /** @param supplier the supplier to edit, or null to add a new one */
    public SupplierDialog(Window owner, Supplier supplier) {
        super(owner, supplier == null ? "Add Supplier" : "Edit Supplier");
        this.editing = supplier;
        addRow("Supplier name", nameField);
        addRow("Address", addressField);
        addRow("Contact number", phoneField);

        JPanel list = Ui.panel(null);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (Supply s : data.supplies()) {
            JCheckBox box = new JCheckBox(s.getId() + " " + Ui.DOT + " " + s.displayName());
            box.setOpaque(false);
            box.setFont(PrintHubTheme.FONT_BODY);
            box.setSelected(supplier != null && supplier.getSupplyIds().contains(s.getId()));
            boxes.add(box);
            choices.add(s);
            list.add(box);
        }
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(360, 150));
        addRow("Supplies they provide", scroll);

        if (supplier != null) {
            nameField.setText(supplier.getName());
            addressField.setText(supplier.getAddress());
            phoneField.setText(supplier.getPhone());
        }
        setActions("Save", this::save);
    }

    private void save() {
        List<Supply> provides = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).isSelected()) {
                provides.add(choices.get(i));
            }
        }
        try {
            if (editing == null) {
                data.addSupplier(nameField.getText(), addressField.getText().trim(), phoneField.getText().trim(), provides);
            } else {
                editing.setName(nameField.getText().trim());
                editing.setAddress(addressField.getText().trim());
                editing.setPhone(phoneField.getText().trim());
                editing.getSupplyIds().clear();
                for (Supply s : provides) {
                    editing.getSupplyIds().add(s.getId());
                }
                data.updateSupplier(editing);
            }
            dispose();
        } catch (IllegalArgumentException ex) {
            warn(ex.getMessage());
        }
    }
}
