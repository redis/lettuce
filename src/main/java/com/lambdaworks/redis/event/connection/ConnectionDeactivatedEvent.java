package com.lambdaworks.redis.event.connection;

import java.net.SocketAddress;

/**
 * Event for a connection deactivation.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.4
 */
public class ConnectionDeactivatedEvent extends ConnectionEventSupport {
    public ConnectionDeactivatedEvent(SocketAddress local, SocketAddress remote) {
        super(local, remote);
    }
}
