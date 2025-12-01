package io.lettuce.core.failover.metrics;

/**
 * Factory for creating {@link CircuitBreakerMetrics} instances.
 */
public final class MetricsFactory {

    private MetricsFactory() {
    }

    /**
     * Create the default lock-free metrics implementation.
     *
     * @return the default {@link CircuitBreakerMetrics} implementation
     */
    public static CircuitBreakerMetrics createDefaultMetrics() {
        return new LockFreeSlidingTimeWindowMetrics();
    }

    public static CircuitBreakerMetrics createDefaultMetrics(int windowSize) {
        return new LockFreeSlidingTimeWindowMetrics(windowSize);
    }

}
