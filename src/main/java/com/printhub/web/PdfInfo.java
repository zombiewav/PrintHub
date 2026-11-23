package com.printhub.web;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

/** Reads the page count of an uploaded PDF, so the customer cannot pick a different number than the file has. */
final class PdfInfo {

    private PdfInfo() {
    }

    /** Number of pages. Throws IllegalArgumentException (with a message for the customer) if it is not a usable PDF. */
    static int countPages(byte[] data) {
        if (data == null || data.length < 5 || data[0] != '%' || data[1] != 'P' || data[2] != 'D' || data[3] != 'F'
                || data[4] != '-') {
            throw new IllegalArgumentException("That file is not a valid PDF.");
        }
        try (PDDocument doc = Loader.loadPDF(data)) {
            int pages = doc.getNumberOfPages();
            if (pages < 1) {
                throw new IllegalArgumentException("That PDF has no pages.");
            }
            return pages;
        } catch (InvalidPasswordException e) {
            throw new IllegalArgumentException("That PDF is password protected. Remove the password and try again.");
        } catch (IOException | RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalArgumentException("That file is not a valid PDF.");
        }
    }
}
