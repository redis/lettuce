package io.lettuce.core.failover.health;

import io.lettuce.core.RedisURI;

/**
 * Manager for health status of Redis endpoints.
 * <p>
 * The health status manager coordinates health checks for multiple endpoints and provides a unified view of their health
 * statuses. It also allows registering listeners to be notified when the health status of any endpoint changes.
 * </p>
 * <p>
 * The health status manager is responsible for:
 * <ul>
 * <li>Creating and managing health checks for each endpoint</li>
 * <li>Notifying listeners when the health status of any endpoint changes</li>
 * <li>Providing a unified view of the health statuses of all endpoints</li>
 * </ul>
 *
 * @author Ali Takavci
 * @author Ivo Gaydazhiev
 * @since 7.1
 */
public interface HealthStatusManager {

    /**
     * Create and register a health check for the given endpoint.
     *
     * @param endpoint the endpoint to check
     * @param strategy the health check strategy
     * @return the health check
     */
    HealthCheck add(RedisURI endpoint, HealthCheckStrategy strategy);

    /**
     * Unregister the health check for the given endpoint.
     *
     * @param endpoint the endpoint to check
     */
    void remove(RedisURI endpoint);

    /**
     * Get the health status for the given endpoint.
     *
     * @param endpoint the endpoint to check
     * @return the health status
     */
    HealthStatus getHealthStatus(RedisURI endpoint);

    /**
     * Register a global listener to be notified when the health status of any endpoint changes.
     *
     * @param listener the listener to add, must not be {@code null}
     */
    void registerListener(HealthStatusListener listener);

    /**
     * Unregister a previously registered global listener.
     *
     * @param listener the listener to remove, must not be {@code null}
     */
    void unregisterListener(HealthStatusListener listener);

    /**
     * Register a listener to be notified when the health status of the given endpoint changes.
     *
     * @param endpoint the endpoint to check
     * @param listener the listener to add, must not be {@code null}
     */
    void registerListener(RedisURI endpoint, HealthStatusListener listener);

    /**
     * Unregister a previously registered listener for the given endpoint.
     *
     * @param endpoint the endpoint to check
     * @param listener the listener to remove, must not be {@code null}
     */
    void unregisterListener(RedisURI endpoint, HealthStatusListener listener);

    /**
     * Get the maximum wait time for the health check to complete.
     *
     * @param endpoint the endpoint to check
     * @return the maximum wait time in milliseconds
     */
    long getMaxWaitFor(RedisURI endpoint);

    /**
     * Close the health status manager and stop all health checks.
     */
    void close();

}
