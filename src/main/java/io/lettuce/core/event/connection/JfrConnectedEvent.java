package io.lettuce.core.event.connection;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.StackTrace;

/**
 * Flight recorder event variant of {@link ConnectedEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Connected")
@StackTrace(false)
class JfrConnectedEvent extends Event {

    private final String redisUri;

    private final String epId;

    private final String channelId;

    private final String local;

    private final String remote;

    public JfrConnectedEvent(ConnectionEventSupport event) {
        this.redisUri = event.getRedisUri();
        this.epId = event.getChannelId();
        this.channelId = event.getChannelId();
        this.local = event.localAddress().toString();
        this.remote = event.remoteAddress().toString();
    }

}
