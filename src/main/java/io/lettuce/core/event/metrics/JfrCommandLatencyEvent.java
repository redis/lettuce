package io.lettuce.core.event.metrics;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

/**
 * JFR Event for CommandLatencyEvent
 * A JfrCommandLatencyEvent is only a trigger of multiple JfrCommandLatency
 *
 * @see JfrCommandLatency
 * @author Hash.Jang
 */
@Category({ "Lettuce", "Command Events" })
@Label("Command Latency Trigger")
@StackTrace(false)
public class JfrCommandLatencyEvent extends Event {
    private final int size;

    public JfrCommandLatencyEvent(CommandLatencyEvent commandLatencyEvent) {
        this.size = commandLatencyEvent.getLatencies().size();
        commandLatencyEvent.getLatencies().forEach((commandLatencyId, commandMetrics) -> {
            JfrCommandLatency jfrCommandLatency = new JfrCommandLatency(commandLatencyId, commandMetrics);
            jfrCommandLatency.commit();
        });
    }
}
