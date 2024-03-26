package io.lettuce.core.event.connection;

import java.net.SocketAddress;

import io.lettuce.core.ClientOptions;

/**
 * Event for a connection activation (after SSL-handshake, {@link ClientOptions#isPingBeforeActivateConnection() PING before
 * activation}, and buffered command replay).
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class ConnectionActivatedEvent extends ConnectionEventSupport {

    public ConnectionActivatedEvent(String redisUri, String epId, String channelId, SocketAddress local, SocketAddress remote) {
        super(redisUri, epId, channelId, local, remote);
    }

    public ConnectionActivatedEvent(SocketAddress local, SocketAddress remote) {
        super(local, remote);
    }

}
