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
import io.lettuce.core.failover.CircuitBreaker;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.metrics.MetricsSnapshot;

/**
 * Represents a Redis database with a weight and a connection.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
public interface RedisDatabase {

    /**
     * Get the unique ID for this database.
     *
     * @return the unique ID
     */
    String getId();

    /**
     * Get the weight for load balancing.
     *
     * @return the weight
     */
    float getWeight();

    /**
     * Get the Redis URI for this database.
     *
     * @return the Redis URI
     */
    RedisURI getRedisURI();

    /**
     * Get the metrics snapshot for this database.
     *
     * @return the metrics snapshot
     */
    MetricsSnapshot getMetricsSnapshot();

    /**
     * Get the health check status for this database.
     *
     * @return the health check status
     */
    HealthStatus getHealthCheckStatus();

    /**
     * Get the circuit breaker state for this database.
     *
     * @return the circuit breaker state
     */
    CircuitBreaker.State getCircuitBreakerState();

}
