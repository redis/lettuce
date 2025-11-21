package io.lettuce.core.failover.metrics;

/**
 * Factory for creating {@link CircuitBreakerMetrics} instances.
 */
public final class CircuitBreakerMetricsFactory {

    private CircuitBreakerMetricsFactory() {
    }

    /**
     * Create the default lock-free metrics implementation.
     *
     * @return the default {@link CircuitBreakerMetrics} implementation
     */
    public static CircuitBreakerMetrics createDefaultMetrics() {
        return new CircuitBreakerMetricsImpl();
    }

}
