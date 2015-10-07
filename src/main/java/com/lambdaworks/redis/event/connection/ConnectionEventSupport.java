package com.lambdaworks.redis.event.connection;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.SocketAddress;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.4
 */
abstract class ConnectionEventSupport implements ConnectionEvent {

    private final SocketAddress local;
    private final SocketAddress remote;

    ConnectionEventSupport(SocketAddress local, SocketAddress remote) {
        checkArgument(local != null, "local must not be null");
        checkArgument(remote != null, "remote must not be null");

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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConnectionEventSupport))
            return false;

        ConnectionEventSupport that = (ConnectionEventSupport) o;

        if (!local.equals(that.local))
            return false;
        if (!remote.equals(that.remote))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = local.hashCode();
        result = 31 * result + remote.hashCode();
        return result;
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
