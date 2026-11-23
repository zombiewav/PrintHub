package com.printhub.web;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small multipart/form-data reader for the upload form: text fields plus one uploaded file.
 * Throws IllegalArgumentException for anything that is not a complete, well-formed body.
 */
final class Multipart {

    private static final Pattern BOUNDARY = Pattern.compile("boundary=(?:\"([^\"]+)\"|([^;\\s]+))", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME = Pattern.compile("(?:^|[;\\s])name=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILENAME = Pattern.compile("filename=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] HEADER_END = {'\r', '\n', '\r', '\n'};

    private final Map<String, String> fields = new LinkedHashMap<>();
    private String fileName;
    private byte[] fileBytes;

    private Multipart() {
    }

    static Multipart parse(String contentType, byte[] body) {
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            throw new IllegalArgumentException("The form was not sent correctly.");
        }
        Matcher b = BOUNDARY.matcher(contentType);
        if (!b.find()) {
            throw new IllegalArgumentException("The form was not sent correctly.");
        }
        String boundary = b.group(1) != null ? b.group(1) : b.group(2);
        byte[] delimiter = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] nextDelimiter = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);

        Multipart result = new Multipart();
        int pos = 0;
        while (true) {
            if (!startsWith(body, delimiter, pos)) {
                throw new IllegalArgumentException("The form was not sent correctly.");
            }
            pos += delimiter.length;
            if (startsWith(body, new byte[] {'-', '-'}, pos)) {
                return result; // closing boundary
            }
            if (!startsWith(body, CRLF, pos)) {
                throw new IllegalArgumentException("The form was not sent correctly.");
            }
            pos += CRLF.length;
            int headerEnd = indexOf(body, HEADER_END, pos);
            if (headerEnd < 0) {
                throw new IllegalArgumentException("The form was not sent correctly.");
            }
            String headers = new String(body, pos, headerEnd - pos, StandardCharsets.UTF_8);
            int dataStart = headerEnd + HEADER_END.length;
            int dataEnd = indexOf(body, nextDelimiter, dataStart);
            if (dataEnd < 0) {
                throw new IllegalArgumentException("The upload was cut off. Please try again.");
            }
            result.addPart(headers, Arrays.copyOfRange(body, dataStart, dataEnd));
            pos = dataEnd + CRLF.length;
        }
    }

    private void addPart(String headers, byte[] data) {
        Matcher n = NAME.matcher(headers);
        if (!n.find()) {
            return;
        }
        Matcher f = FILENAME.matcher(headers);
        if (f.find()) {
            if (fileBytes == null) {
                String raw = f.group(1);
                fileName = raw.substring(Math.max(raw.lastIndexOf('/'), raw.lastIndexOf('\\')) + 1);
                fileBytes = data;
            }
        } else {
            fields.put(n.group(1), new String(data, StandardCharsets.UTF_8));
        }
    }

    /** The text field with this name, or null. */
    String field(String name) {
        return fields.get(name);
    }

    /** The uploaded file's name without any folder, or null when no file was sent. */
    String fileName() {
        return fileName;
    }

    byte[] fileBytes() {
        return fileBytes;
    }

    private static boolean startsWith(byte[] data, byte[] prefix, int at) {
        if (at < 0 || at + prefix.length > data.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[at + i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(byte[] data, byte[] pattern, int from) {
        outer:
        for (int i = from; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
