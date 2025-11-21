package io.lettuce.core.failover.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

/**
 * Unit tests for lock-free sliding window metrics implementation.
 *
 * @author Ali Takavci
 * @since 7.1
 */
@Tag("unit")
@DisplayName("Lock-Free Sliding Window Metrics")
class LockFreeSlidingTimeWindowMetricsUnitTests {

    private static final Duration BUCKET_SIZE_DURATION = Duration.ofSeconds(1);

    private static final int BUCKET_SIZE = toSeconds(BUCKET_SIZE_DURATION);

    private LockFreeSlidingTimeWindowMetrics metrics;

    TestClock clock;

    @BeforeEach
    void setUp() {
        clock = new TestClock();
        metrics = new LockFreeSlidingTimeWindowMetrics(LockFreeSlidingTimeWindowMetrics.DEFAULT_WINDOW_DURATION_SECONDS, clock);
    }

    @Test
    @DisplayName("should initialize with default configuration")
    void shouldInitializeWithDefaults() {
        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(0);
        assertThat(snapshot.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should record successful events")
    void shouldRecordSuccessfulEvents() {
        metrics.recordSuccess();
        metrics.recordSuccess();
        metrics.recordSuccess();

        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(3);
        assertThat(snapshot.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should record failed events")
    void shouldRecordFailedEvents() {
        metrics.recordFailure();
        metrics.recordFailure();

        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(0);
        assertThat(snapshot.getFailureCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should track mixed success and failure events")
    void shouldTrackMixedEvents() {
        metrics.recordSuccess();
        metrics.recordSuccess();
        metrics.recordFailure();
        metrics.recordSuccess();
        metrics.recordFailure();

        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(3);
        assertThat(snapshot.getFailureCount()).isEqualTo(2);
        assertThat(snapshot.getTotalCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("should calculate failure rate correctly")
    void shouldCalculateFailureRate() {
        metrics.recordSuccess();
        metrics.recordSuccess();
        metrics.recordFailure();

        MetricsSnapshot snapshot = metrics.getSnapshot();
        double failureRate = snapshot.getFailureRate();
        assertThat(failureRate).isCloseTo(33.33, offset(0.1));
    }

    @Test
    @DisplayName("should return zero failure rate when no events")
    void shouldReturnZeroFailureRateWhenNoEvents() {
        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getFailureRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("should return 100% failure rate when all failures")
    void shouldReturn100PercentFailureRateWhenAllFailures() {
        metrics.recordFailure();
        metrics.recordFailure();

        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getFailureRate()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("should provide immutable snapshot")
    void shouldProvideImmutableSnapshot() {
        metrics.recordSuccess();
        metrics.recordSuccess();
        metrics.recordFailure();

        MetricsSnapshot snapshot1 = metrics.getSnapshot();
        assertThat(snapshot1.getSuccessCount()).isEqualTo(2);
        assertThat(snapshot1.getFailureCount()).isEqualTo(1);

        // Record more events
        metrics.recordSuccess();

        // Snapshot should not change
        assertThat(snapshot1.getSuccessCount()).isEqualTo(2);
        assertThat(snapshot1.getFailureCount()).isEqualTo(1);

        // New snapshot should reflect new state
        MetricsSnapshot snapshot2 = metrics.getSnapshot();
        assertThat(snapshot2.getSuccessCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("should validate configuration")
    void shouldValidateConfiguration() {
        // Window duration must be at least 1 second
        assertThatThrownBy(() -> new LockFreeSlidingTimeWindowMetrics(0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Window duration must be at least 1 second");

        assertThatThrownBy(() -> new LockFreeSlidingTimeWindowMetrics(-5)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Window duration must be at least 1 second");
    }

    @Test
    @DisplayName("should handle high throughput")
    void shouldHandleHighThroughput() {
        int eventCount = 100_000;

        for (int i = 0; i < eventCount; i++) {
            if (i % 2 == 0) {
                metrics.recordSuccess();
            } else {
                metrics.recordFailure();
            }
        }

        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getTotalCount()).isEqualTo(eventCount);
        assertThat(snapshot.getSuccessCount()).isEqualTo(eventCount / 2);
        assertThat(snapshot.getFailureCount()).isEqualTo(eventCount / 2);
    }

    @Test
    @DisplayName("should be thread-safe")
    void shouldBeThreadSafe() throws InterruptedException {
        int threadCount = 10;
        int eventsPerThread = 10_000;

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < eventsPerThread; j++) {
                    if ((threadId + j) % 2 == 0) {
                        metrics.recordSuccess();
                    } else {
                        metrics.recordFailure();
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getTotalCount()).isEqualTo(threadCount * eventsPerThread);
    }

    @Test
    @DisplayName("should track events on window with single bucket")
    void shouldTrackEventsOnWindowWithSingleBucket() {
        int windowSize = BUCKET_SIZE * 1;
        LockFreeSlidingTimeWindowMetrics metrics = new LockFreeSlidingTimeWindowMetrics(windowSize, clock);
        metrics.recordSuccess();
        metrics.recordFailure();
        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(1);
        assertThat(snapshot.getFailureCount()).isEqualTo(1);

        // advance one bucket
        clock.advance(BUCKET_SIZE_DURATION);
        assertThat(metrics.getSnapshot().getSuccessCount()).isEqualTo(0);
        assertThat(metrics.getSnapshot().getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("should track events on bucket boundary")
    void shouldTrackEventsAcrossMultipleBuckets() {

        // 2 second window with 1s buckets = 2 buckets
        int windowSize = BUCKET_SIZE * 2;
        LockFreeSlidingTimeWindowMetrics metrics = new LockFreeSlidingTimeWindowMetrics(windowSize, clock);

        // Record events at specific times
        // bucket 0
        metrics.recordSuccess();
        metrics.recordFailure();
        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(1);
        assertThat(snapshot.getFailureCount()).isEqualTo(1);

        // bucket 0
        clock.advance(BUCKET_SIZE_DURATION.minus(1, ChronoUnit.MILLIS));
        metrics.recordSuccess();
        metrics.recordFailure();
        snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(2);
        assertThat(snapshot.getFailureCount()).isEqualTo(2);

        // bucket 1
        clock.advance(Duration.of(1, ChronoUnit.MILLIS));
        metrics.recordSuccess();
        metrics.recordFailure();
        snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(3);
        assertThat(snapshot.getFailureCount()).isEqualTo(3);

        // bucket 2 // drop bucket 0 from window
        clock.advance(BUCKET_SIZE_DURATION);
        snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(1);
        assertThat(snapshot.getFailureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("should track events across multiple buckets")
    void shouldAggregateEventsFromMultipleBuckets() {

        int windowSize = BUCKET_SIZE * 2;
        TestClock clock = new TestClock(0);
        LockFreeSlidingTimeWindowMetrics metrics = new LockFreeSlidingTimeWindowMetrics(windowSize, clock);

        // bucket 0
        clock.advance(Duration.ZERO.plusMillis(1));
        metrics.recordSuccess();
        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(1);

        // bucket 1
        clock.advance(BUCKET_SIZE_DURATION);
        metrics.recordSuccess();
        MetricsSnapshot snapshot2 = metrics.getSnapshot();
        assertThat(snapshot2.getSuccessCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should aggregate events from non-sequential buckets")
    void shouldAggregateEventsFromNonSequentialBuckets() {

        int windowSize = BUCKET_SIZE * 3;
        TestClock clock = new TestClock(0);
        LockFreeSlidingTimeWindowMetrics metrics = new LockFreeSlidingTimeWindowMetrics(windowSize, clock);

        // bucket 0
        clock.advance(Duration.ZERO.plusMillis(1));
        metrics.recordSuccess();
        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(1);

        // bucket 1. - no events
        clock.advance(BUCKET_SIZE_DURATION);
        MetricsSnapshot snapshot2 = metrics.getSnapshot();
        assertThat(snapshot2.getSuccessCount()).isEqualTo(1);

        // bucket 2
        clock.advance(BUCKET_SIZE_DURATION);
        metrics.recordSuccess();
        MetricsSnapshot snapshot3 = metrics.getSnapshot();
        assertThat(snapshot3.getSuccessCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should return correct event count after window advances")
    void shouldAggregateEventsAfterBucketRotation() {

        int windowSize = BUCKET_SIZE * 3;
        TestClock clock = new TestClock(0);
        LockFreeSlidingTimeWindowMetrics metrics = new LockFreeSlidingTimeWindowMetrics(windowSize, clock);

        // bucket 0
        clock.advance(Duration.ZERO.plusMillis(1));
        metrics.recordSuccess();
        for (int i = 1; i <= 2; i++) {
            // one success per bucket
            clock.advance(BUCKET_SIZE_DURATION);
            metrics.recordSuccess();
            assertThat(metrics.getSnapshot().getSuccessCount()).isEqualTo(i + 1);
        }
        // drop bucket 0 from moving window
        clock.advance(BUCKET_SIZE_DURATION);
        assertThat(metrics.getSnapshot().getSuccessCount()).isEqualTo(2);
    }

    static private int toSeconds(Duration seconds) {
        return Math.toIntExact(seconds.getSeconds());
    }

}
