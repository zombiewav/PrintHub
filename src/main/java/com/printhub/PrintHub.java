package com.printhub;

import com.printhub.ui.LoginFrame;
import com.printhub.ui.PrintHubTheme;
import javax.swing.SwingUtilities;

/**
 * PrintHub: Printing Supplies and Service Management System.
 * Application entry point.
 */
public class PrintHub {

    public static void main(String[] args) {
        PrintHubTheme.install();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
