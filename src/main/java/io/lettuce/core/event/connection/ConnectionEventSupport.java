package io.lettuce.core.event.connection;

import java.net.SocketAddress;

import io.lettuce.core.internal.LettuceAssert;

/**
 * @author Mark Paluch
 * @since 3.4
 */
abstract class ConnectionEventSupport implements ConnectionEvent {

    private final String redisUri;

    private final String epId;

    private final String channelId;

    private final SocketAddress local;

    private final SocketAddress remote;

    ConnectionEventSupport(SocketAddress local, SocketAddress remote) {
        this(null, null, null, local, remote);
    }

    ConnectionEventSupport(String redisUri, String epId, String channelId, SocketAddress local, SocketAddress remote) {
        LettuceAssert.notNull(local, "Local must not be null");
        LettuceAssert.notNull(remote, "Remote must not be null");

        this.redisUri = redisUri;
        this.epId = epId;
        this.channelId = channelId;
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

    /**
     * @return the underlying Redis URI.
     */
    String getRedisUri() {
        return redisUri;
    }

    /**
     * @return endpoint identifier.
     */
    String getEpId() {
        return epId;
    }

    /**
     * @return channel identifier.
     */
    String getChannelId() {
        return channelId;
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
