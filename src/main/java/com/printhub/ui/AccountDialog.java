package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.model.Role;
import java.awt.Window;
import javax.swing.JComboBox;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** Admin adds a staff or admin account. */
public class AccountDialog extends FormDialog {

    private final PrintHubData data = PrintHubData.get();
    private final JTextField usernameField = Ui.field(new JTextField(20));
    private final JTextField nameField = Ui.field(new JTextField());
    private final JComboBox<Role> roleBox = Ui.combo(new JComboBox<>(Role.values()));
    private final JPasswordField passwordField = Ui.field(new JPasswordField());

    public AccountDialog(Window owner) {
        super(owner, "Add Account");
        addRow("Username", usernameField);
        addRow("Full name", nameField);
        addRow("Role", roleBox);
        roleBox.setSelectedItem(Role.STAFF);
        addRow("Password (at least 6 characters)", passwordField);
        setActions("Add Account", this::save);
    }

    private void save() {
        try {
            data.accounts().add(usernameField.getText(), new String(passwordField.getPassword()), nameField.getText(),
                    (Role) roleBox.getSelectedItem());
            data.changed();
            dispose();
        } catch (IllegalArgumentException | SecurityException ex) {
            warn(ex.getMessage());
        }
    }
}
