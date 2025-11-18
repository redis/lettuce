package io.lettuce.core.failover.metrics;

/**
 * Interface for circuit breaker metrics tracking successes and failures within a time-based sliding window. Thread-safe and
 * lock-free using atomic operations.
 *
 * <p>
 * This interface defines the contract for tracking metrics over a configurable time period. Old data outside the window is
 * automatically expired and cleaned up.
 * </p>
 *
 * @author Ali Takavci
 * @since 7.1
 */
public interface CircuitBreakerMetrics {

    /**
     * Record a successful command execution. Lock-free operation.
     */
    void recordSuccess();

    /**
     * Record a failed command execution. Lock-free operation.
     */
    void recordFailure();

    /**
     * Get a snapshot of the current metrics within the time window. Use the snapshot to access success count, failure count,
     * total count, and failure rate.
     *
     * @return an immutable snapshot of current metrics
     */
    MetricsSnapshot getSnapshot();

}
