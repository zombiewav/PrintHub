package com.printhub.web;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class MultipartTest {

    private static final String BOUNDARY = "----TestBoundary123";
    private static final String TYPE = "multipart/form-data; boundary=" + BOUNDARY;

    private static void text(ByteArrayOutputStream out, String name, String value) throws Exception {
        out.write(("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value
                + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void file(ByteArrayOutputStream out, String name, String filename, byte[] data) throws Exception {
        out.write(("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + filename + "\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] end(ByteArrayOutputStream out) throws Exception {
        out.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    @Test
    void readsTextFieldsAndTheUploadedFile() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        text(out, "name", "Maria Santos");
        text(out, "copies", "2");
        file(out, "file", "thesis.pdf", new byte[] {1, 2, 3, 4});
        Multipart m = Multipart.parse(TYPE, end(out));
        assertEquals("Maria Santos", m.field("name"));
        assertEquals("2", m.field("copies"));
        assertEquals("thesis.pdf", m.fileName());
        assertArrayEquals(new byte[] {1, 2, 3, 4}, m.fileBytes());
    }

    @Test
    void binaryFileContentMayContainLineBreaksAndBoundaryLikeText() throws Exception {
        byte[] tricky = ("line1\r\nline2\r\n--" + BOUNDARY.substring(0, 6) + "\r\n\0\1\2").getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        file(out, "file", "a.pdf", tricky);
        Multipart m = Multipart.parse(TYPE, end(out));
        assertArrayEquals(tricky, m.fileBytes());
    }

    @Test
    void utf8FieldValuesAreKept() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        text(out, "name", "Niño Dela Peña");
        Multipart m = Multipart.parse(TYPE, end(out));
        assertEquals("Niño Dela Peña", m.field("name"));
    }

    @Test
    void missingFieldsAndMissingFileAreNull() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        text(out, "name", "x");
        Multipart m = Multipart.parse(TYPE, end(out));
        assertNull(m.field("service"));
        assertNull(m.fileName());
        assertNull(m.fileBytes());
    }

    @Test
    void wrongContentTypeOrBrokenBodyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Multipart.parse("text/plain", new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> Multipart.parse("multipart/form-data", new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> Multipart.parse(TYPE, "garbage with no boundary".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void anOnlyPartialBodyWithoutTheClosingBoundaryIsRejected() {
        byte[] cut = ("--" + BOUNDARY + "\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\nabc")
                .getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> Multipart.parse(TYPE, cut));
    }
}
