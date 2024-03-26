package io.lettuce.core.event.connection;

import io.lettuce.core.event.Event;

/**
 * Event to connect to Redis.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class ConnectEvent implements Event {

    private final String redisUri;

    private final String epId;

    public ConnectEvent(String redisUri, String epId) {
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
