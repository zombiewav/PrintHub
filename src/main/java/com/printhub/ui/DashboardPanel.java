package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import com.printhub.data.PrintHubData;
import com.printhub.model.JobOrder;
import com.printhub.model.PaymentMethod;
import com.printhub.model.Supply;
import com.printhub.model.Transaction;
import com.printhub.model.WasteRecord;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.table.DefaultTableModel;

/** Dashboard: summary cards, next job in the queue, low-stock alerts, today's sales and waste. */
public class DashboardPanel extends javax.swing.JPanel implements Refreshable {

    private final PrintHubData data = PrintHubData.get();
    private final Consumer<String> navigate;
    private final DefaultTableModel lowModel = Tables.model("Item", "Stock", "Reorder", "Status");
    private final JProgressBar[] wasteBar;
    private final JLabel[] wasteCount;

    public DashboardPanel(Consumer<String> navigate) {
        this.navigate = navigate;
        initComponents();
        dateLabel.setText(LocalDate.now().format(Ui.LONG_DATE));
        wasteBar = new JProgressBar[] {wasteBar0, wasteBar1, wasteBar2};
        wasteCount = new JLabel[] {wasteCount0, wasteCount1, wasteCount2};

        lowTable.setModel(lowModel);
        Tables.prepare(lowTable, new int[] {1, 2}, new int[] {3}, false);
        Tables.widths(lowTable, 220, 70, 70, 80);
        lowTable.setRowSelectionAllowed(false);
        refresh();
    }

    // ---- Next in Queue (built in code because it changes with the queue) ----------------------

    private void fillNext() {
        nextBody.removeAll();
        JobOrder job = data.peekNext();
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        if (job == null) {
            nextBody.add(Ui.caption("No pending jobs. The queue is empty."), g);
            g.gridy = 1;
            g.weighty = 1;
            nextBody.add(Ui.panel(null), g);
            nextBody.revalidate();
            nextBody.repaint();
            return;
        }

        JPanel top = Ui.panel(new java.awt.BorderLayout());
        JPanel left = Ui.flow(FlowLayout.LEFT, 8);
        left.add(Ui.label(job.getId(), FONT_HEADING.deriveFont(18f), TEXT));
        if (job.isRush()) {
            left.add(PrintHubTheme.badge("Rush"));
        }
        top.add(left, java.awt.BorderLayout.WEST);
        JPanel right = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.add(PrintHubTheme.badge("Pending"));
        top.add(right, java.awt.BorderLayout.EAST);
        nextBody.add(top, g);

        JPanel info = Ui.panel(new GridLayout(2, 3, 16, 6));
        info.add(infoCell("Customer", job.getCustomer()));
        info.add(infoCell("Service", job.getService().label()));
        info.add(infoCell("Received", job.getReceived().format(Ui.TIME)));
        info.add(infoCell("Paper size", job.getSize().label()));
        info.add(infoCell("Pages " + Ui.TIMES + " copies",
                job.getPages() + " " + Ui.TIMES + " " + job.getCopies() + " (" + job.sheets() + " sheets)"));
        info.add(infoCell("Queue #", "1"));
        g.gridy = 1;
        g.insets = new Insets(8, 0, 0, 0);
        nextBody.add(info, g);

        JPanel buttons = Ui.flow(FlowLayout.LEFT, 8);
        JButton start = Ui.primary("Start Job");
        start.addActionListener(e -> data.startNextJob());
        JButton open = Ui.secondary("Open Job Queue");
        open.addActionListener(e -> navigate.accept("queue"));
        buttons.add(start);
        buttons.add(open);
        g.gridy = 2;
        g.insets = new Insets(12, 0, 0, 0);
        nextBody.add(buttons, g);

        g.gridy = 3;
        g.weighty = 1;
        nextBody.add(Ui.panel(null), g);
        nextBody.revalidate();
        nextBody.repaint();
    }

    private static JPanel infoCell(String caption, String value) {
        JPanel p = Ui.panel(new GridLayout(2, 1, 0, 0));
        p.add(Ui.caption(caption));
        p.add(Ui.bold(value));
        return p;
    }

    // ---- Refresh ----------------------------------------------------------------------------

