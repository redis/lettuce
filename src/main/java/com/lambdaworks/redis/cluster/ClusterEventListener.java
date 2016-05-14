package com.lambdaworks.redis.cluster;

/**
 * @author Mark Paluch
 */
interface ClusterEventListener {

    void onAskRedirection();

    void onMovedRedirection();

    void onReconnection(int attempt);

    static ClusterEventListener NO_OP = new ClusterEventListener() {
        @Override
        public void onAskRedirection() {

        }

        @Override
        public void onMovedRedirection() {

        }

        @Override
        public void onReconnection(int attempt) {

        }
    };
}
