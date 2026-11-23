package com.printhub.ui;

import com.printhub.data.PrintHubData;
import com.printhub.service.SettingsService.ShopSettings;
import java.awt.Window;
import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** Admin: how the queue runs and how stock is estimated. Changes apply straight away. */
public class ShopSettingsDialog extends FormDialog {

    private final PrintHubData data = PrintHubData.get();
    private final JCheckBox autoBox = new JCheckBox("Start the next paid job automatically");
    private final JSpinner slotsSpinner;
    private final JSpinner timeoutSpinner;
    private final JSpinner inkSpinner;

    public ShopSettingsDialog(Window owner) {
        super(owner, "Shop Settings");
        ShopSettings current = data.shopSettings();
        slotsSpinner = Ui.spinner(new JSpinner(new SpinnerNumberModel(current.printerSlots(), 1, 10, 1)));
        timeoutSpinner = Ui.spinner(new JSpinner(new SpinnerNumberModel(current.unpaidTimeoutMinutes(), 5, 1440, 5)));
        inkSpinner = Ui.spinner(new JSpinner(new SpinnerNumberModel(current.inkPerPage(), 1, 10, 1)));
        autoBox.setSelected(current.autoDispatch());
        autoBox.setOpaque(false);
        autoBox.setFont(PrintHubTheme.FONT_BODY_BOLD);
        autoBox.setFocusable(false);

        addWide(autoBox);
        addRow("Jobs that can print at the same time", slotsSpinner);
        addRow("Cancel an unpaid online order after (minutes)", timeoutSpinner);
        addRow("Ink used per page (units, for the stock estimate)", inkSpinner);
        setActions("Save", this::save);
    }

    private void save() {
        try {
            data.saveShopSettings(new ShopSettings(autoBox.isSelected(), (Integer) slotsSpinner.getValue(),
                    (Integer) timeoutSpinner.getValue(), (Integer) inkSpinner.getValue()));
            dispose();
        } catch (IllegalArgumentException | SecurityException ex) {
            warn(ex.getMessage());
        }
    }
}
