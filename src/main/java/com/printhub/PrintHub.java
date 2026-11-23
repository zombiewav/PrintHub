package com.printhub;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * PrintHub: Printing Supplies and Service Management System.
 * Application entry point.
 */
public class PrintHub {

    public static void main(String[] args) {
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("PrintHub");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new JLabel("PrintHub", SwingConstants.CENTER));
            frame.setSize(1280, 720);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
