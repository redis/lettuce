package io.lettuce.core.failover.metrics;

import java.time.Duration;

/**
 * Controllable clock implementation for testing time-dependent behavior.
 * <p>
 * This clock allows tests to control the passage of time by manually advancing the clock. Supports both {@link Duration} and
 * millisecond-based time advancement.
 * </p>
 * <p>
 * Example usage:
 * 
 * <pre>
 * 
 * {
 *     &#64;code
 *     TestClock clock = new TestClock();
 *     LockFreeSlidingWindowMetrics metrics = new LockFreeSlidingWindowMetrics(2000, 1000, clock);
 *
 *     metrics.recordSuccess();
 *     clock.advance(Duration.ofSeconds(1)); // Advance by 1 second
 *     metrics.recordSuccess();
 *     clock.advance(500); // Advance by 500 milliseconds
 *
 *     MetricsSnapshot snapshot = metrics.getSnapshot();
 * }
 * </pre>
 * </p>
 *
 * @author Ali Takavci
 * @since 7.1
 */
public class TestClock implements Clock {

    private long currentTimeNs;

    /**
     * Create a new test clock starting at time 0.
     */
    public TestClock() {
        this(0L);
    }

    /**
     * Create a new test clock starting at the specified time in nanoseconds.
     *
     * @param currentTime the initial time
     */
    public TestClock(long currentTime) {
        this.currentTimeNs = currentTime;
    }

    @Override
    public long monotonicTime() {
        return currentTimeNs;
    }

    /**
     * Advance the clock by the specified duration.
     *
     * @param duration the duration to advance
     * @return this clock for method chaining
     */
    public TestClock advance(Duration duration) {
        this.currentTimeNs += duration.toNanos();
        return this;
    }

    /**
     * Set the clock to a specific time in nanoseconds.
     *
     * @param time the time to set in nanoseconds
     * @return this clock for method chaining
     */
    public TestClock setTime(long currentTime) {
        this.currentTimeNs = currentTime;
        return this;
    }

}
