package io.lettuce.core.failover.metrics;

/**
 * Interface for time-based sliding window metrics. Provides lock-free, thread-safe tracking of success and failure counts
 * within a configurable time window.
 *
 * <p>
 * Implementations must be:
 * <ul>
 * <li>Lock-free: No explicit locks, using atomic operations</li>
 * <li>Thread-safe: Safe for concurrent access from multiple threads</li>
 * <li>Efficient: Minimal memory overhead and fast operations</li>
 * <li>Time-based: Automatic expiration of old data outside the window</li>
 * </ul>
 * </p>
 *
 * @author Ali Takavci
 * @since 7.1
 */
interface SlidingWindowMetrics {

    /**
     * Record a successful command execution. Lock-free operation.
     */
    void recordSuccess();

    /**
     * Record a failed command execution. Lock-free operation.
     */
    void recordFailure();

    /**
     * Get a snapshot of the current metrics within the time window. This is a point-in-time view and does not change after
     * being returned. Use the snapshot to access success count, failure count, total count, and failure rate.
     *
     * @return an immutable snapshot of current metrics
     */
    MetricsSnapshot getSnapshot();

}
