package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.failover.metrics.LockFreeSlidingWindowMetrics;
import io.lettuce.core.failover.metrics.MetricsSnapshot;

/**
 * Unit tests for lock-free sliding window metrics implementation.
 *
 * @author Ali Takavci
 * @since 7.1
 */
@Tag("unit")
@DisplayName("Lock-Free Sliding Window Metrics")
class LockFreeSlidingWindowMetricsUnitTests {

    private LockFreeSlidingWindowMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new LockFreeSlidingWindowMetrics();
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
    @DisplayName("should reset all metrics")
    void shouldResetAllMetrics() {
        metrics.recordSuccess();
        metrics.recordSuccess();
        metrics.recordFailure();

        metrics.reset();

        MetricsSnapshot snapshot = metrics.getSnapshot();
        assertThat(snapshot.getSuccessCount()).isEqualTo(0);
        assertThat(snapshot.getFailureCount()).isEqualTo(0);
        assertThat(snapshot.getTotalCount()).isEqualTo(0);
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
        assertThatThrownBy(() -> new LockFreeSlidingWindowMetrics(60_000, 500)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bucket duration must be at least");

        assertThatThrownBy(() -> new LockFreeSlidingWindowMetrics(1_000, 2_000)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Window duration must be >= bucket duration");
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

}
