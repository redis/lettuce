package io.lettuce.core.event.connection;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;

/**
 * Flight recorder event variant of {@link ConnectionCreatedEvent}.
 *
 * @author Mark Paluch
 * @since 6.1
 */
@Category({ "Lettuce", "Connection Events" })
@Label("Connection Created")
class JfrConnectionCreatedEvent extends Event {

    private final String redisUri;

    private final String epId;

    public JfrConnectionCreatedEvent(ConnectionCreatedEvent event) {
        this.redisUri = event.getRedisUri();
        this.epId = event.getEpId();
    }

    public String getRedisUri() {
        return redisUri;
    }

    public String getEpId() {
        return epId;
    }

}
