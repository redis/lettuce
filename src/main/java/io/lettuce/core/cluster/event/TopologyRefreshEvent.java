package io.lettuce.core.cluster.event;

import java.util.List;

import io.lettuce.core.RedisURI;
import io.lettuce.core.event.Event;

/**
 * Event for initiating a topology refresh.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class TopologyRefreshEvent implements Event {

    private final List<RedisURI> topologyRefreshSource;

    public TopologyRefreshEvent(List<RedisURI> topologyRefreshSource) {
        this.topologyRefreshSource = topologyRefreshSource;
    }

    public List<RedisURI> getTopologyRefreshSource() {
        return topologyRefreshSource;
    }

}
