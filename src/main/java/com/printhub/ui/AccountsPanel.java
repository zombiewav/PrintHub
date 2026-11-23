package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.model.Account;
import com.printhub.model.Role;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Accounts (admin only): add staff, change roles, reset passwords, delete accounts. */
public class AccountsPanel extends JPanel implements Refreshable {

    private final PrintHubData data = PrintHubData.get();
    private final DefaultTableModel model = Tables.model("Username", "Full name", "Role");
    private final JTable table = Pages.table(model, new int[] {}, new int[] {}, 200, 280, 120);
    private final JLabel note = Pages.note();
    private List<Account> shown = new ArrayList<>();

    public AccountsPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        add(Pages.header("Accounts", "Admins manage everything. Staff run the counter."), BorderLayout.NORTH);

        JButton add = Ui.primary("Add Account");
        JButton role = Ui.secondary("Change Role");
        JButton reset = Ui.secondary("Reset Password");
        JButton delete = Ui.danger(new JButton("Delete"));
        add.addActionListener(e -> new AccountDialog(owner()).open());
        role.addActionListener(e -> changeRole());
        reset.addActionListener(e -> {
            Account a = selected();
            if (a != null) {
                new ChangePasswordDialog(owner(), a.getUsername(), false).open();
            }
        });
        delete.addActionListener(e -> delete());
        JPanel footer = Pages.footer();
        footer.add(add);
        footer.add(role);
        footer.add(reset);
        footer.add(delete);
        footer.add(note);

        CardPanel card = new CardPanel(new BorderLayout());
        card.add(Pages.tablePage(table, footer), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        refresh();
    }

    private Window owner() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private Account selected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) {
            JOptionPane.showMessageDialog(this, "Select an account in the table first.", "No account selected",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return shown.get(row);
    }

    private void changeRole() {
        Account a = selected();
        if (a == null) {
            return;
        }
        Object choice = JOptionPane.showInputDialog(this, "New role for " + a.getFullName() + ":", "Change Role",
                JOptionPane.QUESTION_MESSAGE, null, Role.values(), a.getRoleType());
        if (choice == null) {
            return;
        }
        try {
            data.accounts().setRole(a.getUsername(), (Role) choice);
            data.changed();
        } catch (IllegalStateException | SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot change role", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void delete() {
        Account a = selected();
        if (a == null) {
            return;
        }
        Account me = data.currentUser();
        if (me != null && me.getUsername().equalsIgnoreCase(a.getUsername())) {
            JOptionPane.showMessageDialog(this, "You cannot delete the account you are signed in with.",
                    "Cannot delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Delete the account of " + a.getFullName() + "?", "Delete account",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            try {
                data.accounts().delete(a.getUsername());
                data.changed();
            } catch (IllegalStateException | SecurityException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot delete", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    @Override
    public void refresh() {
        shown = data.accounts().all();
        model.setRowCount(0);
        for (Account a : shown) {
            model.addRow(new Object[] {a.getUsername(), a.getFullName(), a.getRole()});
        }
        note.setText(shown.size() + " accounts");
    }
}
