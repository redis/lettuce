package io.lettuce.core.event.connection;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;

/**
 * Flight recorder event variant of {@link ReconnectAttemptEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Reconnect attempt")
@StackTrace(false)
public class JfrReconnectAttemptEvent extends Event {

    private final String redisUri;

    private final String epId;

    private final String remote;

    private final int attempt;

    @Timespan
    private final long delay;

    public JfrReconnectAttemptEvent(ReconnectAttemptEvent event) {

        this.epId = event.getEpId();
        this.remote = event.remoteAddress().toString();
        this.redisUri = event.getRedisUri();
        this.attempt = event.getAttempt();
        this.delay = event.getDelay().toNanos();
    }

}
