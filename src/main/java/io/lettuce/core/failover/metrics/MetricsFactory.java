package io.lettuce.core.failover.metrics;

/**
 * Factory for creating {@link CircuitBreakerMetrics} instances.
 */
public class MetricsFactory {

    public static final MetricsFactory DEFAULT = new MetricsFactory();

    MetricsFactory() {
    }

    /**
     * Create the default lock-free metrics implementation with the specified window size.
     *
     * @param windowSize the window size in seconds
     * @return the default {@link CircuitBreakerMetrics} implementation
     */
    public CircuitBreakerMetrics createDefaultMetrics(int windowSize) {
        return new LockFreeSlidingTimeWindowMetrics(windowSize);
    }

}
