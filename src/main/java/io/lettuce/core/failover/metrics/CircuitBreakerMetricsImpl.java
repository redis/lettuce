package io.lettuce.core.failover.metrics;

/**
 * Lock-free, thread-safe implementation of circuit breaker metrics using a time-based sliding window. Tracks successes and
 * failures within a configurable time period.
 *
 * <p>
 * This implementation uses a lock-free sliding window mechanism to track metrics over a configurable time period (default: 2
 * seconds). Old data outside the window is automatically expired and cleaned up.
 * </p>
 *
 * @author Ali Takavci
 * @since 7.1
 */
class CircuitBreakerMetricsImpl implements CircuitBreakerMetrics {

    /**
     * Default window duration: 2 seconds.
     */
    private static final int DEFAULT_WINDOW_DURATION_SECONDS = 2;

    /**
     * Lock-free sliding window metrics implementation.
     */
    private final SlidingWindowMetrics slidingWindow;

    /**
     * Create metrics instance with default configuration (2 second window).
     */
    CircuitBreakerMetricsImpl() {
        this(DEFAULT_WINDOW_DURATION_SECONDS);
    }

    /**
     * Create metrics instance with custom window duration in seconds.
     *
     * @param windowDurationSeconds the window duration in seconds (must be >= 1)
     * @throws IllegalArgumentException if windowDurationSeconds < 1
     */
    CircuitBreakerMetricsImpl(int windowDurationSeconds) {
        this.slidingWindow = new LockFreeSlidingTimeWindowMetrics(windowDurationSeconds);
    }

    /**
     * Record a successful command execution. Lock-free operation.
     */
    @Override
    public void recordSuccess() {
        slidingWindow.recordSuccess();
    }

    /**
     * Record a failed command execution. Lock-free operation.
     */
    @Override
    public void recordFailure() {
        slidingWindow.recordFailure();
    }

    /**
     * Get a snapshot of the current metrics within the time window.
     *
     * @return an immutable snapshot of current metrics
     */
    @Override
    public MetricsSnapshot getSnapshot() {
        return slidingWindow.getSnapshot();
    }

    @Override
    public String toString() {
        return "CircuitBreakerMetrics{" + slidingWindow.getSnapshot() + '}';
    }

}
