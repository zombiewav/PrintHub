package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import com.printhub.data.PrintHubData;
import com.printhub.model.JobOrder;
import com.printhub.model.PaperSize;
import com.printhub.model.PaymentMethod;
import com.printhub.model.Service;
import com.printhub.model.StockEstimate;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** New Order: order details, live price breakdown, stock check and payment. */
public class NewOrderPanel extends javax.swing.JPanel implements Refreshable {

    private final PrintHubData data = PrintHubData.get();
    private final ButtonGroup payGroup = new ButtonGroup();
    private final CardLayout payCards;

    private double currentTotal;
    private boolean stockOk = true;

    public NewOrderPanel() {
        initComponents();
        payCards = (CardLayout) payFields.getLayout();
        payGroup.add(cashRadio);
        payGroup.add(gcashRadio);
        stockRows.setLayout(new BoxLayout(stockRows, BoxLayout.Y_AXIS));

        wire();
        refresh();
    }

    // ---- Behaviour --------------------------------------------------------------------------

    private void wire() {
        serviceBox.addActionListener(e -> recalc());
        sizeBox.addActionListener(e -> recalc());
        pagesSpinner.addChangeListener(e -> recalc());
        copiesSpinner.addChangeListener(e -> recalc());
        rushCheck.addActionListener(e -> recalc());
        cashRadio.addActionListener(e -> payCards.show(payFields, "cash"));
        gcashRadio.addActionListener(e -> payCards.show(payFields, "gcash"));
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
        clearButton.addActionListener(e -> clearForm());
        placeButton.addActionListener(e -> placeOrder());
    }

    @Override
    public void refresh() {
        orderNoLabel.setText("Order No. " + data.nextOrderId());
        recalc();
    }

    private void recalc() {
        Service s = (Service) serviceBox.getSelectedItem();
        PaperSize z = (PaperSize) sizeBox.getSelectedItem();
        int p = (Integer) pagesSpinner.getValue();
        int c = (Integer) copiesSpinner.getValue();
        boolean isRush = rushCheck.isSelected();

        double unit = data.prices().get(s, z);
        double sub = unit * p * c;
        double fee = isRush ? data.prices().getRushFee() : 0;
        currentTotal = sub + fee;

        priceTitleLabel.setText(s.label() + " " + Ui.DOT + " " + z.label());
        unitPriceLabel.setText(Ui.money(unit));
        pagesValueLabel.setText(Ui.TIMES + " " + p);
        copiesValueLabel.setText(Ui.TIMES + " " + c);
        subtotalLabel.setText(Ui.money(sub));
        rushFeeLabel.setText(Ui.money(fee));
        totalLabel.setText(Ui.money(currentTotal));
        sheetsLabel.setText(Ui.count(p * c) + " sheets in total");
        amountDueLabel.setText(Ui.money(currentTotal));

        StockEstimate est = data.estimate(s, z, p * c);
        stockOk = est.isSufficient();
        fillStock(est);
        placeButton.setEnabled(stockOk);
        updateChange();
    }

    // ---- Stock check rows (built in code: they change with every edit) ------------------------

