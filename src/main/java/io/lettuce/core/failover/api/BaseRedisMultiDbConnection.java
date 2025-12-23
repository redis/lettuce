package io.lettuce.core.failover.api;

import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.failover.DatabaseConfig;

/**
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public interface BaseRedisMultiDbConnection {

    /**
     * Switch to a different database.
     *
     * @param redisURI the Redis URI of the database to switch to, must not be {@code null}
     * @return true if the switch was successful, otherwise false due to one of the following reasons;
     *         <p>
     *         - the requested database is unhealthy or gets unhealthy in the process of switching
     *         <p>
     *         - the requested database is the same as the current one
     *         <p>
     *         - the requested database is not found
     * @throws IllegalArgumentException if the database does not exist
     */
    boolean switchTo(RedisURI redisURI);

    /**
     * Get the current database endpoint.
     *
     * @return the current database endpoint
     */
    RedisURI getCurrentEndpoint();

    /**
     * Get all available database endpoints.
     *
     * @return an iterable of all database endpoints
     */
    Iterable<RedisURI> getEndpoints();

    /**
     * Check if an endpoint is healthy (health status is HEALTHY and circuit breaker is CLOSED).
     *
     * @param endpoint the Redis endpoint URI
     * @return true if the endpoint is healthy (HEALTHY status and CLOSED circuit breaker), false otherwise
     * @throws IllegalArgumentException if the endpoint is not known
     */
    boolean isHealthy(RedisURI endpoint);

    /**
     * Get the {@link BaseRedisDatabase} for a specific endpoint.
     *
     * @param redisURI the Redis endpoint URI
     * @return the {@link BaseRedisDatabase} for the endpoint
     * @throws IllegalArgumentException if the endpoint is not known
     */
    BaseRedisDatabase getDatabase(RedisURI redisURI);

    /**
     * Add a new database to the multi-database connection.
     *
     * @param redisURI the Redis URI for the new database, must not be {@code null}
     * @param weight the weight for load balancing, must be greater than 0
     * @throws IllegalArgumentException if the database already exists or parameters are invalid
     */
    void addDatabase(RedisURI redisURI, float weight);

    /**
     * Add a new database to the multi-database connection.
     *
     * @param databaseConfig the database configuration, must not be {@code null}
     * @throws IllegalArgumentException if the database already exists or configuration is invalid
     */
    void addDatabase(DatabaseConfig databaseConfig);

    /**
     * Remove a database from the multi-database connection.
     *
     * @param redisURI the Redis URI of the database to remove, must not be {@code null}
     * @throws IllegalArgumentException if the database does not exist
     * @throws UnsupportedOperationException if attempting to remove the currently active database
     */
    void removeDatabase(RedisURI redisURI);

}
