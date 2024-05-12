package io.lettuce.core.cluster;

import io.lettuce.core.ConnectionEvents.Reconnect;
import io.lettuce.core.protocol.ReconnectionListener;

/**
 * @author Mark Paluch
 */
class ReconnectEventListener implements ReconnectionListener {

    private final ClusterEventListener clusterEventListener;

    public ReconnectEventListener(ClusterEventListener clusterEventListener) {
        this.clusterEventListener = clusterEventListener;
    }

    @Override
    public void onReconnectAttempt(Reconnect reconnect) {
        clusterEventListener.onReconnectAttempt(reconnect.getAttempt());
    }

}