    private void fillStock(StockEstimate est) {
        stockRows.removeAll();
        stockRows.add(sectionLabel("Paper (sheets)"));
        for (StockEstimate.Line l : est.getPaper()) {
            stockRows.add(stockRow(l));
        }
        stockRows.add(sectionLabel("Ink (pages remaining)"));
        for (StockEstimate.Line l : est.getInk()) {
            stockRows.add(stockRow(l));
        }
        if (stockOk) {
            stockBanner.setColors(COMPLETED[0], null);
            stockBannerText.setForeground(COMPLETED[1]);
            stockBannerText.setText("All materials available");
        } else {
            stockBanner.setColors(INSUFFICIENT[0], null);
            stockBannerText.setForeground(INSUFFICIENT[1]);
            stockBannerText.setText("Not enough stock. Order cannot be placed.");
        }
        stockRows.revalidate();
        stockRows.repaint();
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = Ui.label(text, FONT_SMALL_BOLD, TEXT_SECONDARY);
        l.setBorder(new EmptyBorder(6, 0, 2, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JPanel stockRow(StockEstimate.Line l) {
        JPanel row = Ui.panel(new GridBagLayout());
        row.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, BORDER), new EmptyBorder(8, 0, 8, 0)));
        row.setAlignmentX(LEFT_ALIGNMENT);

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 1;
        g.anchor = GridBagConstraints.WEST;
        row.add(Ui.bold(l.getLabel()), g);

        g.gridx = 1;
        g.weightx = 0;
        g.anchor = GridBagConstraints.EAST;
        if (!l.isUsed()) {
            row.add(Ui.label("Not used", FONT_SMALL, TEXT_SECONDARY), g);
        } else if (l.isSufficient()) {
            row.add(PrintHubTheme.badge("OK"), g);
        } else {
            row.add(PrintHubTheme.badge("Insufficient"), g);
        }

        g.gridx = 0;
        g.gridy = 1;
        g.gridwidth = 2;
        g.weightx = 1;
        g.anchor = GridBagConstraints.WEST;
        String info = l.isUsed()
                ? "Required " + Ui.count(l.getRequired()) + " " + Ui.DOT + " Available " + Ui.count(l.getAvailable())
                : "Available " + Ui.count(l.getAvailable());
        row.add(Ui.caption(info), g);

        if (l.isUsed() && !l.isSufficient()) {
            g.gridy = 2;
            row.add(Ui.label("Insufficient, short by " + Ui.count(l.shortBy()) + " " + l.getUnit(),
                    FONT_SMALL_BOLD, INSUFFICIENT[1]), g);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    // ---- Payment and placing the order --------------------------------------------------------

    private void updateChange() {
        double paid = Ui.parseAmount(tenderedField.getText());
        if (paid < 0 || paid < currentTotal) {
            changeField.setText(paid < 0 ? "" : Ui.money(0));
        } else {
            changeField.setText(Ui.money(paid - currentTotal));
        }
    }

    private void clearForm() {
        customerField.setText("");
        serviceBox.setSelectedIndex(0);
        sizeBox.setSelectedIndex(0);
        pagesSpinner.setValue(1);
        copiesSpinner.setValue(1);
        rushCheck.setSelected(false);
        cashRadio.setSelected(true);
        payCards.show(payFields, "cash");
        tenderedField.setText("");
        referenceField.setText("");
        senderField.setText("");
        recalc();
    }

    private void placeOrder() {
        String name = customerField.getText().trim();
        if (name.isEmpty()) {
            warn("Enter the customer name.");
            customerField.requestFocusInWindow();
            return;
        }
        PaymentMethod method = cashRadio.isSelected() ? PaymentMethod.CASH : PaymentMethod.GCASH;
        String ref = null;
        double paid = 0;
        if (method == PaymentMethod.CASH) {
            paid = Ui.parseAmount(tenderedField.getText());
            if (paid < currentTotal) {
                warn("Amount tendered is less than the amount due (" + Ui.money(currentTotal) + ").");
                tenderedField.requestFocusInWindow();
                return;
            }
        } else {
            ref = referenceField.getText().trim();
            if (ref.isEmpty()) {
                warn("Enter the GCash reference number.");
                referenceField.requestFocusInWindow();
                return;
            }
            if (senderField.getText().trim().isEmpty()) {
                warn("Enter the sender number.");
                senderField.requestFocusInWindow();
                return;
            }
        }

        JobOrder job = data.placeOrder(name, (Service) serviceBox.getSelectedItem(), (PaperSize) sizeBox.getSelectedItem(),
                (Integer) pagesSpinner.getValue(), (Integer) copiesSpinner.getValue(), rushCheck.isSelected(), method, ref);

        String message = "Order " + job.getId() + " placed for " + name + ".\nTotal: " + Ui.money(currentTotal);
        if (method == PaymentMethod.CASH) {
            message += "\nChange: " + Ui.money(paid - currentTotal);
        }
        JOptionPane.showMessageDialog(this, message, "Order placed", JOptionPane.INFORMATION_MESSAGE);
        clearForm();
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Cannot place order", JOptionPane.WARNING_MESSAGE);
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
        orderNoLabel = new javax.swing.JLabel();
        centerPanel = new javax.swing.JPanel();
        detailsCard = new com.printhub.ui.CardPanel();
        detailsHeading = new javax.swing.JLabel();
        customerCaption = new javax.swing.JLabel();
        customerField = new javax.swing.JTextField();
        serviceCaption = new javax.swing.JLabel();
        serviceBox = new javax.swing.JComboBox<>();
        sizeCaption = new javax.swing.JLabel();
        sizeBox = new javax.swing.JComboBox<>();
        pagesCaption = new javax.swing.JLabel();
        pagesSpinner = new javax.swing.JSpinner();
        copiesCaption = new javax.swing.JLabel();
        copiesSpinner = new javax.swing.JSpinner();
        rushCheck = new javax.swing.JCheckBox();
        rushHint = new javax.swing.JLabel();
        detailsFiller = new javax.swing.JPanel();
        priceCard = new com.printhub.ui.CardPanel();
        priceHeading = new javax.swing.JLabel();
        priceTitleLabel = new javax.swing.JLabel();
        unitName = new javax.swing.JLabel();
        unitPriceLabel = new javax.swing.JLabel();
        pagesName = new javax.swing.JLabel();
        pagesValueLabel = new javax.swing.JLabel();
        copiesName = new javax.swing.JLabel();
        copiesValueLabel = new javax.swing.JLabel();
        priceRule = new javax.swing.JPanel();
        subtotalName = new javax.swing.JLabel();
        subtotalLabel = new javax.swing.JLabel();
        rushFeeName = new javax.swing.JLabel();
        rushFeeLabel = new javax.swing.JLabel();
        priceFiller = new javax.swing.JPanel();
        totalBox = new com.printhub.ui.RoundedPanel();
        totalCaption = new javax.swing.JLabel();
        totalLabel = new javax.swing.JLabel();
        sheetsLabel = new javax.swing.JLabel();
        stockCard = new com.printhub.ui.CardPanel();
        stockHeading = new javax.swing.JLabel();
        stockHost = new javax.swing.JPanel();
        stockRows = new javax.swing.JPanel();
        stockBanner = new com.printhub.ui.RoundedPanel();
        stockBannerText = new javax.swing.JLabel();
        southWrap = new javax.swing.JPanel();
        paymentCard = new com.printhub.ui.CardPanel();
        paymentLeft = new javax.swing.JPanel();
        paymentHeading = new javax.swing.JLabel();
        cashRadio = new javax.swing.JRadioButton();
        gcashRadio = new javax.swing.JRadioButton();
        paymentLeftFiller = new javax.swing.JPanel();
        paymentCenter = new javax.swing.JPanel();
        payFields = new javax.swing.JPanel();
        cashFieldsPanel = new javax.swing.JPanel();
        tenderedCaption = new javax.swing.JLabel();
        changeCaption = new javax.swing.JLabel();
        tenderedField = new javax.swing.JTextField();
        changeField = new javax.swing.JTextField();
        cashFiller = new javax.swing.JPanel();
        gcashFieldsPanel = new javax.swing.JPanel();
        referenceCaption = new javax.swing.JLabel();
        senderCaption = new javax.swing.JLabel();
        referenceField = new javax.swing.JTextField();
        senderField = new javax.swing.JTextField();
        gcashFiller = new javax.swing.JPanel();
        paymentCenterFiller = new javax.swing.JPanel();
        paymentRight = new javax.swing.JPanel();
        dueCaption = new javax.swing.JLabel();
        amountDueLabel = new javax.swing.JLabel();
        paymentRightFiller = new javax.swing.JPanel();
        buttonsPanel = new javax.swing.JPanel();
        clearButton = new javax.swing.JButton();
        placeButton = new javax.swing.JButton();

        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        headerPanel.setOpaque(false);
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 16, 0));
        headerPanel.setPreferredSize(new java.awt.Dimension(10, 48));
        headerPanel.setLayout(new java.awt.GridBagLayout());

        titleLabel.setText("New Order");
        titleLabel.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        headerPanel.add(titleLabel, gridBagConstraints);

        orderNoLabel.setText("Order No. JOB-0236");
        orderNoLabel.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        headerPanel.add(orderNoLabel, gridBagConstraints);
        add(headerPanel, java.awt.BorderLayout.NORTH);

        centerPanel.setOpaque(false);
        centerPanel.setLayout(new java.awt.GridBagLayout());

        detailsCard.setPreferredSize(new java.awt.Dimension(330, 10));
        detailsCard.setLayout(new java.awt.GridBagLayout());

        detailsHeading.setText("Order Details");
        detailsHeading.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        detailsCard.add(detailsHeading, gridBagConstraints);

        customerCaption.setText("Customer name");
        customerCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        customerCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        detailsCard.add(customerCaption, gridBagConstraints);

        com.printhub.ui.Ui.field(customerField);
        customerField.putClientProperty("JTextField.placeholderText", "Enter customer name");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 0);
        detailsCard.add(customerField, gridBagConstraints);

