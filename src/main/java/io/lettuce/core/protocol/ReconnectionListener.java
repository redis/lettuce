package io.lettuce.core.protocol;

import io.lettuce.core.ConnectionEvents;

/**
 * Listener for reconnection events.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public interface ReconnectionListener {

    ReconnectionListener NO_OP = new ReconnectionListener() {

        @Override
        public void onReconnectAttempt(ConnectionEvents.Reconnect reconnect) {

        }

    };

    /**
     * Listener method notified on a reconnection attempt.
     *
     * @param reconnect the event payload.
     */
    void onReconnectAttempt(ConnectionEvents.Reconnect reconnect);

}
