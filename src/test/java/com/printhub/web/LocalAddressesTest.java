package com.printhub.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalAddressesTest {

    @Test
    void listsOnlyRealIpv4AddressesNeverLoopback() {
        for (String ip : LocalAddresses.ipv4()) {
            assertTrue(ip.matches("\\d{1,3}(\\.\\d{1,3}){3}"), ip);
            assertFalse(ip.startsWith("127."), ip);
        }
    }

    @Test
    void theWindowsHotspotAddressIsListedFirst() {
        List<String> sorted = LocalAddresses.hotspotFirst(List.of("192.168.1.20", "192.168.137.1", "10.0.0.5"));
        assertEquals("192.168.137.1", sorted.get(0));
        assertEquals(3, sorted.size());
        assertEquals(List.of("10.0.0.5"), LocalAddresses.hotspotFirst(List.of("10.0.0.5")));
    }

    @Test
    void theAddressUrlHasTheHttpSchemeAndThePort() {
        assertEquals("http://192.168.137.1:8080/", LocalAddresses.url("192.168.137.1", 8080));
    }

    @Test
    void qrCodeIsASquareImageOfDarkAndLightPixels() {
        BufferedImage img = QrCodes.image("http://192.168.137.1:8080/", 220);
        assertEquals(220, img.getWidth());
        assertEquals(220, img.getHeight());
        boolean dark = false;
        boolean light = false;
        for (int y = 0; y < 220; y++) {
            for (int x = 0; x < 220; x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                dark |= rgb == 0x000000;
                light |= rgb == 0xFFFFFF;
            }
        }
        assertTrue(dark && light);
    }
}
