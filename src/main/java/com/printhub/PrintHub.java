package com.printhub;

import com.printhub.data.PrintHubData;
import com.printhub.ui.LoginFrame;
import com.printhub.ui.PrintHubTheme;
import com.printhub.ui.SetupFrame;
import java.util.Arrays;
import javax.swing.SwingUtilities;

/**
 * PrintHub: Printing Supplies and Service Management System.
 * Application entry point. The first run on a computer shows the shop setup; start with {@code --demo} to load the
 * sample shop instead (only when there is no shop yet).
 */
public class PrintHub {

    public static void main(String[] args) {
        PrintHubTheme.install();
        boolean demo = Arrays.asList(args).contains("--demo");
        SwingUtilities.invokeLater(() -> {
            if (PrintHubData.needsSetup()) {
                if (demo) {
                    PrintHubData.loadDemo();
                } else {
                    new SetupFrame(() -> new LoginFrame().setVisible(true)).setVisible(true);
                    return;
                }
            }
            new LoginFrame().setVisible(true);
        });
    }
}
