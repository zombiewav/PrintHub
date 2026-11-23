package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * A small hand-written dialog: a column of "caption + field" rows with Cancel and a primary button underneath.
 * Used by the dialogs that are not built in the NetBeans form editor.
 */
public class FormDialog extends JDialog {

    private final JPanel form = Ui.panel(new GridBagLayout());
    private final JPanel footer = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    private int row;

    protected FormDialog(Window owner, String title) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PANEL);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.add(form, BorderLayout.CENTER);
        footer.setBorder(new EmptyBorder(16, 0, 0, 0));
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
        setResizable(false);
    }

    /** Adds "caption" above a field that spans the full width. Returns the caption so it can be hidden with the field. */
    protected JLabel addRow(String caption, JComponent field) {
        JLabel label = Ui.caption(caption);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(row == 1 ? 0 : 12, 0, 4, 0);
        form.add(label, c);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(field, c);
        return label;
    }

    /** Adds a full-width component (a note or a summary) with no caption. */
    protected void addWide(Component component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(row == 1 ? 0 : 12, 0, 0, 0);
        form.add(component, c);
    }

    /** Cancel and the primary button. The action runs when the primary button is pressed. */
    protected void setActions(String okText, Runnable ok) {
        footer.removeAll();
        JButton cancel = Ui.secondary("Cancel");
        cancel.addActionListener(e -> dispose());
        JButton primary = Ui.primary(okText);
        primary.addActionListener(e -> ok.run());
        footer.add(cancel);
        footer.add(primary);
        getRootPane().setDefaultButton(primary);
    }

    /** Only a close button, for read-only dialogs. */
    protected void setCloseOnly(String text) {
        footer.removeAll();
        JButton close = Ui.primary(text);
        close.addActionListener(e -> dispose());
        footer.add(close);
        getRootPane().setDefaultButton(close);
    }

    protected void warn(String message) {
        JOptionPane.showMessageDialog(this, message, getTitle(), JOptionPane.WARNING_MESSAGE);
    }

    /** Sizes the dialog to its content, centres it over the owner and shows it. */
    public void open() {
        pack();
        setMinimumSize(new java.awt.Dimension(380, 0));
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }
}
