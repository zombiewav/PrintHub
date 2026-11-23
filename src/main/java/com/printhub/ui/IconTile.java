package com.printhub.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Rounded square with a centered icon (logo, dashboard card icons).
 * Properties: tileColor, iconName (e.g. "printer"), iconColor, iconSize, arc.
 */
public class IconTile extends JLabel {

    private Color tileColor = Color.WHITE;
    private String iconName = "";
    private Color iconColor = Color.BLACK;
    private int iconSize = 20;
    private int arc = 8;

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
        if (iconName == null || iconName.isEmpty()) {
            setIcon(null);
        } else {
            setIcon(Icons.get(iconName, iconSize, iconColor));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(tileColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}
