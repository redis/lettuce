package io.lettuce.core.masterreplica;

import static jdk.jfr.Timespan.*;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;

/**
 * Flight recorder event variant of {@link SentinelTopologyRefreshEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Master/Replica Events" })
@Label("Sentinel Topology Refresh Trigger")
@StackTrace(false)
class JfrSentinelTopologyRefreshEvent extends Event {

    private final String source;

    private final String message;

    @Timespan(value = MILLISECONDS)
    private final long delay;

    public JfrSentinelTopologyRefreshEvent(SentinelTopologyRefreshEvent event) {
        this.source = event.getSource();
        this.message = event.getMessage();
        this.delay = event.getDelayMs();
    }

}
