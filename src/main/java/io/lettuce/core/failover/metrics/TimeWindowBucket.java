package io.lettuce.core.failover.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable time window bucket for lock-free sliding window metrics. Each bucket represents a fixed time interval (e.g., 1
 * second) and holds atomic counters for success and failure counts.
 *
 * <p>
 * This class is designed for lock-free, thread-safe operation with minimal memory overhead.
 * </p>
 *
 * @author Ali Takavci
 * @since 7.1
 */
public class TimeWindowBucket {

    /**
     * Timestamp (milliseconds) when this bucket was created or reset.
     */
    private volatile long timestamp;

    /**
     * Atomic counter for successful command executions in this bucket. Lock-free updates.
     */
    private final AtomicLong successCount;

    /**
     * Atomic counter for failed command executions in this bucket. Lock-free updates.
     */
    private final AtomicLong failureCount;

    /**
     * Create a new time window bucket with the given timestamp.
     *
     * @param timestamp the bucket start time in milliseconds
     */
    public TimeWindowBucket(long timestamp) {
        this.timestamp = timestamp;
        this.successCount = new AtomicLong(0);
        this.failureCount = new AtomicLong(0);
    }

    /**
     * Get the timestamp (milliseconds) when this bucket was created or reset.
     *
     * @return the bucket timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Set the timestamp for this bucket. Used during bucket reset/rotation.
     *
     * @param timestamp the new timestamp in milliseconds
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Get the current success count for this bucket. Lock-free read.
     *
     * @return the success count
     */
    public long getSuccessCount() {
        return successCount.get();
    }

    /**
     * Atomically increment the success counter. Lock-free operation.
     *
     * @return the new success count
     */
    public long incrementSuccessCount() {
        return successCount.incrementAndGet();
    }

    /**
     * Get the current failure count for this bucket. Lock-free read.
     *
     * @return the failure count
     */
    public long getFailureCount() {
        return failureCount.get();
    }

    /**
     * Atomically increment the failure counter. Lock-free operation.
     *
     * @return the new failure count
     */
    public long incrementFailureCount() {
        return failureCount.incrementAndGet();
    }

    /**
     * Reset both counters to zero. Used during bucket rotation. Lock-free operation.
     */
    public void reset() {
        successCount.set(0);
        failureCount.set(0);
    }

    /**
     * Get the total count (success + failure) for this bucket.
     *
     * @return the total count
     */
    public long getTotalCount() {
        return successCount.get() + failureCount.get();
    }

    /**
     * Check if this bucket is stale (older than the given age in milliseconds).
     *
     * @param currentTimeMs the current time in milliseconds
     * @param maxAgeMs the maximum age in milliseconds
     * @return true if the bucket is older than maxAgeMs
     */
    public boolean isStale(long currentTimeMs, long maxAgeMs) {
        return (currentTimeMs - timestamp) > maxAgeMs;
    }

    @Override
    public String toString() {
        return "TimeWindowBucket{" + "timestamp=" + timestamp + ", success=" + successCount.get() + ", failure="
                + failureCount.get() + '}';
    }

}
