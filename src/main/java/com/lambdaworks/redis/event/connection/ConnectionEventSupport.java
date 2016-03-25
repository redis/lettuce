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
        LettuceAssert.notNull(local, "local must not be null");
        LettuceAssert.notNull(remote, "remote must not be null");

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
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        appendConnectionId(sb);
        sb.append(']');
        return sb.toString();
    }

    void appendConnectionId(StringBuffer sb) {
        sb.append(local);
        sb.append(" -> ").append(remote);
    }
}
