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

    private final RedisUriDetails redisUriDetails;

    public SocketAddressEndpoint(SocketAddress socketAddress) {
        this(socketAddress, null);
    }

    SocketAddressEndpoint(SocketAddress socketAddress, RedisUriDetails redisUriDetails) {
        this.socketAddress = socketAddress;
        this.redisUriDetails = redisUriDetails;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    /**
     * @return the connection details or {@code null} if the endpoint was created without a {@code RedisURI}.
     */
    RedisUriDetails getRedisUriDetails() {
        return redisUriDetails;
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
