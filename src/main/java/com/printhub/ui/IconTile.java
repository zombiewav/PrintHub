package com.printhub.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Rounded square with a centered icon (dashboard card icons), or the PrintHub logo.
 * Properties: tileColor, iconName (e.g. "printer"), iconColor, iconSize, arc, logo.
 */
public class IconTile extends JLabel {

    private Color tileColor = Color.WHITE;
    private String iconName = "";
    private Color iconColor = Color.BLACK;
    private int iconSize = 20;
    private int arc = 8;
    private boolean logo;

    public IconTile() {
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setOpaque(false);
    }

    public Color getTileColor() {
        return tileColor;
    }

    public void setTileColor(Color tileColor) {
        this.tileColor = tileColor;
        repaint();
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
        refreshIcon();
    }

    public Color getIconColor() {
        return iconColor;
    }

    public void setIconColor(Color iconColor) {
        this.iconColor = iconColor;
        refreshIcon();
    }

    public int getIconSize() {
        return iconSize;
    }

    public void setIconSize(int iconSize) {
        this.iconSize = iconSize;
        refreshIcon();
    }

    public int getArc() {
        return arc;
    }

    public void setArc(int arc) {
        this.arc = arc;
        repaint();
    }

    private void refreshIcon() {
        if (logo || iconName == null || iconName.isEmpty()) {
            setIcon(null);
        } else {
            setIcon(Icons.get(iconName, iconSize, iconColor));
        }
    }

    public boolean isLogo() {
        return logo;
    }

    /** True: draw the PrintHub logo over the whole tile instead of a colored tile with an icon. */
    public void setLogo(boolean logo) {
        this.logo = logo;
        refreshIcon();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (logo) {
            // pick the logo file for the real pixel size (display scaling), so it stays sharp
            int side = Math.min(getWidth(), getHeight());
            double scale = g2.getTransform().getScaleX();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.drawImage(Ui.logo((int) Math.ceil(side * scale)), (getWidth() - side) / 2, (getHeight() - side) / 2,
                    side, side, null);
            g2.dispose();
            return;
        }
        g2.setColor(tileColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
