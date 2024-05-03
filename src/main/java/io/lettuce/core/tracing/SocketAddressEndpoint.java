package io.lettuce.core.tracing;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * {@link Tracing.Endpoint} based on a {@link SocketAddress}.
 *
 * @author Mark Paluch
 * @since 6.3
 */
class SocketAddressEndpoint implements Tracing.Endpoint {

    private final SocketAddress socketAddress;

    public SocketAddressEndpoint(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    @Override
    public String toString() {

        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) socketAddress;
            return inet.getHostString() + ":" + inet.getPort();
        }

        return socketAddress.toString();
    }

}
