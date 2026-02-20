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
     * Get the weight for endpoint priority.
     *
     * @return the weight
     */
    float getWeight();

    /**
     * Set the weight for endpoint priority.
     *
     * @param weight the weight
     */
    void setWeight(float weight);

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

    /**
     * Check if this database is healthy.
     *
     * @return true if the database is healthy, false otherwise
     */
    boolean isHealthy();

}
