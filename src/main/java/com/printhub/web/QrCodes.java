package com.printhub.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;
import java.util.Map;

/** Draws the QR code a customer scans to open the portal. */
public final class QrCodes {

    private QrCodes() {
    }

    /** A square black-on-white QR code, size x size pixels, with a small quiet border. */
    public static BufferedImage image(String text, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    img.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            return img;
        } catch (WriterException e) {
            throw new IllegalArgumentException("Cannot make a QR code for that text.", e);
        }
    }
}
