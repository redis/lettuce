package io.lettuce.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author Mark Paluch
 * @soundtrack Ronski Speed - Maracaido Sessions, formerly Tool Sessions (May 2016)
 */
public class CanConnect {

    /**
     * Check whether a TCP connection can be established to the given {@link SocketAddress}.
     *
     * @param host
     * @param port
     * @return
     */
    public static boolean to(String host, int port) {
        return to(new InetSocketAddress(host, port));
    }

    /**
     * Check whether a TCP connection can be established to the given {@link SocketAddress}.
     *
     * @param socketAddress
     * @return
     */
    private static boolean to(SocketAddress socketAddress) {

        try {
            Socket socket = new Socket();
            socket.connect(socketAddress, (int) TimeUnit.SECONDS.toMillis(5));
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
