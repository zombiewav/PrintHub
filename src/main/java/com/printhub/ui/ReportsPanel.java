package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import com.printhub.data.PrintHubData;
import com.printhub.model.JobOrder;
import com.printhub.model.Restock;
import com.printhub.model.Service;
import com.printhub.model.Supply;
import com.printhub.service.ReportService;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/** Reports (admin only): sales, inventory, restocking, usage and jobs. */
public class ReportsPanel extends JPanel implements Refreshable {

    private static final String[] PERIODS = {"Today", "This week", "This month"};

    private final PrintHubData data = PrintHubData.get();

    // Sales
    private final JComboBox<String> periodBox = Ui.combo(new JComboBox<>(PERIODS));
    private final JLabel cashValue = value();
    private final JLabel gcashValue = value();
    private final JLabel totalValue = value();
    private final JLabel countValue = value();
    private final DefaultTableModel perDayModel = Tables.model("Date", "Collections");
    private final JTable perDayTable = Pages.table(perDayModel, new int[] {1}, new int[] {}, 240, 160);
    private final JLabel salesNote = Pages.note();

    // Inventory
    private final DefaultTableModel stockModel = Tables.model("Item", "Size", "In stock", "Reorder level", "Status",
            "Unit cost", "Value");
    private final JTable stockTable = Pages.table(stockModel, new int[] {2, 3, 5, 6}, new int[] {4}, 280, 80, 120, 120, 90,
            100, 120);
    private final JLabel stockNote = Pages.note();

    // Restocking
    private final DefaultTableModel restockModel = Tables.model("Date", "Supplier", "Item", "Quantity", "Total cost");
    private final JTable restockTable = Pages.table(restockModel, new int[] {3, 4}, new int[] {}, 170, 200, 260, 90, 120);
    private final JLabel restockNote = Pages.note();

    // Usage
    private final DefaultTableModel usedModel = Tables.model("Material", "Units used");
    private final JTable usedTable = Pages.table(usedModel, new int[] {1}, new int[] {}, 320, 100);
    private final DefaultTableModel popularModel = Tables.model("Service", "Jobs");
    private final JTable popularTable = Pages.table(popularModel, new int[] {1}, new int[] {}, 260, 100);

    // Jobs
    private final DefaultTableModel jobsModel = Tables.model("Job ID", "Customer", "Service", "Size", "Pages " + Ui.TIMES
            + " Copies", "Status", "Payment");
    private final JTable jobsTable = Pages.table(jobsModel, new int[] {}, new int[] {5, 6}, 100, 200, 160, 80, 120, 110, 100);
    private final JLabel jobsNote = Pages.note();

    private final TabHeader stockHeader;
    private final TabHeader restockHeader;
    private final TabHeader jobsHeader;

