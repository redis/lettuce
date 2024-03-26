package io.lettuce.core.event.connection;

import java.net.SocketAddress;

/**
 * Event for a established TCP-level connection.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class ConnectedEvent extends ConnectionEventSupport {

    public ConnectedEvent(String redisUri, String epId, String channelId, SocketAddress local, SocketAddress remote) {
        super(redisUri, epId, channelId, local, remote);
    }

    public ConnectedEvent(SocketAddress local, SocketAddress remote) {
        super(local, remote);
    }

}
