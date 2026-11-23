package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.FONT_SMALL_BOLD;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;

/** Pill shaped status badge (20 px high, 8 px side padding, 12 px bold). */
public class Badge extends JLabel {

    public Badge(String text, Color background, Color foreground) {
        super(text);
        setOpaque(false);
        setFont(FONT_SMALL_BOLD);
        setBackground(background);
        setForeground(foreground);
        setBorder(new EmptyBorder(2, 8, 2, 8));
    }

    public void setColors(Color background, Color foreground) {
        setBackground(background);
        setForeground(foreground);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
