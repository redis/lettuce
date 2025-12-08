package io.lettuce.core.failover.health;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.DatabaseConnectionProvider;

/**
 * Supplier for health check strategies.
 *
 * @author Ivo Gaydazhiev
 * @since 7.1
 */
public interface HealthCheckStrategySupplier {

    /**
     * Get the health check strategy for the given Redis URI and client options.
     *
     * @param redisURI the Redis URI
     * @param clientOptions the client options
     * @return the health check strategy
     */
    HealthCheckStrategy get(RedisURI redisURI, DatabaseConnectionProvider connectionProvider);

}
