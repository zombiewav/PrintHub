package com.printhub.ui;

import com.printhub.data.PrintHubData;
import java.awt.Window;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/**
 * Sets a password. You change your own (current password required), or an admin resets someone else's
 * (no current password needed).
 */
public class ChangePasswordDialog extends FormDialog {

    private final PrintHubData data = PrintHubData.get();
    private final String username;
    private final boolean own;
    private final JPasswordField oldField = Ui.field(new JPasswordField(20));
    private final JPasswordField newField = Ui.field(new JPasswordField());
    private final JPasswordField confirmField = Ui.field(new JPasswordField());

    /** @param own true when the signed-in user changes their own password */
    public ChangePasswordDialog(Window owner, String username, boolean own) {
        super(owner, own ? "Change Password" : "Reset Password " + Ui.DOT + " " + username);
        this.username = username;
        this.own = own;
        if (own) {
            addRow("Current password", oldField);
        }
        addRow("New password (at least 6 characters)", newField);
        addRow("Confirm new password", confirmField);
        setActions("Save", this::save);
    }

    private void save() {
        String fresh = new String(newField.getPassword());
        if (!fresh.equals(new String(confirmField.getPassword()))) {
            warn("The two new passwords do not match.");
            return;
        }
        try {
            if (own) {
                data.accounts().changePassword(username, new String(oldField.getPassword()), fresh);
            } else {
                data.accounts().resetPassword(username, fresh);
            }
            JOptionPane.showMessageDialog(this, "Password saved.", getTitle(), JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IllegalArgumentException | SecurityException ex) {
            warn(ex.getMessage());
        }
    }
}
