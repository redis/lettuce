package io.lettuce.core.failover;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Provider for accessing existing database connections from MultiDbClient. This allows health check strategies to reuse the
 * same connection infrastructure as the main MultiDbClient instead of creating separate connections, reducing resource usage.
 *
 * @author Ivo Gaydazhiev
 * @since 7.1
 */
@FunctionalInterface
public interface DatabaseConnectionProvider {

    /**
     * Get a connection to the specified database endpoint. The connection is managed by the MultiDbClient and should be used
     * for health checks. The returned connection should not be closed by the caller.
     *
     * @param endpoint the Redis URI of the database endpoint
     * @return a stateful Redis connection to the specified endpoint, or null if no connection exists for this endpoint
     */
    StatefulRedisConnection<?, ?> getConnection(RedisURI endpoint);

}
