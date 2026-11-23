package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.web.LocalAddresses;
import com.printhub.web.QrCodes;
import java.awt.Window;
import java.io.IOException;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/**
 * Customer portal control: start or stop it, and show the address and QR code customers use after they join the
 * computer's Wi-Fi hotspot.
 */
public class PortalDialog extends FormDialog {

    private static final int DEFAULT_PORT = 8080;

    private final PrintHubData data = PrintHubData.get();
    private final JLabel statusLabel = Ui.bold(" ");
    private final JSpinner portSpinner = Ui.spinner(new JSpinner(new SpinnerNumberModel(DEFAULT_PORT, 1024, 65535, 1)));
    private final JButton toggleButton = Ui.primary("Start portal");
    private final JLabel addressLabel = Ui.body(" ");
    private final JLabel qrLabel = new JLabel();
    private final JLabel helpLabel = Ui.label(" ", PrintHubTheme.FONT_SMALL, PrintHubTheme.TEXT_SECONDARY);

    public PortalDialog(Window owner) {
        super(owner, "Customer Portal");
        addWide(statusLabel);
        addRow("Port", portSpinner);
        addWide(toggleButton);
        addWide(addressLabel);
        qrLabel.setHorizontalAlignment(SwingConstants.CENTER);
        addWide(qrLabel);
        addWide(helpLabel);
        toggleButton.addActionListener(e -> toggle());
        setCloseOnly("Close");
        refreshState();
    }

    private void toggle() {
        if (data.isPortalRunning()) {
            data.stopPortal();
        } else {
            try {
                data.startPortal((Integer) portSpinner.getValue());
            } catch (IOException ex) {
                warn("Could not start the portal on port " + portSpinner.getValue() + ". Is another program using "
                        + "that port? (" + ex.getMessage() + ")");
            }
        }
        refreshState();
    }

    private void refreshState() {
        boolean running = data.isPortalRunning();
        toggleButton.setText(running ? "Stop portal" : "Start portal");
        portSpinner.setEnabled(!running);
        if (running) {
            portSpinner.setValue(data.portalPort());
            List<String> ips = LocalAddresses.ipv4();
            statusLabel.setText("Running: customers can send PDFs");
            if (ips.isEmpty()) {
                addressLabel.setText("<html>No network address found. Turn on the Mobile Hotspot first.</html>");
                qrLabel.setIcon(null);
            } else {
                StringBuilder sb = new StringBuilder("<html>Customers open:<br>");
                for (String ip : ips) {
                    sb.append("<b>").append(LocalAddresses.url(ip, data.portalPort())).append("</b><br>");
                }
                addressLabel.setText(sb.append("</html>").toString());
                qrLabel.setIcon(new ImageIcon(QrCodes.image(LocalAddresses.url(ips.get(0), data.portalPort()), 200)));
            }
        } else {
            statusLabel.setText("Stopped");
            addressLabel.setText("<html>Press Start. Then customers join this computer's Wi-Fi hotspot and open the "
                    + "address shown here, or scan the QR code.</html>");
            qrLabel.setIcon(null);
        }
        helpLabel.setText("<html>On Windows: Settings &gt; Network &amp; internet &gt; Mobile hotspot, turn it on. "
                + "The address is usually 192.168.137.1. If Windows asks, allow PrintHub through the firewall.<br>"
                + "Orders arrive unpaid. Mark them paid in the Job Queue and they print.</html>");
        pack();
    }
}
