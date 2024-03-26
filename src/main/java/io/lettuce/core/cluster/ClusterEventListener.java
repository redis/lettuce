package io.lettuce.core.cluster;

/**
 * Event listener for cluster state/cluster node events.
 *
 * @author Mark Paluch
 */
interface ClusterEventListener {

    /**
     * Event callback if a command receives a {@literal ASK} redirection.
     */
    default void onAskRedirection() {
    }

    /**
     * Event callback if a command receives a {@literal MOVED} redirection.
     */
    default void onMovedRedirection() {
    }

    /**
     * Event callback if a connection tries to reconnect.
     */
    default void onReconnectAttempt(int attempt) {
    }

    /**
     * Event callback if a command should be routed to a slot that is not covered.
     *
     * @since 5.2
     */
    default void onUncoveredSlot(int slot) {
    }

    /**
     * Event callback if a connection is attempted to an unknown node.
     *
     * @since 5.1
     */
    default void onUnknownNode() {
    }

    ClusterEventListener NO_OP = new ClusterEventListener() {
    };

}
