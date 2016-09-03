package com.lambdaworks.redis.event.connection;

import java.net.SocketAddress;

import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * @author Mark Paluch
 * @since 3.4
 */
abstract class ConnectionEventSupport implements ConnectionEvent {

    private final SocketAddress local;
    private final SocketAddress remote;

    ConnectionEventSupport(SocketAddress local, SocketAddress remote) {
        LettuceAssert.notNull(local, "Local must not be null");
        LettuceAssert.notNull(remote, "Remote must not be null");

        this.local = local;
        this.remote = remote;
    }

    /**
     * Returns the local address.
     * 
     * @return the local address
     */
    public SocketAddress localAddress() {
        return local;
    }

    /**
     * Returns the remote address.
     * 
     * @return the remote address
     */
    public SocketAddress remoteAddress() {
        return remote;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append(local);
        sb.append(" -> ").append(remote);
        sb.append(']');
        return sb.toString();
    }
}
