package com.printhub.ui;

import static com.printhub.ui.PrintHubTheme.*;

import com.printhub.model.Receipt;
import java.awt.Font;
import java.awt.Window;
import javax.swing.JTextArea;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/** Shows a transaction receipt as plain text. */
public class ReceiptDialog extends FormDialog {

    public ReceiptDialog(Window owner, Receipt receipt) {
        super(owner, "Receipt");
        JTextArea text = new JTextArea(receipt.text());
        text.setEditable(false);
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        text.setForeground(TEXT);
        text.setBackground(BACKGROUND);
        text.setBorder(new CompoundBorder(new LineBorder(BORDER), new EmptyBorder(12, 16, 12, 16)));
        text.setRows(text.getLineCount());
        addWide(text);
        setCloseOnly("Close");
    }
}
