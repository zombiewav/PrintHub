package com.printhub.web;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The addresses customers can use to reach this computer, for the portal screen and its QR code. */
public final class LocalAddresses {

    /** The address Windows gives itself when it shares Wi-Fi as a Mobile Hotspot. */
    static final String WINDOWS_HOTSPOT = "192.168.137.1";

    private LocalAddresses() {
    }

    /** IPv4 addresses of every network that is up (not loopback), hotspot address first. */
    public static List<String> ipv4() {
        List<String> found = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback()) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()
                            && !address.isLinkLocalAddress()) {
                        found.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
            // no network information; the screen then says no address was found
        }
        return hotspotFirst(found);
    }

    /** Moves the Windows hotspot address to the front, keeping the rest in order. */
    static List<String> hotspotFirst(List<String> addresses) {
        List<String> sorted = new ArrayList<>(addresses);
        if (sorted.remove(WINDOWS_HOTSPOT)) {
            sorted.add(0, WINDOWS_HOTSPOT);
        }
        return sorted;
    }

    public static String url(String ip, int port) {
        return "http://" + ip + ":" + port + "/";
    }
}
