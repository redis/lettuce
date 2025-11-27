package io.lettuce.core.failover.health;

import io.lettuce.core.RedisURI;

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

}
