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
public class CircuitBreakerMetricsImpl implements CircuitBreakerMetrics {

    /**
     * Default window duration: 2 seconds.
     */
    private static final long DEFAULT_WINDOW_DURATION_MS = 2_000;

    /**
     * Default bucket duration: 1 second.
     */
    private static final long DEFAULT_BUCKET_DURATION_MS = 1_000;

    /**
     * Lock-free sliding window metrics implementation.
     */
    private final SlidingWindowMetrics slidingWindow;

    /**
     * Create metrics instance with default configuration (2 second window, 1 second buckets).
     */
    public CircuitBreakerMetricsImpl() {
        this(DEFAULT_WINDOW_DURATION_MS, DEFAULT_BUCKET_DURATION_MS);
    }

    /**
     * Create metrics instance with custom window configuration.
     *
     * @param windowDurationMs the window duration in milliseconds
     * @param bucketDurationMs the bucket duration in milliseconds
     */
    public CircuitBreakerMetricsImpl(long windowDurationMs, long bucketDurationMs) {
        this.slidingWindow = new LockFreeSlidingWindowMetrics(windowDurationMs, bucketDurationMs);
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

    /**
     * Reset all metrics to zero.
     */
    @Override
    public void reset() {
        slidingWindow.reset();
    }

    @Override
    public String toString() {
        return "CircuitBreakerMetrics{" + slidingWindow.getSnapshot() + '}';
    }

}
