package com.lambdaworks.redis;

import java.net.SocketAddress;

/**
 * Connection identifier. A connection identifier consists of the {@link #localAddress()} and the {@link #remoteAddress()}.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.4
 */
public interface ConnectionId {

    /**
     * Returns the local address.
     * 
     * @return the local address
     */
    SocketAddress localAddress();

    /**
     * Returns the remote address.
     * 
     * @return the remote address
     */
    SocketAddress remoteAddress();
}
