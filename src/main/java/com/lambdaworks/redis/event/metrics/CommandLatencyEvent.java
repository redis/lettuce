package com.lambdaworks.redis.event.metrics;

import java.util.Map;

import com.lambdaworks.redis.event.Event;
import com.lambdaworks.redis.metrics.CommandLatencyId;
import com.lambdaworks.redis.metrics.CommandMetrics;

/**
 * Event that transports command latency metrics. This event carries latencies for multiple commands and connections.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class CommandLatencyEvent implements Event {

    private Map<CommandLatencyId, CommandMetrics> latencies;

    public CommandLatencyEvent(Map<CommandLatencyId, CommandMetrics> latencies) {
        this.latencies = latencies;
    }

    /**
     * Returns the latencies mapped between {@link CommandLatencyId connection/command} and the {@link CommandMetrics metrics}.
     * 
     * @return the latency map.
     */
    public Map<CommandLatencyId, CommandMetrics> getLatencies() {
        return latencies;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(latencies);
        return sb.toString();
    }
}
