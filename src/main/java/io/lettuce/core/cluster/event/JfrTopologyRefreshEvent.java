package io.lettuce.core.cluster.event;

import java.util.StringJoiner;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import io.lettuce.core.RedisURI;

/**
 * Flight recorder event variant of {@link TopologyRefreshEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Cluster Events" })
@Label("Topology Refresh")
@StackTrace(value = false)
class JfrTopologyRefreshEvent extends Event {

    private final String sources;

    public JfrTopologyRefreshEvent(TopologyRefreshEvent event) {

        StringJoiner joiner = new StringJoiner(", ");

        for (RedisURI redisURI : event.getTopologyRefreshSource()) {
            joiner.add(redisURI.toString());
        }

        this.sources = joiner.toString();
    }

}
