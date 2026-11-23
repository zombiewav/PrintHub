package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.model.JobOrder;
import com.printhub.service.CustomerService.CustomerSummary;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/** Customers: who has ordered, how much they paid, and their past jobs. Built from the jobs and payments on record. */
public class CustomersPanel extends JPanel implements Refreshable {

    private final PrintHubData data = PrintHubData.get();
    private final JTextField searchField = Ui.field(new JTextField());
    private final DefaultTableModel customerModel = Tables.model("Customer", "Jobs", "Total paid", "Last visit");
    private final JTable customerTable = Pages.table(customerModel, new int[] {1, 2}, new int[] {}, 280, 80, 140, 200);
    private final DefaultTableModel jobModel = Tables.model("Job ID", "Date / Time", "Service", "Size",
            "Pages " + Ui.TIMES + " Copies", "Status", "Payment", "Total");
    private final JTable jobTable = Pages.table(jobModel, new int[] {7}, new int[] {5, 6}, 90, 170, 200, 80, 120, 100,
            90, 100);
    private final JLabel jobsTitle = Ui.bold("Jobs");
    private final JLabel note = Pages.note();
    private List<CustomerSummary> shown = new ArrayList<>();

    public CustomersPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        add(Pages.header("Customers", "Everyone who has ordered, built from the jobs and payments on record."),
                BorderLayout.NORTH);

        searchField.putClientProperty("JTextField.placeholderText", "Search by customer name");
        searchField.putClientProperty("JTextField.leadingIcon", Icons.get("search", 16, PrintHubTheme.TEXT_DISABLED));
        searchField.setPreferredSize(new java.awt.Dimension(280, 32));
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
        customerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showJobs();
            }
        });

        JPanel top = Ui.panel(new BorderLayout());
        top.setBorder(new javax.swing.border.EmptyBorder(0, 0, 12, 0));
        JPanel searchRow = Ui.flow(java.awt.FlowLayout.LEFT, 8);
        searchRow.add(searchField);
        top.add(searchRow, BorderLayout.WEST);
        top.add(note, BorderLayout.EAST);

        JPanel jobsBox = Ui.panel(new BorderLayout(0, 8));
        jobsBox.add(jobsTitle, BorderLayout.NORTH);
        jobsBox.add(Ui.scroll(jobTable), BorderLayout.CENTER);
        JPanel body = Ui.panel(new GridLayout(2, 1, 0, 16));
        body.add(Ui.scroll(customerTable));
        body.add(jobsBox);

        CardPanel card = new CardPanel(new BorderLayout());
        card.add(top, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        String keep = selectedName();
        shown = data.customerSummaries(searchField.getText());
        customerModel.setRowCount(0);
        int select = -1;
        for (int i = 0; i < shown.size(); i++) {
            CustomerSummary c = shown.get(i);
            customerModel.addRow(new Object[] {c.name(), Ui.count(c.jobs()), Ui.money(c.spent()),
                c.lastVisit().format(Ui.FULL_STAMP)});
            if (c.name().equals(keep)) {
                select = i;
            }
        }
        if (select >= 0) {
            customerTable.setRowSelectionInterval(select, select);
        }
        note.setText(shown.size() + " customers");
        showJobs();
    }

    private String selectedName() {
        int row = customerTable.getSelectedRow();
        return row >= 0 && row < shown.size() ? shown.get(row).name() : null;
    }

    private void showJobs() {
        String name = selectedName();
        jobModel.setRowCount(0);
        if (name == null) {
            jobsTitle.setText("Select a customer to see their jobs");
            return;
        }
        jobsTitle.setText("Jobs of " + name);
        for (JobOrder j : data.customerJobs(name)) {
            jobModel.addRow(new Object[] {j.getId(), j.getCreated().format(Ui.FULL_STAMP),
                j.getDetail() == null ? j.getService().label() : j.getService().label() + " " + Ui.DOT + " " + j.getDetail(),
                j.getSize().label(), j.pagesCopies(), j.getStatus().label(), j.getPaymentStatus().label(),
                Ui.money(j.getTotal())});
        }
    }
}
