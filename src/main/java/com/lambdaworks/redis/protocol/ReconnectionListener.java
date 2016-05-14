package com.lambdaworks.redis.protocol;

import com.lambdaworks.redis.ConnectionEvents;

/**
 * Listener for reconnection events.
 * 
 * @author Mark Paluch
 * @since 4.2
 */
public interface ReconnectionListener {

    ReconnectionListener NO_OP = new ReconnectionListener() {
        @Override
        public void onReconnect(ConnectionEvents.Reconnect reconnect) {

        }
    };

    /**
     * Listener method notified on a reconnection attempt.
     * 
     * @param reconnect the event payload.
     */
    void onReconnect(ConnectionEvents.Reconnect reconnect);

}
