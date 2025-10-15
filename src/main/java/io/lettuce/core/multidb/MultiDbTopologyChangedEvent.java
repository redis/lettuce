package io.lettuce.core.multidb;

import io.lettuce.core.RedisURI;
import io.lettuce.core.event.Event;
import io.lettuce.core.models.role.RedisNodeDescription;

import java.util.List;

/**
 * Event triggered after obtaining the Master/Replica topology.
 *
 * @author Mark Paluch
 * @since 6.1
 */
class MultiDbTopologyChangedEvent implements Event {

    private final RedisURI source;

    private final List<RedisNodeDescription> nodes;

    public MultiDbTopologyChangedEvent(RedisURI source, List<RedisNodeDescription> nodes) {
        this.source = source;
        this.nodes = nodes;
    }

    public RedisURI getSource() {
        return source;
    }

    public List<RedisNodeDescription> getNodes() {
        return nodes;
    }

}
