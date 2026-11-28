package com.printhub.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listens on the https port and resets every connection at once. The portal is plain http, but phone browsers try
 * https:// first. When nothing answers on 443, Windows' firewall can drop the attempt silently and the browser waits,
 * or gives up with an error. A reset tells the browser straight away that https is not there, so it opens the
 * http:// address instead.
 */
final class HttpsRefuser {

    private final ServerSocket socket;

    private HttpsRefuser(ServerSocket socket) {
        this.socket = socket;
    }

    /** Opens the port, or returns null if it is in use or not allowed (the portal still works without it). */
    static HttpsRefuser open(int port) {
        try {
            ServerSocket ss = new ServerSocket();
            ss.bind(new InetSocketAddress(port));
            HttpsRefuser refuser = new HttpsRefuser(ss);
            Thread t = new Thread(refuser::acceptLoop, "printhub-https-refuser");
            t.setDaemon(true);
            t.start();
            return refuser;
        } catch (IOException e) {
            return null;
        }
    }

    int port() {
        return socket.getLocalPort();
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // already closed
        }
    }

    private void acceptLoop() {
        while (!socket.isClosed()) {
            try (Socket client = socket.accept()) {
                client.setSoLinger(true, 0); // close with a reset, not a polite goodbye
            } catch (IOException e) {
                // the socket was closed by stop(), or one client misbehaved; keep going until closed
            }
        }
    }
}
