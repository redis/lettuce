package io.lettuce.core.cluster.event;

import java.util.Collections;
import java.util.List;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.event.Event;

/**
 * Signals a discovered cluster topology change. The event carries the view {@link #before()} and {@link #after} the change.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class ClusterTopologyChangedEvent implements Event {

    private final List<RedisClusterNode> before;

    private final List<RedisClusterNode> after;

    /**
     * Creates a new {@link ClusterTopologyChangedEvent}.
     *
     * @param before the cluster topology view before the topology changed, must not be {@code null}
     * @param after the cluster topology view after the topology changed, must not be {@code null}
     */
    public ClusterTopologyChangedEvent(List<RedisClusterNode> before, List<RedisClusterNode> after) {
        this.before = Collections.unmodifiableList(before);
        this.after = Collections.unmodifiableList(after);
    }

    /**
     * Returns the cluster topology view before the topology changed.
     *
     * @return the cluster topology view before the topology changed.
     */
    public List<RedisClusterNode> before() {
        return before;
    }

    /**
     * Returns the cluster topology view after the topology changed.
     *
     * @return the cluster topology view after the topology changed.
     */
    public List<RedisClusterNode> after() {
        return after;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [before=").append(before.size());
        sb.append(", after=").append(after.size());
        sb.append(']');
        return sb.toString();
    }

}
