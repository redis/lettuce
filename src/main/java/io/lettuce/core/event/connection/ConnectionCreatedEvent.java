package io.lettuce.core.event.connection;

import io.lettuce.core.event.Event;

/**
 * Event for a created connection object. Creating a connection is the first step in establishing a Redis connection.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class ConnectionCreatedEvent implements Event {

    private final String redisUri;

    private final String epId;

    public ConnectionCreatedEvent(String redisUri, String epId) {
        this.redisUri = redisUri;
        this.epId = epId;
    }

    public String getRedisUri() {
        return redisUri;
    }

    public String getEpId() {
        return epId;
    }

}
