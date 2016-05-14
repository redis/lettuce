package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.ConnectionEvents.Reconnect;
import com.lambdaworks.redis.protocol.ReconnectionListener;

/**
 * @author Mark Paluch
 */
class ReconnectEventListener implements ReconnectionListener {

    private final ClusterEventListener clusterEventListener;

    public ReconnectEventListener(ClusterEventListener clusterEventListener) {
        this.clusterEventListener = clusterEventListener;
    }

    @Override
    public void onReconnect(Reconnect reconnect) {
        clusterEventListener.onReconnection(reconnect.getAttempt());
    }
}
