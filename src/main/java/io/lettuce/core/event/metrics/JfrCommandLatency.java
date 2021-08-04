package io.lettuce.core.event.metrics;

import io.lettuce.core.metrics.CommandLatencyId;
import io.lettuce.core.metrics.CommandMetrics;
import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

import java.util.concurrent.TimeUnit;

/**
 * A JFR event for Each Command Latency
 * triggered by JfrCommandLatencyEvent
 *
 * @see JfrCommandLatency
 * @author Hash.Jang
 */
@Category({ "Lettuce", "Command Events" })
@Label("Command Latency")
@StackTrace(false)
public class JfrCommandLatency extends Event {
    private final String remoteAddress;
    private final String commandType;
    private final long count;
    private final TimeUnit timeUnit;
    private final long firstResponseMin;
    private final long firstResponseMax;
    private final String firstResponsePercentiles;
    private final long completionResponseMin;
    private final long completionResponseMax;
    private final String completionResponsePercentiles;

    public JfrCommandLatency(CommandLatencyId commandLatencyId, CommandMetrics commandMetrics) {
        this.remoteAddress = commandLatencyId.remoteAddress().toString();
        this.commandType = commandLatencyId.commandType().toString();
        this.count = commandMetrics.getCount();
        this.timeUnit = commandMetrics.getTimeUnit();
        this.firstResponseMin = commandMetrics.getFirstResponse().getMin();
        this.firstResponseMax = commandMetrics.getFirstResponse().getMax();
        this.firstResponsePercentiles = commandMetrics.getFirstResponse().getPercentiles().toString();
        this.completionResponseMin = commandMetrics.getCompletion().getMin();
        this.completionResponseMax = commandMetrics.getCompletion().getMax();
        this.completionResponsePercentiles = commandMetrics.getCompletion().getPercentiles().toString();
    }
}
