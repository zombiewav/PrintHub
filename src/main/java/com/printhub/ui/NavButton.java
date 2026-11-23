package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * One sidebar row: gray icon and text, teal on a tinted background when selected.
 * Set the text and iconName (e.g. "dashboard") in the Properties window.
 */
public class NavButton extends JToggleButton {

    private String iconName = "";
    private boolean hover;

    public NavButton() {
        setIconTextGap(12);
        setFont(FONT_BODY);
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(new EmptyBorder(0, 12, 0, 12));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setFocusable(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addItemListener(e -> setFont(isSelected() ? FONT_BODY_BOLD : FONT_BODY));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
        if (iconName == null || iconName.isEmpty()) {
            setIcon(null);
            setSelectedIcon(null);
        } else {
            setIcon(Icons.get(iconName, 20, TEXT_SECONDARY));
            setSelectedIcon(Icons.get(iconName, 20, ACCENT));
        }
    }

    @Override
    public Color getForeground() {
        return isSelected() ? ACCENT : TEXT;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (isSelected()) {
            g2.setColor(ACCENT_TINT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC * 2, ARC * 2);
        } else if (hover) {
            g2.setColor(BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC * 2, ARC * 2);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
