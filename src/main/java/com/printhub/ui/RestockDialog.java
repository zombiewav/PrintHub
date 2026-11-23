package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.model.Supplier;
import com.printhub.model.Supply;
import java.awt.Window;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Record a restocking purchase from a supplier: raises the stock and adds to the purchase history. */
public class RestockDialog extends FormDialog {

    private final PrintHubData data = PrintHubData.get();
    private final Supplier supplier;
    private final JComboBox<Supply> itemBox = Ui.combo(new JComboBox<>());
    private final JSpinner qtySpinner = Ui.spinner(new JSpinner(new SpinnerNumberModel(100, 1, 1_000_000, 10)));
    private final JTextField costField = Ui.field(new JTextField());
    private final JLabel unitCostLabel = Ui.body(Ui.DASH);

    public RestockDialog(Window owner, Supplier supplier) {
        super(owner, "Record Restock " + Ui.DOT + " " + supplier.getName());
        this.supplier = supplier;
        for (Supply s : data.supplies()) {
            if (supplier.getSupplyIds().contains(s.getId())) {
                itemBox.addItem(s);
            }
        }
        addRow("Item", itemBox);
        addRow("Quantity bought", qtySpinner);
        addRow("Total cost (" + Ui.PESO + ")", costField);
        addRow("Cost per unit", unitCostLabel);

        qtySpinner.addChangeListener(e -> updateUnitCost());
        costField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateUnitCost();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateUnitCost();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateUnitCost();
            }
        });
        setActions("Record Restock", this::save);
    }

    private void updateUnitCost() {
        double cost = Ui.parseAmount(costField.getText());
        int qty = (Integer) qtySpinner.getValue();
        unitCostLabel.setText(cost < 0 ? Ui.DASH : Ui.money(cost / qty));
    }

    private void save() {
        Supply s = (Supply) itemBox.getSelectedItem();
        if (s == null) {
            warn(supplier.getName() + " has no supplies listed. Edit the supplier and tick what they provide.");
            return;
        }
        double cost = Ui.parseAmount(costField.getText());
        if (cost < 0) {
            warn("Enter the total cost as a number.");
            return;
        }
        try {
            data.restockFrom(supplier, s, (Integer) qtySpinner.getValue(), cost);
            dispose();
        } catch (IllegalArgumentException ex) {
            warn(ex.getMessage());
        }
    }
}
