package io.lettuce.core.cluster.event;

import java.util.List;

import io.lettuce.core.RedisURI;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.DurationalEvent;

import static io.lettuce.core.event.DurationalEvent.EventStatus.COMPLETED;
import static io.lettuce.core.event.DurationalEvent.EventStatus.IN_PROGRESS;

/**
 * Event for initiating a topology refresh.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class TopologyRefreshEvent implements DurationalEvent, Event {

    private final List<RedisURI> topologyRefreshSource;
    private EventStatus status;

    public TopologyRefreshEvent(List<RedisURI> topologyRefreshSource) {
        this.topologyRefreshSource = topologyRefreshSource;
        this.status = IN_PROGRESS;
    }

    public List<RedisURI> getTopologyRefreshSource() {
        return topologyRefreshSource;
    }

    @Override
    public void completeEvent() {
        this.status = COMPLETED;
    }

    @Override
    public DurationalEvent.EventStatus getEventStatus() {
        return status;
    }
}
