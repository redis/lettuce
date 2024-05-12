package io.lettuce.core.event.connection;

import java.io.PrintWriter;
import java.io.StringWriter;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;

/**
 * Flight recorder event variant of {@link ReconnectFailedEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Reconnect attempt failed")
public class JfrReconnectFailedEvent extends Event {

    private final String redisUri;

    private final String epId;

    private final String remote;

    private final int attempt;

    private final String cause;

    public JfrReconnectFailedEvent(ReconnectFailedEvent event) {

        this.redisUri = event.getRedisUri();
        this.epId = event.getEpId();
        this.remote = event.remoteAddress().toString();

        StringWriter writer = new StringWriter();
        event.getCause().printStackTrace(new PrintWriter(writer));
        this.cause = writer.getBuffer().toString();
        this.attempt = event.getAttempt();
    }

}
