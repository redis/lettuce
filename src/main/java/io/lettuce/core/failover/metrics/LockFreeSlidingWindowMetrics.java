package io.lettuce.core.failover.metrics;

/**
 * Lock-free, thread-safe implementation of sliding window metrics using atomic operations and a ring buffer of time buckets.
 *
 * <p>
 * This implementation uses:
 * <ul>
 * <li>Fixed-size ring buffer of {@link TimeWindowBucket} objects</li>
 * <li>Atomic operations for lock-free updates</li>
 * <li>Time-based bucketing for efficient expiration</li>
 * <li>Lazy expiration: old data is ignored during queries, not explicitly cleaned</li>
 * </ul>
 * </p>
 *
 * <p>
 * Memory overhead: ~160 bytes per instance (2 buckets × 40 bytes each)
 * </p>
 *
 * <p>
 * Thread-safety: All operations are lock-free and thread-safe. Multiple threads can record events and query metrics
 * concurrently without contention.
 * </p>
 *
 * @author Ali Takavci
 * @since 7.1
 */
public class LockFreeSlidingWindowMetrics implements SlidingWindowMetrics {

    /**
     * Fixed bucket duration.
     * This is NOT configurable to ensure consistent behavior and even bucket distribution.
     */
    static final long BUCKET_DURATION_MS = 1_000;

    /**
     * Default window duration: 2 seconds.
     */
    static final int DEFAULT_WINDOW_DURATION_SECONDS = 2;

    /**
     * Window duration in milliseconds.
     */
    private final long windowDurationMs;

    /**
     * Bucket duration in milliseconds.
     */
    private final long bucketDurationMs;

    /**
     * Number of buckets in the ring buffer.
     */
    private final int bucketCount;

    /**
     * Ring buffer of time window buckets. Fixed-size, reused for lock-free operation.
     */
    private final TimeWindowBucket[] ringBuffer;

    /**
     * Clock for getting current time. Allows for testable time-dependent behavior.
     */
    private final Clock clock;

    /**
     * Create a new lock-free sliding window metrics with default configuration (60 seconds window, 200ms buckets).
     */
    public LockFreeSlidingWindowMetrics() {
        this(DEFAULT_WINDOW_DURATION_SECONDS);
    }

    /**
     * Create a new lock-free sliding window metrics with custom window duration.
     *
     * @param windowDurationSeconds the window duration in seconds (must be >= 1)
     * @throws IllegalArgumentException if windowDurationSeconds < 1
     */
    public LockFreeSlidingWindowMetrics(int windowDurationSeconds) {
        this(windowDurationSeconds, Clock.SYSTEM);
    }

    /**
     * Create a new lock-free sliding window metrics with custom window duration and clock.
     *
     * @param windowDurationSeconds the window duration in seconds (must be >= 1)
     * @param clock the clock to use for getting current time
     * @throws IllegalArgumentException if windowDurationSeconds < 1
     */
    public LockFreeSlidingWindowMetrics(int windowDurationSeconds, Clock clock) {
        if (windowDurationSeconds < 1) {
            throw new IllegalArgumentException("Window duration must be at least 1 second, got: " + windowDurationSeconds);
        }

        this.windowDurationMs = windowDurationSeconds * 1000L;
        this.bucketDurationMs = BUCKET_DURATION_MS;
        this.clock = clock;

        // Enforce that window size is evenly divisible by bucket size
        if (windowDurationMs % bucketDurationMs != 0) {
            throw new IllegalArgumentException("Window duration (" + windowDurationMs + "ms) must be evenly divisible by bucket duration ("
                    + bucketDurationMs + "ms). Window: " + windowDurationSeconds + "s");
        }

        this.bucketCount = (int) (windowDurationMs / bucketDurationMs);
        this.ringBuffer = new TimeWindowBucket[bucketCount];

        // Initialize all buckets
        long currentTime = clock.currentTimeMillis();
        for (int i = 0; i < bucketCount; i++) {
            ringBuffer[i] = new TimeWindowBucket(currentTime);
            currentTime += bucketDurationMs;
        }
    }

    @Override
    public void recordSuccess() {
        recordEvent(true);
    }

    @Override
    public void recordFailure() {
        recordEvent(false);
    }

    /**
     * Record an event (success or failure). Lock-free operation.
     *
     * @param isSuccess true for success, false for failure
     */
    private void recordEvent(boolean isSuccess) {
        long currentTimeMs = clock.currentTimeMillis();
        TimeWindowBucket bucket = getCurrentBucket(currentTimeMs);

        if (isSuccess) {
            bucket.incrementSuccessCount();
        } else {
            bucket.incrementFailureCount();
        }
    }

    /**
     * Get the current bucket for the given time, rotating if necessary. Lock-free operation.
     *
     * @param currentTimeMs the current time in milliseconds
     * @return the current bucket
     */
    private TimeWindowBucket getCurrentBucket(long currentTimeMs) {
        // Calculate which bucket this time belongs to
        int bucketIndex = (int) ((currentTimeMs / bucketDurationMs) % bucketCount);

        // Get the bucket
        TimeWindowBucket bucket = ringBuffer[bucketIndex];

        // Check if bucket needs rotation (is stale)
        if (bucket.isStale(currentTimeMs, windowDurationMs)) {
            // Reset and update the bucket
            bucket.reset();
            bucket.setTimestamp(currentTimeMs);
        }

        return bucket;
    }

    @Override
    public MetricsSnapshot getSnapshot() {
        long currentTimeMs = clock.currentTimeMillis();
        long windowStart = currentTimeMs - windowDurationMs;

        long totalSuccess = 0;
        long totalFailure = 0;

        // Iterate through all buckets and sum valid ones
        for (TimeWindowBucket bucket : ringBuffer) {
            // Only include buckets within the current window
            if (bucket.getTimestamp() > windowStart) {
                totalSuccess += bucket.getSuccessCount();
                totalFailure += bucket.getFailureCount();
            }
        }

        return new MetricsSnapshot(totalSuccess, totalFailure, currentTimeMs);
    }

    @Override
    public void reset() {
        for (TimeWindowBucket bucket : ringBuffer) {
            bucket.reset();
            bucket.setTimestamp(clock.currentTimeMillis());
        }
    }

    @Override
    public String toString() {
        MetricsSnapshot snapshot = getSnapshot();
        return "LockFreeSlidingWindowMetrics{" + "window=" + windowDurationMs + "ms, bucket=" + bucketDurationMs
                + "ms, buckets=" + bucketCount + ", " + snapshot + '}';
    }

}
