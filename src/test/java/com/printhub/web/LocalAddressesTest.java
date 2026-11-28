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

    private static LocalAddresses.Nic nic(String name, String display, String ip) {
        return new LocalAddresses.Nic(name, display, ip);
    }

    @Test
    void theWindowsHotspotAddressIsListedFirst() {
        List<String> sorted = LocalAddresses.reachable(List.of(
                nic("wlan0", "Intel(R) Wi-Fi 6 AX201 160MHz", "192.168.1.20"),
                nic("wlan3", "Microsoft Wi-Fi Direct Virtual Adapter #2", "192.168.137.1"),
                nic("eth0", "Realtek PCIe GbE Family Controller", "10.0.0.5")));
        assertEquals(List.of("192.168.137.1", "192.168.1.20", "10.0.0.5"), sorted);
    }

    @Test
    void hyperVAndOtherInternalAdaptersAreLeftOut() {
        // What this machine reported: Windows lists the Hyper-V switch, which phones cannot reach, after Wi-Fi
        // or even before it.
        List<String> sorted = LocalAddresses.reachable(List.of(
                nic("eth12", "Hyper-V Virtual Ethernet Adapter", "172.26.48.1"),
                nic("wlan0", "Intel(R) Wi-Fi 6 AX201 160MHz", "192.168.1.142"),
                nic("eth3", "vEthernet (WSL)", "172.19.0.1"),
                nic("eth4", "VirtualBox Host-Only Ethernet Adapter", "192.168.56.1"),
                nic("eth5", "VMware Virtual Ethernet Adapter for VMnet8", "192.168.80.1"),
                nic("eth6", "Bluetooth Device (Personal Area Network)", "192.168.44.1")));
        assertEquals(List.of("192.168.1.142"), sorted);
    }

    @Test
    void wiFiComesBeforeWiredWhenThereIsNoHotspot() {
        List<String> sorted = LocalAddresses.reachable(List.of(
                nic("eth0", "Realtek PCIe GbE Family Controller", "10.0.0.5"),
                nic("wlan0", "Intel(R) Wi-Fi 6 AX201 160MHz", "192.168.1.142")));
        assertEquals(List.of("192.168.1.142", "10.0.0.5"), sorted);
    }

    @Test
    void onlyInternalAdaptersMeansNoAddress() {
        assertTrue(LocalAddresses.reachable(List.of(nic("eth12", "Hyper-V Virtual Ethernet Adapter", "172.26.48.1")))
                .isEmpty());
    }

    @Test
    void theAddressUrlHasTheHttpSchemeAndThePort() {
        assertEquals("http://192.168.137.1:8080/", LocalAddresses.url("192.168.137.1", 8080));
        assertEquals("http://192.168.137.1/", LocalAddresses.url("192.168.137.1", 80));
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