        serviceCaption.setText("Service type");
        serviceCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        serviceCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        detailsCard.add(serviceCaption, gridBagConstraints);

        serviceBox.setModel(new javax.swing.DefaultComboBoxModel<>(com.printhub.model.Service.values()));
        com.printhub.ui.Ui.combo(serviceBox);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 0);
        detailsCard.add(serviceBox, gridBagConstraints);

        sizeCaption.setText("Paper size");
        sizeCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        sizeCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        detailsCard.add(sizeCaption, gridBagConstraints);

        sizeBox.setModel(new javax.swing.DefaultComboBoxModel<>(com.printhub.model.PaperSize.values()));
        com.printhub.ui.Ui.combo(sizeBox);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 0);
        detailsCard.add(sizeBox, gridBagConstraints);

        pagesCaption.setText("Pages");
        pagesCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        pagesCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 8);
        detailsCard.add(pagesCaption, gridBagConstraints);

        pagesSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, 100000, 1));
        com.printhub.ui.Ui.spinner(pagesSpinner);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 8);
        detailsCard.add(pagesSpinner, gridBagConstraints);

        copiesCaption.setText("Copies");
        copiesCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        copiesCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        detailsCard.add(copiesCaption, gridBagConstraints);

        copiesSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, 100000, 1));
        com.printhub.ui.Ui.spinner(copiesSpinner);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 16, 0);
        detailsCard.add(copiesSpinner, gridBagConstraints);

        rushCheck.setText("Rush order");
        rushCheck.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        rushCheck.setOpaque(false);
        rushCheck.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        detailsCard.add(rushCheck, gridBagConstraints);

        rushHint.setText("<html>Adds a flat rush fee and moves the job<br>to the front of the queue.</html>");
        rushHint.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        rushHint.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 24, 0, 0);
        detailsCard.add(rushHint, gridBagConstraints);

        detailsFiller.setOpaque(false);
        detailsFiller.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        detailsCard.add(detailsFiller, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        centerPanel.add(detailsCard, gridBagConstraints);

        priceCard.setPreferredSize(new java.awt.Dimension(300, 10));
        priceCard.setLayout(new java.awt.GridBagLayout());

        priceHeading.setText("Price Breakdown");
        priceHeading.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        priceCard.add(priceHeading, gridBagConstraints);

        priceTitleLabel.setText(" ");
        priceTitleLabel.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        priceCard.add(priceTitleLabel, gridBagConstraints);

        unitName.setText("Unit price");
        unitName.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        priceCard.add(unitName, gridBagConstraints);

        unitPriceLabel.setText(" ");
        unitPriceLabel.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        unitPriceLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        priceCard.add(unitPriceLabel, gridBagConstraints);

        pagesName.setText("Pages");
        pagesName.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        priceCard.add(pagesName, gridBagConstraints);

        pagesValueLabel.setText(" ");
        pagesValueLabel.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pagesValueLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        priceCard.add(pagesValueLabel, gridBagConstraints);

        copiesName.setText("Copies");
        copiesName.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        priceCard.add(copiesName, gridBagConstraints);

        copiesValueLabel.setText(" ");
        copiesValueLabel.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        copiesValueLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        priceCard.add(copiesValueLabel, gridBagConstraints);

        priceRule.setBackground(new java.awt.Color(229, 231, 235));
        priceRule.setPreferredSize(new java.awt.Dimension(1, 1));
        priceRule.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 0);
        priceCard.add(priceRule, gridBagConstraints);

        subtotalName.setText("Subtotal");
        subtotalName.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        priceCard.add(subtotalName, gridBagConstraints);

        subtotalLabel.setText(" ");
        subtotalLabel.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        subtotalLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        priceCard.add(subtotalLabel, gridBagConstraints);

        rushFeeName.setText("Rush fee");
        rushFeeName.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        priceCard.add(rushFeeName, gridBagConstraints);

        rushFeeLabel.setText(" ");
        rushFeeLabel.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        rushFeeLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        priceCard.add(rushFeeLabel, gridBagConstraints);

        priceFiller.setOpaque(false);
        priceFiller.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        priceCard.add(priceFiller, gridBagConstraints);

        totalBox.setFillColor(new java.awt.Color(230, 244, 241));
        totalBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));
        totalBox.setPreferredSize(new java.awt.Dimension(10, 104));
        totalBox.setLayout(new java.awt.GridBagLayout());

        totalCaption.setText("Total amount");
        totalCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        totalCaption.setForeground(new java.awt.Color(15, 118, 110));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        totalBox.add(totalCaption, gridBagConstraints);

        totalLabel.setText(" ");
        totalLabel.setFont(new java.awt.Font("Segoe UI", 1, 28)); // NOI18N
        totalLabel.setForeground(new java.awt.Color(15, 118, 110));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        totalBox.add(totalLabel, gridBagConstraints);

        sheetsLabel.setText(" ");
        sheetsLabel.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        sheetsLabel.setForeground(new java.awt.Color(15, 118, 110));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        totalBox.add(sheetsLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        priceCard.add(totalBox, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 16, 0, 0);
        centerPanel.add(priceCard, gridBagConstraints);

        stockCard.setPreferredSize(new java.awt.Dimension(10, 10));
        stockCard.setLayout(new java.awt.BorderLayout());

        stockHeading.setText("Stock Check");
        stockHeading.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        stockHeading.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 8, 0));
        stockCard.add(stockHeading, java.awt.BorderLayout.NORTH);

        stockHost.setOpaque(false);
        stockHost.setLayout(new java.awt.BorderLayout());

        stockRows.setOpaque(false);
        stockRows.setLayout(new java.awt.BorderLayout());
        stockHost.add(stockRows, java.awt.BorderLayout.NORTH);
        stockCard.add(stockHost, java.awt.BorderLayout.CENTER);

        stockBanner.setFillColor(new java.awt.Color(220, 252, 231));
        stockBanner.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 12));
        stockBanner.setPreferredSize(new java.awt.Dimension(10, 36));
        stockBanner.setLayout(new java.awt.BorderLayout());

        stockBannerText.setText("All materials available");
        stockBannerText.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        stockBannerText.setForeground(new java.awt.Color(21, 128, 61));
        stockBanner.add(stockBannerText, java.awt.BorderLayout.CENTER);
        stockCard.add(stockBanner, java.awt.BorderLayout.SOUTH);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 16, 0, 0);
        centerPanel.add(stockCard, gridBagConstraints);
        add(centerPanel, java.awt.BorderLayout.CENTER);

        southWrap.setOpaque(false);
        southWrap.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 0, 0, 0));
        southWrap.setLayout(new java.awt.BorderLayout());

        paymentCard.setPreferredSize(new java.awt.Dimension(10, 136));
        paymentCard.setLayout(new java.awt.BorderLayout());

        paymentLeft.setOpaque(false);
        paymentLeft.setPreferredSize(new java.awt.Dimension(200, 10));
        paymentLeft.setLayout(new java.awt.GridBagLayout());

        paymentHeading.setText("Payment Method");
        paymentHeading.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        paymentLeft.add(paymentHeading, gridBagConstraints);

        cashRadio.setText("Cash");
        cashRadio.setOpaque(false);
        cashRadio.setSelected(true);
        cashRadio.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(16, 0, 8, 0);
        paymentLeft.add(cashRadio, gridBagConstraints);

        gcashRadio.setText("GCash");
        gcashRadio.setOpaque(false);
        gcashRadio.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        paymentLeft.add(gcashRadio, gridBagConstraints);

        paymentLeftFiller.setOpaque(false);
        paymentLeftFiller.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        paymentLeft.add(paymentLeftFiller, gridBagConstraints);
        paymentCard.add(paymentLeft, java.awt.BorderLayout.WEST);

        paymentCenter.setOpaque(false);
        paymentCenter.setLayout(new java.awt.GridBagLayout());

        payFields.setOpaque(false);
        payFields.setPreferredSize(new java.awt.Dimension(440, 70));
        payFields.setLayout(new java.awt.CardLayout());

        cashFieldsPanel.setOpaque(false);
        cashFieldsPanel.setLayout(new java.awt.GridBagLayout());

        tenderedCaption.setText("Amount tendered");
        tenderedCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        tenderedCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 8);
        cashFieldsPanel.add(tenderedCaption, gridBagConstraints);

        changeCaption.setText("Change");
        changeCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        changeCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        cashFieldsPanel.add(changeCaption, gridBagConstraints);

        tenderedField.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        com.printhub.ui.Ui.field(tenderedField);
        tenderedField.putClientProperty("JTextField.placeholderText", "\u20B10.00");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        cashFieldsPanel.add(tenderedField, gridBagConstraints);

        changeField.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        changeField.setEnabled(false);
        changeField.setEditable(false);
        com.printhub.ui.Ui.field(changeField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        cashFieldsPanel.add(changeField, gridBagConstraints);

        cashFiller.setOpaque(false);
        cashFiller.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        cashFieldsPanel.add(cashFiller, gridBagConstraints);
        payFields.add(cashFieldsPanel, "cash");

        gcashFieldsPanel.setOpaque(false);
        gcashFieldsPanel.setLayout(new java.awt.GridBagLayout());

        referenceCaption.setText("GCash reference number");
        referenceCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        referenceCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 8);
        gcashFieldsPanel.add(referenceCaption, gridBagConstraints);

        senderCaption.setText("Sender number");
        senderCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        senderCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 0);
        gcashFieldsPanel.add(senderCaption, gridBagConstraints);

        com.printhub.ui.Ui.field(referenceField);
        referenceField.putClientProperty("JTextField.placeholderText", "e.g. 7014 882 305 117");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        gcashFieldsPanel.add(referenceField, gridBagConstraints);

        com.printhub.ui.Ui.field(senderField);
        senderField.putClientProperty("JTextField.placeholderText", "09xx xxx xxxx");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        gcashFieldsPanel.add(senderField, gridBagConstraints);

        gcashFiller.setOpaque(false);
        gcashFiller.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gcashFieldsPanel.add(gcashFiller, gridBagConstraints);
        payFields.add(gcashFieldsPanel, "gcash");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(32, 16, 0, 0);
        paymentCenter.add(payFields, gridBagConstraints);

        paymentCenterFiller.setOpaque(false);
        paymentCenterFiller.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        paymentCenter.add(paymentCenterFiller, gridBagConstraints);
        paymentCard.add(paymentCenter, java.awt.BorderLayout.CENTER);

        paymentRight.setOpaque(false);
        paymentRight.setLayout(new java.awt.GridBagLayout());

        dueCaption.setText("Amount due");
        dueCaption.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        dueCaption.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        paymentRight.add(dueCaption, gridBagConstraints);

        amountDueLabel.setText(" ");
        amountDueLabel.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        paymentRight.add(amountDueLabel, gridBagConstraints);

        paymentRightFiller.setOpaque(false);
        paymentRightFiller.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        paymentRight.add(paymentRightFiller, gridBagConstraints);

        buttonsPanel.setOpaque(false);
        buttonsPanel.setLayout(new java.awt.GridBagLayout());

        clearButton.setText("Clear");
        com.printhub.ui.Ui.secondary(clearButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
        buttonsPanel.add(clearButton, gridBagConstraints);

        placeButton.setText("Place Order");
        com.printhub.ui.Ui.primary(placeButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        buttonsPanel.add(placeButton, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        paymentRight.add(buttonsPanel, gridBagConstraints);
        paymentCard.add(paymentRight, java.awt.BorderLayout.EAST);
        southWrap.add(paymentCard, java.awt.BorderLayout.CENTER);
        add(southWrap, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel amountDueLabel;
    private javax.swing.JPanel buttonsPanel;
    private javax.swing.JPanel cashFieldsPanel;
    private javax.swing.JPanel cashFiller;
    private javax.swing.JRadioButton cashRadio;
    private javax.swing.JPanel centerPanel;
    private javax.swing.JLabel changeCaption;
    private javax.swing.JTextField changeField;
    private javax.swing.JButton clearButton;
    private javax.swing.JLabel copiesCaption;
    private javax.swing.JLabel copiesName;
    private javax.swing.JSpinner copiesSpinner;
    private javax.swing.JLabel copiesValueLabel;
    private javax.swing.JLabel customerCaption;
    private javax.swing.JTextField customerField;
    private com.printhub.ui.CardPanel detailsCard;
    private javax.swing.JPanel detailsFiller;
    private javax.swing.JLabel detailsHeading;
    private javax.swing.JLabel dueCaption;
    private javax.swing.JPanel gcashFieldsPanel;
    private javax.swing.JPanel gcashFiller;
    private javax.swing.JRadioButton gcashRadio;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JLabel orderNoLabel;
    private javax.swing.JLabel pagesCaption;
    private javax.swing.JLabel pagesName;
    private javax.swing.JSpinner pagesSpinner;
    private javax.swing.JLabel pagesValueLabel;
    private javax.swing.JPanel payFields;
    private com.printhub.ui.CardPanel paymentCard;
    private javax.swing.JPanel paymentCenter;
    private javax.swing.JPanel paymentCenterFiller;
    private javax.swing.JLabel paymentHeading;
    private javax.swing.JPanel paymentLeft;
    private javax.swing.JPanel paymentLeftFiller;
    private javax.swing.JPanel paymentRight;
    private javax.swing.JPanel paymentRightFiller;
    private javax.swing.JButton placeButton;
    private com.printhub.ui.CardPanel priceCard;
    private javax.swing.JPanel priceFiller;
    private javax.swing.JLabel priceHeading;
    private javax.swing.JPanel priceRule;
    private javax.swing.JLabel priceTitleLabel;
    private javax.swing.JLabel referenceCaption;
    private javax.swing.JTextField referenceField;
    private javax.swing.JCheckBox rushCheck;
    private javax.swing.JLabel rushFeeLabel;
    private javax.swing.JLabel rushFeeName;
    private javax.swing.JLabel rushHint;
    private javax.swing.JLabel senderCaption;
    private javax.swing.JTextField senderField;
    private javax.swing.JComboBox<com.printhub.model.Service> serviceBox;
    private javax.swing.JLabel serviceCaption;
    private javax.swing.JLabel sheetsLabel;
    private javax.swing.JComboBox<com.printhub.model.PaperSize> sizeBox;
    private javax.swing.JLabel sizeCaption;
    private javax.swing.JPanel southWrap;
    private com.printhub.ui.RoundedPanel stockBanner;
    private javax.swing.JLabel stockBannerText;
    private com.printhub.ui.CardPanel stockCard;
    private javax.swing.JLabel stockHeading;
    private javax.swing.JPanel stockHost;
    private javax.swing.JPanel stockRows;
    private javax.swing.JLabel subtotalLabel;
    private javax.swing.JLabel subtotalName;
    private javax.swing.JLabel tenderedCaption;
    private javax.swing.JTextField tenderedField;
    private javax.swing.JLabel titleLabel;
    private com.printhub.ui.RoundedPanel totalBox;
    private javax.swing.JLabel totalCaption;
    private javax.swing.JLabel totalLabel;
    private javax.swing.JLabel unitName;
    private javax.swing.JLabel unitPriceLabel;
    // End of variables declaration//GEN-END:variables
}
