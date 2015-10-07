package com.lambdaworks.redis.event.connection;

import java.net.SocketAddress;

import com.lambdaworks.redis.ClientOptions;

/**
 * Event for a connection activation (after SSL-handshake, {@link ClientOptions#isPingBeforeActivateConnection() PING before
 * activation}, and buffered command replay).
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.4
 */
public class ConnectionActivatedEvent extends ConnectionEventSupport {
    public ConnectionActivatedEvent(SocketAddress local, SocketAddress remote) {
        super(local, remote);
    }
}
