
package io.lettuce.core.failover.health;

import io.lettuce.core.RedisURI;

/**
 * Interface for health check operations on a Redis endpoint.
 * <p>
 * A health check periodically monitors the health status of a Redis endpoint and notifies registered listeners when the status
 * changes.
 * </p>
 *
 * @author Ali Takavci
 * @author Ivo Gaydazhiev
 * @since 7.1
 */
public interface HealthCheck {

    /**
     * Get the endpoint being monitored.
     *
     * @return the Redis URI of the endpoint
     */
    RedisURI getEndpoint();

    /**
     * Get the current health status of the endpoint.
     *
     * @return the current health status (HEALTHY, UNHEALTHY, or UNKNOWN)
     */
    HealthStatus getStatus();

    /**
     * Stop the health check. This will stop the periodic health check execution and clean up resources.
     */
    void stop();

    /**
     * Start the health check. This will begin periodic health check execution.
     */
    void start();

    /**
     * Get the maximum wait duration (in milliseconds) to wait for health check HealthStatus reach stable state.
     * <p>
     * Transition to stable state means either HEALTHY or UNHEALTHY. UNKNOWN is not considered stable. This is calculated based
     * on the health check strategy's timeout, retry delay and retry count.
     * </p>
     *
     * @return the maximum wait duration in milliseconds
     */
    long getMaxWaitFor();

    /**
     * Add a listener to be notified when the health status changes.
     * <p>
     * The listener will be invoked whenever the health status transitions from one state to another (e.g., HEALTHY to
     * UNHEALTHY, UNKNOWN to HEALTHY, etc.).
     * </p>
     *
     * @param listener the listener to add, must not be {@code null}
     */
    void addListener(HealthStatusListener listener);

    /**
     * Remove a previously registered listener.
     *
     * @param listener the listener to remove, must not be {@code null}
     */
    void removeListener(HealthStatusListener listener);

}
