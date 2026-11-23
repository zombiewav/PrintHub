package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.model.JobOrder;
import com.printhub.model.PaymentStatus;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Job Queue: Pending, Processing and Completed jobs. Rush jobs go first, then first come, first served. */
public class JobQueuePanel extends javax.swing.JPanel implements Refreshable {

    private final PrintHubData data = PrintHubData.get();

    private final JobTab pendingJobs = new JobTab(true);
    private final JobTab processingJobs = new JobTab(false);
    private final JobTab completedJobs = new JobTab(false);
    private final JobTab cancelledJobs = new JobTab(false);
    private final TabHeader pendingTab;
    private final TabHeader processingTab;
    private final TabHeader completedTab;
    private final TabHeader cancelledTab;
    private final JButton markPaidButton = Ui.primary("Mark Paid");
    private final JButton cancelJobButton = Ui.danger(new JButton("Cancel Job"));

    private List<JobOrder> pendingRows = new ArrayList<>();
    private List<JobOrder> processingRows = new ArrayList<>();
    private List<JobOrder> completedRows = new ArrayList<>();
    private List<JobOrder> cancelledRows = new ArrayList<>();

    public JobQueuePanel() {
        initComponents();
        pendingTab = Ui.addTab(tabs, "Pending", pendingJobs, 0);
        processingTab = Ui.addTab(tabs, "Processing", processingJobs, 0);
        completedTab = Ui.addTab(tabs, "Completed", completedJobs, 0);
        cancelledTab = Ui.addTab(tabs, "Cancelled", cancelledJobs, 0);
        Ui.syncTabs(tabs);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 4;
        right.gridy = 0;
        right.insets = new Insets(16, 16, 0, 8);
        footer.add(markPaidButton, right);
        right = new GridBagConstraints();
        right.gridx = 5;
        right.gridy = 0;
        right.insets = new Insets(16, 0, 0, 0);
        footer.add(cancelJobButton, right);
        markPaidButton.addActionListener(e -> markPaid());
        cancelJobButton.addActionListener(e -> cancelJob());
        pendingJobs.getTable().getSelectionModel().addListSelectionListener(e -> updateButtons());

        startButton.addActionListener(e -> {
            JobOrder job = data.startNextJob();
            if (job != null) {
                tabs.setSelectedIndex(1);
                int row = processingRows.indexOf(job);
                if (row >= 0) {
                    processingJobs.getTable().setRowSelectionInterval(row, row);
                }
            }
        });
        markDoneButton.addActionListener(e -> {
            int row = processingJobs.getTable().getSelectedRow();
            if (row >= 0 && row < processingRows.size()) {
                try {
                    data.completeJob(processingRows.get(row));
                } catch (IllegalStateException ex) {
                    warn(ex.getMessage());
                }
            }
        });
        reportWasteButton.addActionListener(e -> {
            Window owner = SwingUtilities.getWindowAncestor(this);
            new ReportWasteDialog(owner, selectedJob()).setVisible(true);
        });
        tabs.addChangeListener(e -> updateButtons());
        processingJobs.getTable().getSelectionModel().addListSelectionListener(e -> updateButtons());
        refresh();
    }

    private JobOrder selectedJob() {
        int tab = tabs.getSelectedIndex();
        JobTab content = tab == 0 ? pendingJobs : tab == 1 ? processingJobs : tab == 2 ? completedJobs : cancelledJobs;
        List<JobOrder> rows = tab == 0 ? pendingRows : tab == 1 ? processingRows : tab == 2 ? completedRows : cancelledRows;
        int row = content.getTable().getSelectedRow();
        if (row >= 0 && row < rows.size()) {
            return rows.get(row);
        }
        return (tab == 1 && !rows.isEmpty()) ? rows.get(0) : null;
    }

    /** The job highlighted in the Pending or Processing tab, or null (no guessing, unlike selectedJob). */
    private JobOrder highlightedActiveJob() {
        int tab = tabs.getSelectedIndex();
        if (tab > 1) {
            return null;
        }
        JobTab content = tab == 0 ? pendingJobs : processingJobs;
        List<JobOrder> rows = tab == 0 ? pendingRows : processingRows;
        int row = content.getTable().getSelectedRow();
        return row >= 0 && row < rows.size() ? rows.get(row) : null;
    }

    private void updateButtons() {
        startButton.setEnabled(data.peekNext() != null);
        markDoneButton.setEnabled(tabs.getSelectedIndex() == 1 && processingJobs.getTable().getSelectedRow() >= 0);
        JobOrder job = highlightedActiveJob();
        markPaidButton.setEnabled(job != null && tabs.getSelectedIndex() == 0
                && job.getPaymentStatus() == PaymentStatus.UNPAID);
        cancelJobButton.setEnabled(job != null);
    }

