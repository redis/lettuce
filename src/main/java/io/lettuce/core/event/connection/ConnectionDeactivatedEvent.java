package io.lettuce.core.event.connection;

import java.net.SocketAddress;

/**
 * Event for a connection deactivation.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class ConnectionDeactivatedEvent extends ConnectionEventSupport {

    public ConnectionDeactivatedEvent(String redisUri, String epId, String channelId, SocketAddress local,
            SocketAddress remote) {
        super(redisUri, epId, channelId, local, remote);
    }

    public ConnectionDeactivatedEvent(SocketAddress local, SocketAddress remote) {
        super(local, remote);
    }

}
