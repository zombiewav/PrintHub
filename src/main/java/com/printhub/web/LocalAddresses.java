package com.printhub.web;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** The addresses customers can use to reach this computer, for the portal screen and its QR code. */
public final class LocalAddresses {

    /** The address Windows gives itself when it shares Wi-Fi as a Mobile Hotspot. */
    static final String WINDOWS_HOTSPOT = "192.168.137.1";

    /**
     * Adapters that only exist inside this computer (Hyper-V, WSL, Docker, VirtualBox, VMware, VPN taps).
     * A phone can never reach them. Plain "Virtual" is not on the list: the Windows hotspot itself runs on
     * "Microsoft Wi-Fi Direct Virtual Adapter".
     */
    private static final List<String> INTERNAL_ADAPTERS = List.of("hyper-v", "vethernet", "wsl", "docker",
            "virtualbox", "vmware", "vmnet", "tap-windows", "wintun", "npcap", "loopback", "bluetooth");

    /** One IPv4 address of one network adapter. */
    record Nic(String name, String displayName, String ip) {
    }

    private LocalAddresses() {
    }

    /**
     * IPv4 addresses a phone can reach: the hotspot first, then Wi-Fi, then wired. Adapters that only exist inside
     * this computer (Hyper-V, WSL and the like) are left out.
     */
    public static List<String> ipv4() {
        List<Nic> found = new ArrayList<>();
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(nic.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()
                            && !address.isLinkLocalAddress()) {
                        found.add(new Nic(nic.getName(), nic.getDisplayName(), address.getHostAddress()));
                    }
                }
            }
        } catch (SocketException ignored) {
            // no network information; the screen then says no address was found
        }
        return reachable(found);
    }

    /** Drops internal adapters and puts the rest in the order customers should try them. */
    static List<String> reachable(List<Nic> nics) {
        return nics.stream()
                .filter(n -> !isInternal(n))
                .sorted(Comparator.comparingInt(LocalAddresses::rank)) // stable: same rank keeps Windows' order
                .map(Nic::ip)
                .distinct()
                .toList();
    }

    private static boolean isInternal(Nic n) {
        String text = (n.name() + " " + n.displayName()).toLowerCase(Locale.ROOT);
        return INTERNAL_ADAPTERS.stream().anyMatch(text::contains);
    }

    private static int rank(Nic n) {
        if (WINDOWS_HOTSPOT.equals(n.ip())) {
            return 0;
        }
        String text = (n.name() + " " + n.displayName()).toLowerCase(Locale.ROOT);
        if (text.startsWith("wlan") || text.contains("wi-fi") || text.contains("wireless")) {
            return 1;
        }
        if (text.startsWith("eth") || text.contains("ethernet")) {
            return 2;
        }
        return 3;
    }

    /** The address customers open. Port 80 is left out, so the address is short and easy to type. */
    public static String url(String ip, int port) {
        return port == 80 ? "http://" + ip + "/" : "http://" + ip + ":" + port + "/";
    }
}
