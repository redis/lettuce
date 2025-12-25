/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     * <p>
     * This method performs a thread-safe switch to the specified database endpoint. If the target database is already the
     * current database, this is a no-op. The method verifies that the target database is healthy and has a closed circuit
     * breaker before switching.
     *
     * @param redisURI the Redis URI of the database to switch to, must not be {@code null}
     * @throws IllegalArgumentException if the database endpoint is not registered in the connection map
     * @throws IllegalStateException if the requested database is unhealthy or circuit breaker is open, or if the requested
     *         database is a different instance than registered in connection map but with the same target endpoint/uri, or if
     *         the switch operation fails
     * @throws UnsupportedOperationException if the source or destination endpoint cannot be located
     */
    void switchTo(RedisURI redisURI);

    /**
     * Get the current database endpoint.
     *
     * @return the current database endpoint
     */
    RedisURI getCurrentEndpoint();

    /**
     * Get the current database.
     *
     * @return the current database
     */
    RedisDatabase getCurrentDatabase();

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
     * Get the {@link RedisDatabase} for a specific endpoint.
     *
     * @param endpoint the Redis endpoint URI
     * @return the {@link RedisDatabase} for the endpoint
     * @throws IllegalArgumentException if the endpoint is not known
     */
    RedisDatabase getDatabase(RedisURI endpoint);

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
