package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.BORDER;
import static com.printhub.ui.PrintHubTheme.PANEL;
import static com.printhub.ui.PrintHubTheme.PANEL_PADDING;
import static com.printhub.ui.PrintHubTheme.padding;

import java.awt.LayoutManager;

/** The white content card: white fill, 1 px border, 16 px padding. */
public class CardPanel extends RoundedPanel {

    public CardPanel() {
        super(null, PANEL, BORDER);
        setBorder(padding(PANEL_PADDING));
    }

    public CardPanel(LayoutManager layout) {
        this();
        setLayout(layout);
    }
}
