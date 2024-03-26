package io.lettuce.core.event.metrics;

import java.util.Map;

import io.lettuce.core.event.Event;
import io.lettuce.core.metrics.CommandLatencyId;
import io.lettuce.core.metrics.CommandMetrics;

/**
 * Event that transports command latency metrics. This event carries latencies for multiple commands and connections.
 *
 * @author Mark Paluch
 */
public class CommandLatencyEvent implements Event {

    private final Map<CommandLatencyId, CommandMetrics> latencies;

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
        return latencies.toString();
    }

}
