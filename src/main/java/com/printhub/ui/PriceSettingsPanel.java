package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import com.printhub.data.PrintHubData;
import com.printhub.model.PaperSize;
import com.printhub.model.PriceTable;
import com.printhub.model.Service;
import java.awt.Component;
import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** Price Settings: the price matrix (service x paper size) and the flat rush fee. */
public class PriceSettingsPanel extends javax.swing.JPanel implements Refreshable {

    private static final Service[] SERVICES = Service.values();
    private static final PaperSize[] SIZES = PaperSize.values();

    private final PrintHubData data = PrintHubData.get();
    /** Only admins may change prices; staff see them read-only. */
    private final boolean editable = data.currentUser() != null
            && data.currentUser().getRoleType().allows(com.printhub.model.Permission.EDIT_PRICES);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[] {"Service", "Charged", "A4", "Short", "Long"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return editable && column >= 2;
        }
    };

    public PriceSettingsPanel() {
        initComponents();
        matrixTable.setModel(model);
        Tables.prepare(matrixTable, new int[] {2, 3, 4}, new int[] {}, false);
        matrixTable.setRowHeight(PRICE_ROW_HEIGHT);
        matrixTable.setRowSelectionAllowed(false);
        matrixTable.setSurrendersFocusOnKeystroke(true);
        Tables.widths(matrixTable, 180, 140, 120, 120, 120);
        matrixTable.getColumnModel().getColumn(0).setCellRenderer(cell(FONT_BODY_BOLD, TEXT, SwingConstants.LEFT, false));
        matrixTable.getColumnModel().getColumn(1).setCellRenderer(cell(FONT_BODY, TEXT_SECONDARY, SwingConstants.LEFT, false));
        for (int c = 2; c <= 4; c++) {
            matrixTable.getColumnModel().getColumn(c).setCellRenderer(cell(FONT_BODY, TEXT, SwingConstants.RIGHT, true));
            matrixTable.getColumnModel().getColumn(c).setCellEditor(new PriceEditor());
        }

        revertButton.addActionListener(e -> refresh());
        saveButton.addActionListener(e -> save());
        if (!editable) {
            rushFeeField.setEditable(false);
            revertButton.setEnabled(false);
            saveButton.setEnabled(false);
        }
        refresh();
    }

    // ---- Cells ------------------------------------------------------------------------------

    private static DefaultTableCellRenderer cell(java.awt.Font font, java.awt.Color color, int align, boolean money) {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, false, false, r, c);
                if (money && v instanceof Double) {
                    setText(Ui.money((Double) v));
                }
                setFont(font);
                setForeground(color);
                setHorizontalAlignment(align);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
    }

    /** Right aligned text editor that only accepts a number. */
    private static final class PriceEditor extends DefaultCellEditor {
        private final JTextField field;

        PriceEditor() {
            super(new JTextField());
            field = (JTextField) getComponent();
            field.setHorizontalAlignment(SwingConstants.RIGHT);
            field.setFont(FONT_BODY);
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object value, boolean sel, int row, int col) {
            field.setText(value instanceof Double ? Ui.plain((Double) value) : "");
            field.selectAll();
            return field;
        }

        @Override
        public Object getCellEditorValue() {
            double v = Ui.parseAmount(field.getText());
            return v < 0 ? 0.0 : v;
        }

        @Override
        public boolean stopCellEditing() {
            if (Ui.parseAmount(field.getText()) < 0) {
                java.awt.Toolkit.getDefaultToolkit().beep();
                return false;
            }
            return super.stopCellEditing();
        }
    }

    // ---- Behaviour --------------------------------------------------------------------------

    @Override
    public void refresh() {
        if (matrixTable.isEditing()) {
            matrixTable.getCellEditor().cancelCellEditing();
        }
        PriceTable prices = data.prices();
        model.setRowCount(0);
        for (Service s : SERVICES) {
            Object[] row = new Object[5];
            row[0] = s.label();
            row[1] = s.charged();
            for (int i = 0; i < SIZES.length; i++) {
                row[2 + i] = prices.get(s, SIZES[i]);
            }
            model.addRow(row);
        }
        rushFeeField.setText(Ui.plain(prices.getRushFee()));
    }

    private void save() {
        if (matrixTable.isEditing()) {
            matrixTable.getCellEditor().stopCellEditing();
        }
        double fee = Ui.parseAmount(rushFeeField.getText());
        if (fee < 0) {
            JOptionPane.showMessageDialog(this, "Enter the rush fee as a number.", "Cannot save",
                    JOptionPane.WARNING_MESSAGE);
            rushFeeField.requestFocusInWindow();
            return;
        }
        PriceTable prices = data.prices();
        for (int r = 0; r < SERVICES.length; r++) {
            for (int c = 0; c < SIZES.length; c++) {
                prices.set(SERVICES[r], SIZES[c], (Double) model.getValueAt(r, 2 + c));
            }
        }
        prices.setRushFee(fee);
        try {
            data.savePrices();
        } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot save", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Prices saved. New orders will use them right away.", "Saved",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        headerPanel = new javax.swing.JPanel();
        titleLabel = new javax.swing.JLabel();
        subtitleLabel = new javax.swing.JLabel();
        topPanel = new javax.swing.JPanel();
        cardsRow = new javax.swing.JPanel();
        matrixCard = new com.printhub.ui.CardPanel();
        matrixHeading = new javax.swing.JLabel();
        matrixBody = new javax.swing.JPanel();
        matrixScroll = new javax.swing.JScrollPane();
        matrixTable = new javax.swing.JTable();
        matrixHint = new javax.swing.JLabel();
        rushCard = new com.printhub.ui.CardPanel();
        rushHeading = new javax.swing.JLabel();
        rushCaption = new javax.swing.JLabel();
        rushFeeField = new javax.swing.JTextField();
        rushNote = new javax.swing.JLabel();
        rushFiller = new javax.swing.JPanel();
        buttonsPanel = new javax.swing.JPanel();
        buttonsSpacer = new javax.swing.JPanel();
        revertButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();

        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        headerPanel.setOpaque(false);
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 16, 0));
        headerPanel.setPreferredSize(new java.awt.Dimension(10, 48));
        headerPanel.setLayout(new java.awt.GridBagLayout());

        titleLabel.setText("Price Settings");
        titleLabel.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        headerPanel.add(titleLabel, gridBagConstraints);

        subtitleLabel.setText("Changes apply to new orders.");
        subtitleLabel.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        headerPanel.add(subtitleLabel, gridBagConstraints);
        add(headerPanel, java.awt.BorderLayout.NORTH);

        topPanel.setOpaque(false);
        topPanel.setLayout(new java.awt.BorderLayout());

        cardsRow.setOpaque(false);
        cardsRow.setLayout(new java.awt.GridBagLayout());

        matrixCard.setPreferredSize(new java.awt.Dimension(100, 326));
        matrixCard.setLayout(new java.awt.BorderLayout());

        matrixHeading.setText("Price Matrix (₱)");
        matrixHeading.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        matrixHeading.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 12, 0));
        matrixCard.add(matrixHeading, java.awt.BorderLayout.NORTH);

        matrixBody.setOpaque(false);
        matrixBody.setLayout(new java.awt.BorderLayout());

        matrixScroll.setPreferredSize(new java.awt.Dimension(10, 224));
        com.printhub.ui.Ui.scroll(matrixScroll);

        matrixScroll.setViewportView(matrixTable);
        matrixBody.add(matrixScroll, java.awt.BorderLayout.NORTH);

        matrixHint.setText("Double-click a cell to edit. Press Enter to confirm, Esc to cancel.");
        matrixHint.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        matrixHint.setForeground(new java.awt.Color(75, 85, 99));
        matrixHint.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 0, 0, 0));
        matrixBody.add(matrixHint, java.awt.BorderLayout.CENTER);
        matrixCard.add(matrixBody, java.awt.BorderLayout.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        cardsRow.add(matrixCard, gridBagConstraints);

        rushCard.setPreferredSize(new java.awt.Dimension(296, 100));
        rushCard.setLayout(new java.awt.GridBagLayout());

        rushHeading.setText("Rush Fee");
        rushHeading.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        rushCard.add(rushHeading, gridBagConstraints);

        rushCaption.setText("Flat fee per rush order (₱)");
        rushCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        rushCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        rushCard.add(rushCaption, gridBagConstraints);

        rushFeeField.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        com.printhub.ui.Ui.field(rushFeeField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 0);
        rushCard.add(rushFeeField, gridBagConstraints);

        rushNote.setText("<html>Added once to every order marked Rush.<br>Rush jobs are served first in the Job Queue.</html>");
        rushNote.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        rushNote.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        rushCard.add(rushNote, gridBagConstraints);

        rushFiller.setOpaque(false);
        rushFiller.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        rushCard.add(rushFiller, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 16, 0, 0);
        cardsRow.add(rushCard, gridBagConstraints);
        topPanel.add(cardsRow, java.awt.BorderLayout.NORTH);
        add(topPanel, java.awt.BorderLayout.CENTER);

        buttonsPanel.setOpaque(false);
        buttonsPanel.setLayout(new java.awt.GridBagLayout());

        buttonsSpacer.setOpaque(false);
        buttonsSpacer.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        buttonsPanel.add(buttonsSpacer, gridBagConstraints);

        revertButton.setText("Revert");
        com.printhub.ui.Ui.secondary(revertButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        buttonsPanel.add(revertButton, gridBagConstraints);

        saveButton.setText("Save");
        com.printhub.ui.Ui.primary(saveButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        buttonsPanel.add(saveButton, gridBagConstraints);
        add(buttonsPanel, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JPanel buttonsSpacer;
    private javax.swing.JPanel cardsRow;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JPanel matrixBody;
    private com.printhub.ui.CardPanel matrixCard;
    private javax.swing.JLabel matrixHeading;
    private javax.swing.JLabel matrixHint;
    private javax.swing.JScrollPane matrixScroll;
    private javax.swing.JTable matrixTable;
    private javax.swing.JButton revertButton;
    private javax.swing.JLabel rushCaption;
    private com.printhub.ui.CardPanel rushCard;
    private javax.swing.JTextField rushFeeField;
    private javax.swing.JPanel rushFiller;
    private javax.swing.JLabel rushHeading;
    private javax.swing.JLabel rushNote;
    private javax.swing.JButton saveButton;
    private javax.swing.JLabel subtitleLabel;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JPanel topPanel;
    // End of variables declaration//GEN-END:variables
}