    public ReportsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        add(Pages.header("Reports", "Collections are not profit: ink, paper and other costs still come out of them."),
                BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setOpaque(false);
        Ui.addTab(tabs, "Sales", salesPage(), 0).withoutCount();
        stockHeader = Ui.addTab(tabs, "Inventory", stockPage(), 0);
        restockHeader = Ui.addTab(tabs, "Restocking", restockPage(), 0);
        Ui.addTab(tabs, "Usage", usagePage(), 0).withoutCount();
        jobsHeader = Ui.addTab(tabs, "Jobs", jobsPage(), 0);
        Ui.syncTabs(tabs);
        CardPanel card = new CardPanel(new BorderLayout());
        card.add(tabs, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        periodBox.addActionListener(e -> refreshSales());
        refresh();
    }

    private static JLabel value() {
        return Ui.label(" ", FONT_CARD_VALUE, TEXT);
    }

    private static JPanel statCard(String caption, JLabel value) {
        CardPanel card = new CardPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;
        card.add(Ui.caption(caption), g);
        g.gridy = 1;
        card.add(value, g);
        return card;
    }

    // ---- Tabs -------------------------------------------------------------------------------

    private JPanel salesPage() {
        JPanel top = Ui.panel(new BorderLayout(0, 12));
        top.setBorder(new EmptyBorder(16, 0, 12, 0));
        JPanel pick = Ui.flow(java.awt.FlowLayout.LEFT, 8);
        pick.add(Ui.caption("Period"));
        pick.add(periodBox);
        top.add(pick, BorderLayout.NORTH);
        JPanel cards = Ui.panel(new GridLayout(1, 4, 12, 0));
        cards.add(statCard("Cash payments", cashValue));
        cards.add(statCard("GCash payments", gcashValue));
        cards.add(statCard("Total collections", totalValue));
        cards.add(statCard("Transactions", countValue));
        top.add(cards, BorderLayout.CENTER);

        JPanel footer = Pages.footer();
        footer.add(salesNote);
        JPanel page = Ui.panel(new BorderLayout());
        page.add(top, BorderLayout.NORTH);
        page.add(Pages.tablePage(perDayTable, footer), BorderLayout.CENTER);
        return page;
    }

    private JPanel stockPage() {
        JPanel footer = Pages.footer();
        footer.add(stockNote);
        return Pages.tablePage(stockTable, footer);
    }

    private JPanel restockPage() {
        JPanel footer = Pages.footer();
        footer.add(restockNote);
        return Pages.tablePage(restockTable, footer);
    }

    private JPanel usagePage() {
        JPanel page = Ui.panel(new GridLayout(1, 2, 16, 0));
        page.setBorder(new EmptyBorder(16, 0, 0, 0));
        page.add(titled("Most frequently used materials (completed jobs)", usedTable));
        page.add(titled("Most popular printing services", popularTable));
        return page;
    }

    private static JPanel titled(String title, JTable table) {
        JPanel box = Ui.panel(new BorderLayout(0, 8));
        box.add(Ui.bold(title), BorderLayout.NORTH);
        box.add(Ui.scroll(table), BorderLayout.CENTER);
        return box;
    }

    private JPanel jobsPage() {
        JPanel footer = Pages.footer();
        footer.add(jobsNote);
        return Pages.tablePage(jobsTable, footer);
    }

    // ---- Showing the data -------------------------------------------------------------------

    @Override
    public void refresh() {
        refreshSales();
        refreshStock();
        refreshRestocking();
        refreshUsage();
        refreshJobs();
    }

    private ReportService reports() {
        return data.reports();
    }

    private void refreshSales() {
        LocalDate today = LocalDate.now();
        int pick = periodBox.getSelectedIndex();
        ReportService.SalesReport r = pick == 0 ? reports().salesForDay(today)
                : pick == 1 ? reports().salesForWeek(today) : reports().salesForMonth(YearMonth.from(today));
        cashValue.setText(Ui.money(r.cash()));
        gcashValue.setText(Ui.money(r.gcash()));
        totalValue.setText(Ui.money(r.total()));
        countValue.setText(Ui.count(r.count()));
        perDayModel.setRowCount(0);
        for (Map.Entry<LocalDate, Double> e : r.perDay().entrySet()) {
            perDayModel.addRow(new Object[] {e.getKey().format(Ui.LONG_DATE), Ui.money(e.getValue())});
        }
        salesNote.setText(pick == 0 ? "Daily cash summary for " + today.format(Ui.LONG_DATE)
                : "Collections per day, " + PERIODS[pick].toLowerCase());
    }

    private void refreshStock() {
        List<Supply> items = reports().inventoryAvailability();
        stockModel.setRowCount(0);
        for (Supply s : items) {
            stockModel.addRow(new Object[] {s.getName(), s.getSize(), Ui.count(s.getStock()) + " " + s.getUnit(),
                Ui.count(s.getReorderLevel()), s.status(), Ui.money(s.getUnitCost()),
                Ui.money(s.getStock() * s.getUnitCost())});
        }
        stockHeader.setCount(items.size());
        stockNote.setText("Total inventory valuation: " + Ui.money(reports().inventoryValuation()) + " " + Ui.DOT + " "
                + reports().lowStock().size() + " items low or out of stock");
    }

    private void refreshRestocking() {
        List<Restock> rows = data.supplierHistory(null);
        restockModel.setRowCount(0);
        for (Restock r : rows) {
            restockModel.addRow(new Object[] {r.getTime().format(Ui.FULL_STAMP), r.getSupplier().getName(),
                r.getSupply() == null ? Ui.DASH : r.getSupply().displayName(), Ui.count(r.getQty()),
                Ui.money(r.getTotalCost())});
        }
        restockHeader.setCount(rows.size());
        LocalDate today = LocalDate.now();
        restockNote.setText("This month: " + Ui.money(reports().restockExpenses(today.withDayOfMonth(1), today))
                + " " + Ui.DOT + " All time: " + Ui.money(reports().restockExpenses(LocalDate.of(2000, 1, 1), today)));
    }

    private void refreshUsage() {
        usedModel.setRowCount(0);
        for (Map.Entry<String, Integer> e : reports().mostUsedMaterials().entrySet()) {
            usedModel.addRow(new Object[] {e.getKey(), Ui.count(e.getValue())});
        }
        popularModel.setRowCount(0);
        for (Map.Entry<Service, Integer> e : reports().popularServices().entrySet()) {
            popularModel.addRow(new Object[] {e.getKey().label(), Ui.count(e.getValue())});
        }
    }

    private void refreshJobs() {
        List<JobOrder> jobs = new ArrayList<>(data.completedJobs());
        jobs.addAll(data.cancelledJobs());
        jobs.sort(Comparator.comparingInt(JobOrder::getNumber).reversed());
        jobsModel.setRowCount(0);
        for (JobOrder j : jobs) {
            jobsModel.addRow(new Object[] {j.getId(), j.getCustomer(), j.getService().label(), j.getSize().label(),
                j.pagesCopies(), j.getStatus().label(), j.getPaymentStatus().label()});
        }
        jobsHeader.setCount(jobs.size());
        ReportService.JobCounts counts = reports().jobCounts();
        jobsNote.setText(counts.completed() + " completed " + Ui.DOT + " " + counts.cancelled() + " cancelled");
    }
}
