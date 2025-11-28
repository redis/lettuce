package io.lettuce.core.failover.health;

import io.lettuce.core.RedisURI;

/**
 * Event arguments for health status change events.
 *
 * @author Ali Takavci
 * @author Ivo Gaydazhiev
 * @since 7.1
 */
public class HealthStatusChangeEvent {

    private final RedisURI endpoint;

    private final HealthStatus oldStatus;

    private final HealthStatus newStatus;

    public HealthStatusChangeEvent(RedisURI endpoint, HealthStatus oldStatus, HealthStatus newStatus) {
        this.endpoint = endpoint;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public RedisURI getEndpoint() {
        return endpoint;
    }

    public HealthStatus getOldStatus() {
        return oldStatus;
    }

    public HealthStatus getNewStatus() {
        return newStatus;
    }

}
