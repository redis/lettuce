package io.lettuce.core.failover.metrics;

/**
 * Factory for creating {@link CircuitBreakerMetrics} instances.
 */
public final class MetricsFactory {

    private static Clock DEFAULT_CLOCK = Clock.SYSTEM;

    private MetricsFactory() {
    }

    /**
     * Create the default lock-free metrics implementation with the specified window size.
     *
     * @param windowSize the window size in seconds
     * @return the default {@link CircuitBreakerMetrics} implementation
     */
    public static CircuitBreakerMetrics createDefaultMetrics(int windowSize) {
        return new LockFreeSlidingTimeWindowMetrics(windowSize, DEFAULT_CLOCK);
    }

}