    private void markPaid() {
        JobOrder job = highlightedActiveJob();
        if (job != null) {
            new MarkPaidDialog(SwingUtilities.getWindowAncestor(this), job).showAndGet();
        }
    }

    private void cancelJob() {
        JobOrder job = highlightedActiveJob();
        if (job == null) {
            return;
        }
        String paidNote = job.getPaymentStatus() == PaymentStatus.PAID
                ? "\nIts payment will be marked void." : "";
        int answer = JOptionPane.showConfirmDialog(this, "Cancel " + job.getId() + " for " + job.getCustomer() + "?"
                + paidNote, "Cancel Job", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                data.cancelJob(job);
            } catch (IllegalStateException ex) {
                warn(ex.getMessage());
            }
        }
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(this, message, "Job Queue", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void refresh() {
        pendingRows = data.pendingJobs();
        processingRows = data.processingJobs();
        completedRows = data.completedJobs();
        cancelledRows = data.cancelledJobs();

        pendingJobs.load(pendingRows, true);
        processingJobs.load(processingRows, true);
        completedJobs.load(completedRows, false);
        cancelledJobs.load(cancelledRows, false);
        pendingJobs.showNext(data.peekNext());

        pendingTab.setCount(pendingRows.size());
        processingTab.setCount(processingRows.size());
        completedTab.setCount(completedRows.size());
        cancelledTab.setCount(cancelledRows.size());

        int rush = 0;
        for (JobOrder j : pendingRows) {
            if (j.isRush()) {
                rush++;
            }
        }
        summaryLabel.setText(pendingRows.size() + " pending jobs " + Ui.DOT + " " + rush + " rush");
        updateButtons();
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
        card = new com.printhub.ui.CardPanel();
        tabs = new javax.swing.JTabbedPane();
        footer = new javax.swing.JPanel();
        startButton = new javax.swing.JButton();
        markDoneButton = new javax.swing.JButton();
        reportWasteButton = new javax.swing.JButton();
        summaryLabel = new javax.swing.JLabel();

        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        headerPanel.setOpaque(false);
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 16, 0));
        headerPanel.setPreferredSize(new java.awt.Dimension(10, 48));
        headerPanel.setLayout(new java.awt.GridBagLayout());

        titleLabel.setText("Job Queue");
        titleLabel.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        headerPanel.add(titleLabel, gridBagConstraints);

        subtitleLabel.setText("Rush jobs go first, then first come, first served.");
        subtitleLabel.setForeground(new java.awt.Color(75, 85, 99));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        headerPanel.add(subtitleLabel, gridBagConstraints);
        add(headerPanel, java.awt.BorderLayout.NORTH);

        card.setLayout(new java.awt.BorderLayout());

        tabs.setOpaque(false);
        card.add(tabs, java.awt.BorderLayout.CENTER);

        footer.setOpaque(false);
        footer.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new java.awt.Color(229, 231, 235)));
        footer.setLayout(new java.awt.GridBagLayout());

        startButton.setText("Start Next Job");
        com.printhub.ui.Ui.primary(startButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(16, 0, 0, 8);
        footer.add(startButton, gridBagConstraints);

        markDoneButton.setText("Mark Completed");
        com.printhub.ui.Ui.primary(markDoneButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(16, 0, 0, 8);
        footer.add(markDoneButton, gridBagConstraints);

        reportWasteButton.setText("Report Waste / Paper Jam");
        com.printhub.ui.Ui.warning(reportWasteButton);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(16, 0, 0, 0);
        footer.add(reportWasteButton, gridBagConstraints);

        summaryLabel.setText(" ");
        summaryLabel.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        summaryLabel.setForeground(new java.awt.Color(75, 85, 99));
        summaryLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(16, 0, 0, 0);
        footer.add(summaryLabel, gridBagConstraints);
        card.add(footer, java.awt.BorderLayout.SOUTH);
        add(card, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.printhub.ui.CardPanel card;
    private javax.swing.JPanel footer;
    private javax.swing.JPanel headerPanel;
    private javax.swing.JButton markDoneButton;
    private javax.swing.JButton reportWasteButton;
    private javax.swing.JButton startButton;
    private javax.swing.JLabel subtitleLabel;
    private javax.swing.JLabel summaryLabel;
    private javax.swing.JTabbedPane tabs;
    private javax.swing.JLabel titleLabel;
    // End of variables declaration//GEN-END:variables
}
