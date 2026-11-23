package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.model.JobOrder;
import com.printhub.model.PaymentMethod;
import com.printhub.model.Transaction;
import java.awt.Window;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Staff confirms a customer has paid: cash (with change) or GCash (with the reference number). */
public class MarkPaidDialog extends FormDialog {

    private final PrintHubData data = PrintHubData.get();
    private final JobOrder job;
    private final JComboBox<PaymentMethod> methodBox = Ui.combo(new JComboBox<>(PaymentMethod.values()));
    private final JTextField tenderedField = Ui.field(new JTextField());
    private final JLabel changeLabel = Ui.bold(Ui.money(0));
    private final JTextField referenceField = Ui.field(new JTextField());
    private final JTextField senderField = Ui.field(new JTextField());
    private final JLabel tenderedCaption;
    private final JLabel changeCaption;
    private final JLabel referenceCaption;
    private final JLabel senderCaption;
    private Transaction result;

    public MarkPaidDialog(Window owner, JobOrder job) {
        super(owner, "Mark as Paid");
        this.job = job;
        addWide(Ui.bold(job.getId() + " " + Ui.DOT + " " + job.getCustomer()));
        addWide(Ui.label("Amount due: " + Ui.money(job.getTotal()), PrintHubTheme.FONT_CARD_VALUE, PrintHubTheme.TEXT));
        addRow("Payment method", methodBox);
        tenderedCaption = addRow("Cash received", tenderedField);
        changeCaption = addRow("Change", changeLabel);
        referenceCaption = addRow("GCash reference number", referenceField);
        senderCaption = addRow("GCash number it was sent from", senderField);

        methodBox.addActionListener(e -> updateFields());
        tenderedField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateChange();
            }
        });
        setActions("Mark as Paid", this::save);
        updateFields();
    }

    /** Shows the dialog and returns the recorded transaction, or null if the user cancelled. */
    public Transaction showAndGet() {
        open();
        return result;
    }

    private boolean isCash() {
        return methodBox.getSelectedItem() == PaymentMethod.CASH;
    }

    private void updateFields() {
        boolean cash = isCash();
        tenderedCaption.setVisible(cash);
        tenderedField.setVisible(cash);
        changeCaption.setVisible(cash);
        changeLabel.setVisible(cash);
        referenceCaption.setVisible(!cash);
        referenceField.setVisible(!cash);
        senderCaption.setVisible(!cash);
        senderField.setVisible(!cash);
        pack();
    }

    private void updateChange() {
        double tendered = Ui.parseAmount(tenderedField.getText());
        changeLabel.setText(tendered >= job.getTotal() ? Ui.money(tendered - job.getTotal()) : Ui.DASH);
    }

    private void save() {
        double tendered = isCash() ? Ui.parseAmount(tenderedField.getText()) : 0;
        if (!isCash() && senderField.getText().trim().isEmpty()) {
            warn("Enter the GCash number the payment was sent from.");
            return;
        }
        try {
            result = data.markPaid(job, (PaymentMethod) methodBox.getSelectedItem(), Math.max(0, tendered),
                    referenceField.getText().trim(), isCash() ? null : senderField.getText().trim());
            dispose();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            warn(ex.getMessage());
        }
    }
}
