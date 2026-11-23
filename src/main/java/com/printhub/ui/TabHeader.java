package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Tab title plus a small count chip, e.g. "Pending 7". */
public final class TabHeader extends JPanel {

    private final JLabel title;
    private final Badge chip;
    private boolean selected;

    public TabHeader(String text, int count) {
        super(new FlowLayout(FlowLayout.LEFT, 8, 0));
        setOpaque(false);
        setBorder(new javax.swing.border.EmptyBorder(0, -8, 0, -8));
        title = Ui.label(text, FONT_BODY_BOLD, TEXT_SECONDARY);
        chip = new Badge(String.valueOf(count), BORDER, TEXT_SECONDARY);
        add(title);
        add(chip);
        setSelected(false);
    }

    public void setCount(int count) {
        chip.setText(String.valueOf(count));
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        title.setForeground(selected ? ACCENT : TEXT_SECONDARY);
        title.setFont(selected ? FONT_BODY_BOLD : FONT_BODY);
        if (selected) {
            chip.setColors(ACCENT_TINT, ACCENT);
        } else {
            chip.setColors(BORDER, TEXT_SECONDARY);
        }
    }

    public boolean isSelected() {
        return selected;
    }
}
