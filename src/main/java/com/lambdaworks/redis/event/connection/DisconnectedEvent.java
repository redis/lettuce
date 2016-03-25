package com.lambdaworks.redis.event.connection;

import java.net.SocketAddress;

/**
 * Event for a disconnect on TCP-level.
 * 
 * @author Mark Paluch
 * @since 3.4
 */
public class DisconnectedEvent extends ConnectionEventSupport {
    public DisconnectedEvent(SocketAddress local, SocketAddress remote) {
        super(local, remote);
    }
}
