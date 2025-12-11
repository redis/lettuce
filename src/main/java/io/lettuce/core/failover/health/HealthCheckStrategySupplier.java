package io.lettuce.core.failover.health;

import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.DatabaseRawConnectionFactory;

/**
 * Supplier for health check strategies.
 *
 * @author Ivo Gaydazhiev
 * @since 7.1
 */
public interface HealthCheckStrategySupplier {

    /**
     * No health check strategy supplier. When used, no health checks will be performed.
     *
     * @since 7.4
     */
    HealthCheckStrategySupplier NO_HEALTH_CHECK = (uri, factory) -> null;

    /**
     * Get the health check strategy for the given Redis URI
     *
     * @param redisURI the Redis URI
     * @param connectionFactory the connection factory
     * @return the health check strategy
     */
    HealthCheckStrategy get(RedisURI redisURI, DatabaseRawConnectionFactory connectionFactory);

}
