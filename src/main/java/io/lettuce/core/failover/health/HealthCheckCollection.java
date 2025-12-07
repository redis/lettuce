package io.lettuce.core.failover.health;

import io.lettuce.core.RedisURI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collection of health checks.
 *
 * <p>
 * This class provides methods to add, remove, and retrieve health checks for different Redis endpoints. On {@link #close()} all
 * health checks are stopped.
 * </p>
 *
 * @author Ali Takavci
 * @author Ivo Gaydazhiev
 **/
public class HealthCheckCollection {

    private final Map<RedisURI, HealthCheck> healthChecks = new ConcurrentHashMap<>();

    public HealthCheck add(HealthCheck healthCheck) {
        return healthChecks.put(healthCheck.getEndpoint(), healthCheck);
    }

    public HealthCheck[] addAll(HealthCheck[] healthChecks) {
        HealthCheck[] old = new HealthCheck[healthChecks.length];
        for (int i = 0; i < healthChecks.length; i++) {
            old[i] = add(healthChecks[i]);
        }
        return old;
    }

    public HealthCheck remove(RedisURI endpoint) {
        HealthCheck old = healthChecks.remove(endpoint);
        if (old != null) {
            old.stop();
        }
        return old;
    }

    public HealthCheck remove(HealthCheck healthCheck) {
        HealthCheck[] temp = new HealthCheck[1];
        healthChecks.computeIfPresent(healthCheck.getEndpoint(), (key, existing) -> {
            if (existing == healthCheck) {
                temp[0] = existing;
                return null;
            }
            return existing;
        });
        return temp[0];
    }

    public HealthCheck get(RedisURI endpoint) {
        return healthChecks.get(endpoint);
    }

    public void close() {
        for (HealthCheck healthCheck : healthChecks.values()) {
            healthCheck.stop();
        }
    }

}
