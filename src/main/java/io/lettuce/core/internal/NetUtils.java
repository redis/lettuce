package io.lettuce.core.internal;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class NetUtils {

    /**
     * Determine if the given {@link SocketAddress} represents a private IP address.
     *
     * @return {@code true} if the given {@link SocketAddress} represents a private IP address.
     */
    public static boolean isPrivateIp(SocketAddress socketAddress) {
        if (!(socketAddress instanceof InetSocketAddress)) {
            return false;
        }

        InetAddress address = ((InetSocketAddress) socketAddress).getAddress();
        if (address == null || address.isAnyLocalAddress()) {
            return false;
        }

        return address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || isUniqueLocalAddress(address);
    }

    // https://datatracker.ietf.org/doc/html/rfc4193
    private static boolean isUniqueLocalAddress(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte[] bytes = address.getAddress();
        return (bytes[0] & (byte) 0xfe) == (byte) 0xfc; // fc00::/7
    }

}
