package io.lettuce.core.cluster.event;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.event.Event;

/**
 * Flight recorder event variant of {@link ClusterTopologyChangedEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Cluster Events" })
@Label("Topology Changed")
@StackTrace(false)
class JfrClusterTopologyChangedEvent extends jdk.jfr.Event implements Event {

    private final String topology;

    /**
     * Creates a new {@link JfrClusterTopologyChangedEvent}.
     */
    public JfrClusterTopologyChangedEvent(ClusterTopologyChangedEvent event) {

        StringBuilder builder = new StringBuilder();

        for (RedisClusterNode redisClusterNode : event.after()) {
            builder.append(String.format("%s [%s] %s %s\n", redisClusterNode.getNodeId(), redisClusterNode.getUri(),
                    redisClusterNode.getFlags(), redisClusterNode.getRole()));
        }

        this.topology = builder.toString();
    }

}
