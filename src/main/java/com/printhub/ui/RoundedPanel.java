package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.ARC;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Panel with rounded corners: a fill color and an optional 1 px line.
 * Drop it on a form and set fillColor / lineColor in the Properties window.
 */
public class RoundedPanel extends JPanel {

    private Color fillColor;
    private Color lineColor;
    private int arc = ARC * 2;

    public RoundedPanel() {
        setOpaque(false);
    }

    public RoundedPanel(LayoutManager layout, Color fillColor, Color lineColor) {
        super(layout);
        this.fillColor = fillColor;
        this.lineColor = lineColor;
        setOpaque(false);
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
        repaint();
    }

    public Color getLineColor() {
        return lineColor;
    }

    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
        repaint();
    }

    /** Corner diameter in px (the design uses a 4 px radius, so 8). */
    public int getArc() {
        return arc;
    }

    public void setArc(int arc) {
        this.arc = arc;
        repaint();
    }

    public void setColors(Color fill, Color line) {
        this.fillColor = fill;
        this.lineColor = line;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (fillColor != null) {
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        if (lineColor != null) {
            g2.setColor(lineColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