    @Override
    public void refresh() {
        List<JobOrder> pending = data.pendingJobs();
        int rush = 0;
        for (JobOrder j : pending) {
            if (j.isRush()) {
                rush++;
            }
        }
        List<Supply> low = data.lowStock();
        int out = 0;
        for (Supply s : low) {
            if (s.status().equals("Out")) {
                out++;
            }
        }

        double total = 0;
        double cash = 0;
        double gcash = 0;
        int txns = 0;
        LocalDate today = LocalDate.now();
        for (Transaction t : data.transactions()) {
            if (t.getDateTime().toLocalDate().equals(today)) {
                txns++;
                total += t.getAmount();
                if (t.getMethod() == PaymentMethod.CASH) {
                    cash += t.getAmount();
                } else {
                    gcash += t.getAmount();
                }
            }
        }

        pendingValue.setText(String.valueOf(pending.size()));
        pendingNote.setText("Waiting in the queue");
        rushValue.setText(String.valueOf(rush));
        rushNote.setText("Served before regular jobs");
        salesValue.setText(Ui.money(total));
        salesNote.setText(txns + " transactions");
        lowValue.setText(String.valueOf(low.size()));
        lowNote.setText("Includes " + out + " out of stock");

        fillNext();

        lowModel.setRowCount(0);
        for (Supply s : low) {
            lowModel.addRow(new Object[] {s.shortName(), Ui.count(s.getStock()), Ui.count(s.getReorderLevel()), s.status()});
        }

        int cashPct = total == 0 ? 0 : (int) Math.round(cash * 100 / total);
        int gcashPct = total == 0 ? 0 : 100 - cashPct;
        salesTotalLabel.setText(Ui.money(total));
        cashText.setText(Ui.money(cash) + " " + Ui.DOT + " " + cashPct + "%");
        gcashText.setText(Ui.money(gcash) + " " + Ui.DOT + " " + gcashPct + "%");
        cashBar.setValue(cashPct);
        gcashBar.setValue(gcashPct);

        int[] counts = new int[3];
        int items = 0;
        LocalDate from = today.minusDays(6);
        for (WasteRecord w : data.wasteLog()) {
            if (!w.getDateTime().toLocalDate().isBefore(from)) {
                counts[w.getReason().ordinal()] += w.getQty();
                items += w.getQty();
            }
        }
        int max = Math.max(1, Math.max(counts[0], Math.max(counts[1], counts[2])));
        wasteSummary.setText("Last 7 days " + Ui.DOT + " " + items + " items");
        for (int i = 0; i < 3; i++) {
            wasteCount[i].setText(String.valueOf(counts[i]));
            wasteBar[i].setValue(counts[i] * 100 / max);
        }
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
        dateLabel = new javax.swing.JLabel();
        bodyPanel = new javax.swing.JPanel();
        pendingCard = new com.printhub.ui.CardPanel();
        pendingTitle = new javax.swing.JLabel();
        pendingValue = new javax.swing.JLabel();
        pendingNote = new javax.swing.JLabel();
        pendingTile = new com.printhub.ui.IconTile();
        rushCard = new com.printhub.ui.CardPanel();
        rushTitle = new javax.swing.JLabel();
        rushValue = new javax.swing.JLabel();
        rushNote = new javax.swing.JLabel();
        rushTile = new com.printhub.ui.IconTile();
        salesCard = new com.printhub.ui.CardPanel();
        salesTitle = new javax.swing.JLabel();
        salesValue = new javax.swing.JLabel();
        salesNote = new javax.swing.JLabel();
        salesTile = new com.printhub.ui.IconTile();
        lowCard = new com.printhub.ui.CardPanel();
        lowTitle = new javax.swing.JLabel();
        lowValue = new javax.swing.JLabel();
        lowNote = new javax.swing.JLabel();
        lowTile = new com.printhub.ui.IconTile();
        nextCard = new com.printhub.ui.CardPanel();
        nextHeading = new javax.swing.JLabel();
        nextBody = new javax.swing.JPanel();
        nextSampleTitle = new javax.swing.JLabel();
        nextSampleInfo = new javax.swing.JLabel();
        alertsCard = new com.printhub.ui.CardPanel();
        lowHeading = new javax.swing.JLabel();
        lowScroll = new javax.swing.JScrollPane();
        lowTable = new javax.swing.JTable();
        salesSplitCard = new com.printhub.ui.CardPanel();
        salesHead = new javax.swing.JPanel();
        salesHeading = new javax.swing.JLabel();
        salesTotalLabel = new javax.swing.JLabel();
        salesRows = new javax.swing.JPanel();
        cashLabel = new javax.swing.JLabel();
        cashText = new javax.swing.JLabel();
        cashBar = new javax.swing.JProgressBar();
        gcashLabel = new javax.swing.JLabel();
        gcashText = new javax.swing.JLabel();
        gcashBar = new javax.swing.JProgressBar();
        salesFiller = new javax.swing.JPanel();
        wasteCard = new com.printhub.ui.CardPanel();
        wasteHead = new javax.swing.JPanel();
        wasteHeading = new javax.swing.JLabel();
        wasteSummary = new javax.swing.JLabel();
        wasteRows = new javax.swing.JPanel();
        wasteName0 = new javax.swing.JLabel();
        wasteBar0 = new javax.swing.JProgressBar();
        wasteCount0 = new javax.swing.JLabel();
        wasteName1 = new javax.swing.JLabel();
        wasteBar1 = new javax.swing.JProgressBar();
        wasteCount1 = new javax.swing.JLabel();
        wasteName2 = new javax.swing.JLabel();
        wasteBar2 = new javax.swing.JProgressBar();
        wasteCount2 = new javax.swing.JLabel();
        wasteFiller = new javax.swing.JPanel();

        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        headerPanel.setOpaque(false);
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 16, 0));
        headerPanel.setPreferredSize(new java.awt.Dimension(10, 48));
        headerPanel.setLayout(new java.awt.GridBagLayout());

        titleLabel.setText("Dashboard");
        titleLabel.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        headerPanel.add(titleLabel, gridBagConstraints);

        dateLabel.setText("November 23, 2026");
        dateLabel.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        headerPanel.add(dateLabel, gridBagConstraints);
        add(headerPanel, java.awt.BorderLayout.NORTH);

        bodyPanel.setOpaque(false);
        bodyPanel.setLayout(new java.awt.GridBagLayout());

        pendingCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 16, 8, 16));
        pendingCard.setPreferredSize(new java.awt.Dimension(10, 96));
        pendingCard.setLayout(new java.awt.GridBagLayout());

        pendingTitle.setText("Pending Jobs");
        pendingTitle.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        pendingTitle.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        pendingCard.add(pendingTitle, gridBagConstraints);

        pendingValue.setText("7");
        pendingValue.setFont(new java.awt.Font("Segoe UI", 1, 28)); // NOI18N
        pendingValue.setForeground(new java.awt.Color(17, 24, 39));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        pendingCard.add(pendingValue, gridBagConstraints);

        pendingNote.setText("Waiting in the queue");
        pendingNote.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        pendingNote.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        pendingCard.add(pendingNote, gridBagConstraints);

        pendingTile.setIconName("clock");
        pendingTile.setIconColor(new java.awt.Color(15, 118, 110));
        pendingTile.setTileColor(new java.awt.Color(230, 244, 241));
        pendingTile.setIconSize(20);
        pendingTile.setArc(8);
        pendingTile.setPreferredSize(new java.awt.Dimension(36, 36));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 0, 0);
        pendingCard.add(pendingTile, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 16);
        bodyPanel.add(pendingCard, gridBagConstraints);

        rushCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 16, 8, 16));
        rushCard.setPreferredSize(new java.awt.Dimension(10, 96));
        rushCard.setLayout(new java.awt.GridBagLayout());

        rushTitle.setText("Rush Jobs");
        rushTitle.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        rushTitle.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        rushCard.add(rushTitle, gridBagConstraints);

        rushValue.setText("2");
        rushValue.setFont(new java.awt.Font("Segoe UI", 1, 28)); // NOI18N
        rushValue.setForeground(new java.awt.Color(194, 65, 12));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        rushCard.add(rushValue, gridBagConstraints);

        rushNote.setText("Served before regular jobs");
        rushNote.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        rushNote.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rushCard.add(rushNote, gridBagConstraints);

        rushTile.setIconName("bolt");
        rushTile.setIconColor(new java.awt.Color(194, 65, 12));
        rushTile.setTileColor(new java.awt.Color(255, 237, 213));
        rushTile.setIconSize(20);
        rushTile.setArc(8);
        rushTile.setPreferredSize(new java.awt.Dimension(36, 36));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 0, 0);
        rushCard.add(rushTile, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 16);
        bodyPanel.add(rushCard, gridBagConstraints);

        salesCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 16, 8, 16));
        salesCard.setPreferredSize(new java.awt.Dimension(10, 96));
        salesCard.setLayout(new java.awt.GridBagLayout());

        salesTitle.setText("Today’s Sales");
        salesTitle.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        salesTitle.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        salesCard.add(salesTitle, gridBagConstraints);

        salesValue.setText("\u20b14,090.00");
        salesValue.setFont(new java.awt.Font("Segoe UI", 1, 28)); // NOI18N
        salesValue.setForeground(new java.awt.Color(17, 24, 39));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        salesCard.add(salesValue, gridBagConstraints);

        salesNote.setText("12 transactions");
        salesNote.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        salesNote.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        salesCard.add(salesNote, gridBagConstraints);

        salesTile.setIconName("peso");
        salesTile.setIconColor(new java.awt.Color(15, 118, 110));
        salesTile.setTileColor(new java.awt.Color(230, 244, 241));
        salesTile.setIconSize(20);
        salesTile.setArc(8);
        salesTile.setPreferredSize(new java.awt.Dimension(36, 36));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 0, 0);
        salesCard.add(salesTile, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 16);
        bodyPanel.add(salesCard, gridBagConstraints);

        lowCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 16, 8, 16));
        lowCard.setPreferredSize(new java.awt.Dimension(10, 96));
        lowCard.setLayout(new java.awt.GridBagLayout());

        lowTitle.setText("Low-Stock Items");
        lowTitle.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lowTitle.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        lowCard.add(lowTitle, gridBagConstraints);

        lowValue.setText("5");
        lowValue.setFont(new java.awt.Font("Segoe UI", 1, 28)); // NOI18N
        lowValue.setForeground(new java.awt.Color(146, 64, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        lowCard.add(lowValue, gridBagConstraints);

        lowNote.setText("Includes 1 out of stock");
        lowNote.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        lowNote.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        lowCard.add(lowNote, gridBagConstraints);

        lowTile.setIconName("alert");
        lowTile.setIconColor(new java.awt.Color(146, 64, 14));
        lowTile.setTileColor(new java.awt.Color(254, 243, 199));
        lowTile.setIconSize(20);
        lowTile.setArc(8);
        lowTile.setPreferredSize(new java.awt.Dimension(36, 36));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 0, 0);
        lowCard.add(lowTile, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 0);
        bodyPanel.add(lowCard, gridBagConstraints);

        nextCard.setPreferredSize(new java.awt.Dimension(10, 10));
        nextCard.setLayout(new java.awt.BorderLayout());

        nextHeading.setText("Next in Queue");
        nextHeading.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        nextHeading.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 12, 0));
        nextCard.add(nextHeading, java.awt.BorderLayout.NORTH);

        nextBody.setOpaque(false);
        nextBody.setLayout(new java.awt.GridBagLayout());
        nextSampleTitle.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        nextSampleTitle.setText("JOB-0231 \u00b7 Juan Dela Cruz");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        nextBody.add(nextSampleTitle, gridBagConstraints);

        nextSampleInfo.setText("Colored Printing \u00b7 A4 \u00b7 12 pages \u00d7 5 copies");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
        nextBody.add(nextSampleInfo, gridBagConstraints);
        nextCard.add(nextBody, java.awt.BorderLayout.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 16);
        bodyPanel.add(nextCard, gridBagConstraints);

        alertsCard.setPreferredSize(new java.awt.Dimension(10, 10));
        alertsCard.setLayout(new java.awt.BorderLayout());

        lowHeading.setText("Low-Stock Alerts");
        lowHeading.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lowHeading.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 12, 0));
        alertsCard.add(lowHeading, java.awt.BorderLayout.NORTH);

        com.printhub.ui.Ui.scroll(lowScroll);
        lowTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Item", "Stock", "Reorder", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });

        lowScroll.setViewportView(lowTable);
        alertsCard.add(lowScroll, java.awt.BorderLayout.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 16, 0);
        bodyPanel.add(alertsCard, gridBagConstraints);

        salesSplitCard.setPreferredSize(new java.awt.Dimension(10, 10));
        salesSplitCard.setLayout(new java.awt.BorderLayout());

        salesHead.setOpaque(false);
        salesHead.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 20, 0));
        salesHead.setLayout(new java.awt.GridBagLayout());

        salesHeading.setText("Today’s Sales");
        salesHeading.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        salesHead.add(salesHeading, gridBagConstraints);

        salesTotalLabel.setText("\u20b14,090.00");
        salesTotalLabel.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        salesHead.add(salesTotalLabel, gridBagConstraints);
        salesSplitCard.add(salesHead, java.awt.BorderLayout.NORTH);

        salesRows.setOpaque(false);
        salesRows.setLayout(new java.awt.GridBagLayout());

        cashLabel.setText("Cash");
        cashLabel.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        salesRows.add(cashLabel, gridBagConstraints);

        cashText.setText("\u20b12,570.00 \u00b7 63%");
        cashText.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        salesRows.add(cashText, gridBagConstraints);

        cashBar.setForeground(new java.awt.Color(15, 118, 110));
        cashBar.setBorderPainted(false);
        cashBar.setValue(63);
        cashBar.setValue(0);
        cashBar.setPreferredSize(new java.awt.Dimension(10, 8));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 16, 0);
        salesRows.add(cashBar, gridBagConstraints);

        gcashLabel.setText("GCash");
        gcashLabel.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        salesRows.add(gcashLabel, gridBagConstraints);

        gcashText.setText("\u20b11,520.00 \u00b7 37%");
        gcashText.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        salesRows.add(gcashText, gridBagConstraints);

        gcashBar.setForeground(new java.awt.Color(124, 196, 187));
        gcashBar.setBorderPainted(false);
        gcashBar.setValue(37);
        gcashBar.setValue(0);
        gcashBar.setPreferredSize(new java.awt.Dimension(10, 8));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        salesRows.add(gcashBar, gridBagConstraints);

        salesFiller.setOpaque(false);
        salesFiller.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        salesRows.add(salesFiller, gridBagConstraints);
        salesSplitCard.add(salesRows, java.awt.BorderLayout.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 16);
        bodyPanel.add(salesSplitCard, gridBagConstraints);

        wasteCard.setPreferredSize(new java.awt.Dimension(10, 10));
        wasteCard.setLayout(new java.awt.BorderLayout());

        wasteHead.setOpaque(false);
        wasteHead.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 20, 0));
        wasteHead.setLayout(new java.awt.GridBagLayout());

        wasteHeading.setText("Waste Recorded This Week");
        wasteHeading.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        wasteHead.add(wasteHeading, gridBagConstraints);

        wasteSummary.setText("Last 7 days \u00b7 18 items");
        wasteSummary.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        wasteSummary.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        wasteHead.add(wasteSummary, gridBagConstraints);
        wasteCard.add(wasteHead, java.awt.BorderLayout.NORTH);

        wasteRows.setOpaque(false);
        wasteRows.setLayout(new java.awt.GridBagLayout());

        wasteName0.setText("Paper Jam");
        wasteName0.setPreferredSize(new java.awt.Dimension(112, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 0);
        wasteRows.add(wasteName0, gridBagConstraints);

        wasteBar0.setForeground(new java.awt.Color(15, 118, 110));
        wasteBar0.setBorderPainted(false);
        wasteBar0.setValue(100);
        wasteBar0.setValue(0);
        wasteBar0.setPreferredSize(new java.awt.Dimension(10, 8));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 12);
        wasteRows.add(wasteBar0, gridBagConstraints);

        wasteCount0.setText("8");
        wasteCount0.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        wasteCount0.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        wasteCount0.setPreferredSize(new java.awt.Dimension(28, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 0);
        wasteRows.add(wasteCount0, gridBagConstraints);

        wasteName1.setText("Misprint");
        wasteName1.setPreferredSize(new java.awt.Dimension(112, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 0);
        wasteRows.add(wasteName1, gridBagConstraints);

        wasteBar1.setForeground(new java.awt.Color(15, 118, 110));
        wasteBar1.setBorderPainted(false);
        wasteBar1.setValue(75);
        wasteBar1.setValue(0);
        wasteBar1.setPreferredSize(new java.awt.Dimension(10, 8));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 12);
        wasteRows.add(wasteBar1, gridBagConstraints);

        wasteCount1.setText("6");
        wasteCount1.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        wasteCount1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        wasteCount1.setPreferredSize(new java.awt.Dimension(28, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 0);
        wasteRows.add(wasteCount1, gridBagConstraints);

        wasteName2.setText("Damaged");
        wasteName2.setPreferredSize(new java.awt.Dimension(112, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 0);
        wasteRows.add(wasteName2, gridBagConstraints);

        wasteBar2.setForeground(new java.awt.Color(15, 118, 110));
        wasteBar2.setBorderPainted(false);
        wasteBar2.setValue(50);
        wasteBar2.setValue(0);
        wasteBar2.setPreferredSize(new java.awt.Dimension(10, 8));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 12);
        wasteRows.add(wasteBar2, gridBagConstraints);

        wasteCount2.setText("4");
        wasteCount2.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        wasteCount2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        wasteCount2.setPreferredSize(new java.awt.Dimension(28, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 14, 0);
        wasteRows.add(wasteCount2, gridBagConstraints);

        wasteFiller.setOpaque(false);
        wasteFiller.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        wasteRows.add(wasteFiller, gridBagConstraints);
        wasteCard.add(wasteRows, java.awt.BorderLayout.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        bodyPanel.add(wasteCard, gridBagConstraints);
        add(bodyPanel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.printhub.ui.CardPanel alertsCard;
    private javax.swing.JPanel bodyPanel;
    private javax.swing.JProgressBar cashBar;
    private javax.swing.JLabel cashLabel;
    private javax.swing.JLabel cashText;
    private javax.swing.JLabel dateLabel;
    private javax.swing.JProgressBar gcashBar;
    private javax.swing.JLabel gcashLabel;
    private javax.swing.JLabel gcashText;
    private javax.swing.JPanel headerPanel;
    private com.printhub.ui.CardPanel lowCard;
    private javax.swing.JLabel lowHeading;
    private javax.swing.JLabel lowNote;
    private javax.swing.JScrollPane lowScroll;
    private javax.swing.JTable lowTable;
    private com.printhub.ui.IconTile lowTile;
    private javax.swing.JLabel lowTitle;
    private javax.swing.JLabel lowValue;
    private javax.swing.JPanel nextBody;
    private javax.swing.JLabel nextSampleInfo;
    private javax.swing.JLabel nextSampleTitle;
    private com.printhub.ui.CardPanel nextCard;
    private javax.swing.JLabel nextHeading;
    private com.printhub.ui.CardPanel pendingCard;
    private javax.swing.JLabel pendingNote;
    private com.printhub.ui.IconTile pendingTile;
    private javax.swing.JLabel pendingTitle;
    private javax.swing.JLabel pendingValue;
    private com.printhub.ui.CardPanel rushCard;
    private javax.swing.JLabel rushNote;
    private com.printhub.ui.IconTile rushTile;
    private javax.swing.JLabel rushTitle;
    private javax.swing.JLabel rushValue;
    private com.printhub.ui.CardPanel salesCard;
    private javax.swing.JPanel salesFiller;
    private javax.swing.JPanel salesHead;
    private javax.swing.JLabel salesHeading;
    private javax.swing.JLabel salesNote;
    private javax.swing.JPanel salesRows;
    private com.printhub.ui.CardPanel salesSplitCard;
    private com.printhub.ui.IconTile salesTile;
    private javax.swing.JLabel salesTitle;
    private javax.swing.JLabel salesTotalLabel;
    private javax.swing.JLabel salesValue;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JProgressBar wasteBar0;
    private javax.swing.JProgressBar wasteBar1;
    private javax.swing.JProgressBar wasteBar2;
    private com.printhub.ui.CardPanel wasteCard;
    private javax.swing.JLabel wasteCount0;
    private javax.swing.JLabel wasteCount1;
    private javax.swing.JLabel wasteCount2;
    private javax.swing.JPanel wasteFiller;
    private javax.swing.JPanel wasteHead;
    private javax.swing.JLabel wasteHeading;
    private javax.swing.JLabel wasteName0;
    private javax.swing.JLabel wasteName1;
    private javax.swing.JLabel wasteName2;
    private javax.swing.JPanel wasteRows;
    private javax.swing.JLabel wasteSummary;
    // End of variables declaration//GEN-END:variables
}
