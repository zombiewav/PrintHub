package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.data.ShopSetup;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * Shown once, the first time PrintHub opens on a computer: the owner names the shop and creates the admin account.
 * Starter prices and supplies are filled in to edit later. "Load demo data" fills in the sample shop instead.
 */
public class SetupFrame extends JFrame {

    private static final Color TEAL = new Color(15, 118, 110);
    private static final Color GRAY_TEXT = new Color(75, 85, 99);

    private final JTextField shopField = Ui.field(new JTextField());
    private final JTextField ownerField = Ui.field(new JTextField());
    private final JTextField usernameField = Ui.field(new JTextField("admin"));
    private final JPasswordField passwordField = Ui.field(new JPasswordField());
    private final JPasswordField confirmField = Ui.field(new JPasswordField());
    private final JLabel errorLabel = caption(" ");
    private final Runnable onDone;

    /** @param onDone runs after the shop is set up (or the demo is loaded), to show the sign-in screen */
    public SetupFrame(Runnable onDone) {
        this.onDone = onDone;
        setTitle("PrintHub - Set up your shop");
        setIconImage(Ui.appIcon());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(brandPanel(), BorderLayout.WEST);
        getContentPane().add(formPanel(), BorderLayout.CENTER);
        getContentPane().setPreferredSize(new Dimension(1280, 720));
        setMinimumSize(new Dimension(900, 640));
        pack();
        setLocationRelativeTo(null);

        shopField.addActionListener(e -> ownerField.requestFocusInWindow());
        ownerField.addActionListener(e -> usernameField.requestFocusInWindow());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> confirmField.requestFocusInWindow());
        confirmField.addActionListener(e -> create());
    }

    private JPanel brandPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(TEAL);
        p.setPreferredSize(new Dimension(520, 720));
        IconTile logo = new IconTile();
        logo.setIconName("printer");
        logo.setIconColor(TEAL);
        logo.setTileColor(Color.WHITE);
        logo.setIconSize(32);
        logo.setArc(12);
        logo.setPreferredSize(new Dimension(64, 64));
        addLeft(p, logo, 0, 24);
        addLeft(p, label("Welcome to PrintHub", Font.BOLD, 32, Color.WHITE), 1, 8);
        addLeft(p, label("<html>Let's set up your shop. This takes a minute<br>and only happens once.</html>",
                Font.PLAIN, 16, Color.WHITE), 2, 24);
        addLeft(p, label("<html>Prices, the rush fee and the usual supplies are filled in for you.<br>"
                + "Change them any time in Price Settings and Inventory.<br><br>"
                + "<b>Before your first order:</b> restock each supply in Inventory with what is on<br>"
                + "your shelf. Supplies start at 0, and orders need stock.</html>", Font.PLAIN, 12,
                new Color(204, 231, 227)), 3, 0);
        return p;
    }

    private JPanel formPanel() {
        CardPanel card = new CardPanel();
        card.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        card.setLayout(new GridBagLayout());
        int row = 0;
        row = add(card, label("Set up your shop", Font.BOLD, 20, null), row, 4);
        row = add(card, caption("You will sign in with this admin account."), row, 20);
        row = field(card, "Shop name", shopField, row);
        row = field(card, "Owner's full name", ownerField, row);
        row = field(card, "Username", usernameField, row);
        row = field(card, "Password (at least 6 characters)", passwordField, row);
        row = field(card, "Type the password again", confirmField, row);
        errorLabel.setForeground(new Color(185, 28, 28));
        row = add(card, errorLabel, row, 8);
        JButton create = Ui.primary("Create shop");
        create.setPreferredSize(new Dimension(10, 38));
        create.addActionListener(e -> create());
        row = add(card, create, row, 12);
        JButton demo = Ui.secondary(new JButton("Load demo data instead"));
        demo.addActionListener(e -> loadDemo());
        row = add(card, demo, row, 0);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        card.add(spacer, filler);
        card.setPreferredSize(new Dimension(400, card.getPreferredSize().height)); // fixed width, tall enough for all

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(243, 244, 246));
        p.add(card);
        return p;
    }

    private void create() {
        char[] password = passwordField.getPassword();
        char[] confirm = confirmField.getPassword();
        try {
            if (!Arrays.equals(password, confirm)) {
                throw new IllegalArgumentException("The two passwords are not the same.");
            }
            PrintHubData.setUpShop(new ShopSetup(shopField.getText(), ownerField.getText(), usernameField.getText(),
                    new String(password)));
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
            return;
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirm, '\0');
        }
        dispose();
        onDone.run();
    }

    private void loadDemo() {
        int answer = JOptionPane.showConfirmDialog(this,
                "Fill PrintHub with a sample shop (made-up jobs, sales and accounts) for a demo?\n\n"
                        + "Sign in with admin / admin123 or lmendoza / printhub.\n"
                        + "Do not use this for a real shop: the passwords are public.",
                "Load demo data", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        PrintHubData.loadDemo();
        dispose();
        onDone.run();
    }

    // ---- layout helpers ----------------------------------------------------------------------

    private static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        if (color != null) {
            l.setForeground(color);
        }
        return l;
    }

    private static JLabel caption(String text) {
        return label(text, Font.PLAIN, 12, GRAY_TEXT);
    }

    private static void addLeft(JPanel p, JComponent c, int row, int below) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row;
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1;
        g.insets = new Insets(0, 64, below, 0);
        p.add(c, g);
    }

    private static int add(JPanel p, JComponent c, int row, int below) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.weightx = 1;
        g.insets = new Insets(0, 0, below, 0);
        p.add(c, g);
        return row + 1;
    }

    private static int field(JPanel p, String caption, JComponent field, int row) {
        row = add(p, caption(caption), row, 4);
        return add(p, field, row, 12);
    }
}
