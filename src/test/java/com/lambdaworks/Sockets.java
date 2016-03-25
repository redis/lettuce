package com.lambdaworks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * @author Mark Paluch
 */
public class Sockets {
    public static boolean isOpen(String host, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), (int) TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS));
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private Sockets() {
        // unused
    }
}
