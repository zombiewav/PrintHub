package com.printhub.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PdfInfoTest {

    @Test
    void countsThePagesOfARealPdf() {
        assertEquals(1, PdfInfo.countPages(PdfSupport.pdf(1)));
        assertEquals(7, PdfInfo.countPages(PdfSupport.pdf(7)));
    }

    @Test
    void textThatIsNotAPdfIsRejected() {
        byte[] notPdf = "hello, this is not a pdf".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> PdfInfo.countPages(notPdf));
    }

    @Test
    void emptyFileIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PdfInfo.countPages(new byte[0]));
    }

    @Test
    void aFileThatOnlyStartsLikeAPdfButIsBrokenIsRejected() {
        byte[] broken = "%PDF-1.4\nthis is not really a pdf".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> PdfInfo.countPages(broken));
    }

    @Test
    void passwordProtectedPdfIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> PdfInfo.countPages(PdfSupport.lockedPdf()));
        assertEquals("That PDF is password protected. Remove the password and try again.", e.getMessage());
    }
}
