package io.lettuce.core.failover;

import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Factory interface to obtain direct {@link StatefulRedisConnection connections} to Redis database nodes. Connections created
 * by this factory are raw connections without CircuitBreaker counting, health checks, or other management features.
 *
 * @author Ivo Gaydazhiev
 * @since 7.4
 */
@Experimental
@FunctionalInterface
public interface RawConnectionFactory {

    /**
     * Creates a new bare connection to the specified database endpoint. The connection is created without CircuitBreaker
     * counting, health checks, or other management features.
     *
     * @param endpoint the Redis URI of the database endpoint
     * @return a new stateful Redis connection to the specified endpoint
     */
    StatefulRedisConnection<?, ?> create(RedisURI endpoint);

}
