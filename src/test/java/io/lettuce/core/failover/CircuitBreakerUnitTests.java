package io.lettuce.core.failover;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.failover.metrics.MetricsSnapshot;

/**
 * Comprehensive unit tests for {@link CircuitBreaker} functionality.
 *
 * @author Ali Takavci
 * @author Ivo Gaydajiev
 * @since 7.1
 */
@Tag(UNIT_TEST)
@DisplayName("CircuitBreaker Unit Tests")
class CircuitBreakerUnitTests {

    @Nested
    @DisplayName("Metrics Evaluation Rules")
    class MetricsEvaluationTests {

        @Test
        @DisplayName("When minimumNumberOfFailures=0, only percentage is considered")
        void shouldConsiderOnlyPercentageWhenMinimumFailuresIsZero() {
            // Given: config with minimumNumberOfFailures=0 and failureRateThreshold=50%
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 0,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config)) {
                // When: record 1 success and 1 failure (50% failure rate, but only 1 failure)
                circuitBreaker.recordSuccess();
                circuitBreaker.recordFailure();

                // Then: circuit should open because percentage threshold is met (minimumNumberOfFailures=0 means ignore count)
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
            }
        }

        @Test
        @DisplayName("When minimumNumberOfFailures=0, circuit opens with any failure meeting percentage")
        void shouldOpenWithSingleFailureWhenMinimumFailuresIsZero() {
            // Given: config with minimumNumberOfFailures=0 and failureRateThreshold=100%
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(100.0f, 0,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config)) {
                // When: record only 1 failure (100% failure rate)
                circuitBreaker.recordFailure();

                // Then: circuit should open
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
            }
        }

        @Test
        @DisplayName("When failureRateThreshold=0.0, only minimumNumberOfFailures is considered")
        void shouldConsiderOnlyCountWhenFailureRateThresholdIsZero() {
            // Given: config with failureRateThreshold=0.0 and minimumNumberOfFailures=5
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(0.0f, 5,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config)) {
                // When: record 100 successes and 5 failures (very low percentage but meets count)
                for (int i = 0; i < 100; i++) {
                    circuitBreaker.recordSuccess();
                }
                for (int i = 0; i < 5; i++) {
                    circuitBreaker.recordFailure();
                }

                // Then: circuit should open because failure count threshold is met (failureRateThreshold=0.0 means ignore
                // percentage)
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
            }
        }

        @Test
        @DisplayName("Both conditions must be met when both thresholds are non-zero")
        void shouldRequireBothConditionsWhenBothThresholdsAreNonZero() {
            // Given: config with failureRateThreshold=50% and minimumNumberOfFailures=10
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 10,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config)) {
                // When: record 5 failures and 5 successes (50% rate but only 5 failures)
                for (int i = 0; i < 5; i++) {
                    circuitBreaker.recordFailure();
                    circuitBreaker.recordSuccess();
                }

                // Then: circuit should remain closed (percentage met but count not met)
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);

                // When: add 5 more failures (now 10 failures, 5 successes = 66.7% rate)
                for (int i = 0; i < 5; i++) {
                    circuitBreaker.recordFailure();
                }

                // Then: circuit should open (both conditions met)
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
            }
        }

        @Test
        @DisplayName("Circuit remains closed when only percentage threshold is met")
        void shouldRemainClosedWhenOnlyPercentageIsMet() {
            // Given: config with failureRateThreshold=50% and minimumNumberOfFailures=10
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 10,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config)) {
                // When: record 9 failures and 1 success (90% rate but only 9 failures)
                for (int i = 0; i < 9; i++) {
                    circuitBreaker.recordFailure();
                }
                circuitBreaker.recordSuccess();

                // Then: circuit should remain closed
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);
            }
        }

        @Test
        @DisplayName("Circuit remains closed when only count threshold is met")
        void shouldRemainClosedWhenOnlyCountIsMet() {
            // Given: config with failureRateThreshold=50% and minimumNumberOfFailures=10
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 10,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config)) {
                // When: record 100 successes and 10 failures (9.1% rate with 10 failures)
                for (int i = 0; i < 100; i++) {
                    circuitBreaker.recordSuccess();
                }
                for (int i = 0; i < 10; i++) {
                    circuitBreaker.recordFailure();
                }

                // Then: circuit should remain closed
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);
            }
        }

    }

    @Nested
    @DisplayName("Basic Circuit Breaker Functionality")
    class BasicFunctionalityTests {

        private CircuitBreakerImpl circuitBreaker;

        @BeforeEach
        void setUp() {
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 5,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            circuitBreaker = new CircuitBreakerImpl(config);
        }

        @Test
        @DisplayName("Should initialize in CLOSED state")
        void shouldInitializeInClosedState() {
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("Should expose metrics")
        void shouldExposeMetrics() {
            assertThat(circuitBreaker.getSnapshot()).isNotNull();
            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getSuccessCount()).isEqualTo(0);
            assertThat(snapshot.getFailureCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should track successes in metrics")
        void shouldTrackSuccesses() {
            circuitBreaker.recordSuccess();
            circuitBreaker.recordSuccess();
            circuitBreaker.recordSuccess();

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getSuccessCount()).isEqualTo(3);
            assertThat(snapshot.getFailureCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should track failures in metrics")
        void shouldTrackFailures() {
            circuitBreaker.recordFailure();
            circuitBreaker.recordFailure();

            MetricsSnapshot snapshot = circuitBreaker.getSnapshot();
            assertThat(snapshot.getSuccessCount()).isEqualTo(0);
            assertThat(snapshot.getFailureCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should transition to OPEN when thresholds are exceeded")
        void shouldTransitionToOpenWhenThresholdsExceeded() {
            // Record 10 failures (100% failure rate, 10 failures)
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure();
            }

            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("Should remain CLOSED when thresholds are not exceeded")
        void shouldRemainClosedWhenThresholdsNotExceeded() {
            // Record 4 failures (below minimum count)
            for (int i = 0; i < 4; i++) {
                circuitBreaker.recordFailure();
            }

            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("Should not transition when already in target state")
        void shouldNotTransitionWhenAlreadyInTargetState() {
            // First transition to OPEN
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure();
            }
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Evaluate again - should remain OPEN without triggering another transition
            circuitBreaker.evaluateMetrics();
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("Should reset metrics on forceful state transition")
        void shouldResetMetricsOnStateTransition() {
            // Given: some recorded events
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);
            circuitBreaker.recordSuccess();
            circuitBreaker.recordFailure();

            // When: force transition to OPEN
            circuitBreaker.transitionTo(CircuitBreaker.State.OPEN);
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Then: metrics should be reset
            assertThat(circuitBreaker.getSnapshot().getSuccessCount()).isEqualTo(0);
            assertThat(circuitBreaker.getSnapshot().getFailureCount()).isEqualTo(0);

            // When: record some more successes and failures
            circuitBreaker.recordSuccess();
            circuitBreaker.recordFailure();

            // Then: metrics should reflect the new events
            assertThat(circuitBreaker.getSnapshot().getSuccessCount()).isEqualTo(1);
            assertThat(circuitBreaker.getSnapshot().getFailureCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should reset metrics on automatic state transition")
        void shouldResetMetricsOnAutomaticStateTransition() {
            // Given: some recorded events
            for (int i = 0; i < 4; i++) {
                circuitBreaker.recordSuccess();
                circuitBreaker.recordFailure();
            }
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);

            // When: record 1 more failures to meet the minimumNumberOfFailures threshold
            circuitBreaker.recordFailure();
            assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);

            // Then: metrics should be reset
            assertThat(circuitBreaker.getSnapshot().getSuccessCount()).isEqualTo(0);
            assertThat(circuitBreaker.getSnapshot().getFailureCount()).isEqualTo(0);
        }

    }

    @Nested
    @DisplayName("Exception Tracking")
    class ExceptionTrackingTests {

        private CircuitBreakerImpl circuitBreaker;

        @BeforeEach
        void setUp() {
            circuitBreaker = new CircuitBreakerImpl(CircuitBreaker.CircuitBreakerConfig.DEFAULT);
        }

        @Test
        @DisplayName("Should track RedisConnectionException")
        void shouldTrackRedisConnectionException() {
            assertThat(circuitBreaker.isCircuitBreakerTrackedException(new RedisConnectionException("test"))).isTrue();
        }

        @Test
        @DisplayName("Should track IOException")
        void shouldTrackIOException() {
            assertThat(circuitBreaker.isCircuitBreakerTrackedException(new IOException("test"))).isTrue();
        }

        @Test
        @DisplayName("Should track ConnectException")
        void shouldTrackConnectException() {
            assertThat(circuitBreaker.isCircuitBreakerTrackedException(new ConnectException("test"))).isTrue();
        }

        @Test
        @DisplayName("Should track RedisCommandTimeoutException")
        void shouldTrackRedisCommandTimeoutException() {
            assertThat(circuitBreaker.isCircuitBreakerTrackedException(new RedisCommandTimeoutException("test"))).isTrue();
        }

        @Test
        @DisplayName("Should track TimeoutException")
        void shouldTrackTimeoutException() {
            assertThat(circuitBreaker.isCircuitBreakerTrackedException(new TimeoutException("test"))).isTrue();
        }

        @Test
        @DisplayName("Should not track unrelated exceptions")
        void shouldNotTrackUnrelatedException() {
            assertThat(circuitBreaker.isCircuitBreakerTrackedException(new IllegalArgumentException("test"))).isFalse();
            assertThat(circuitBreaker.isCircuitBreakerTrackedException(new NullPointerException("test"))).isFalse();
            assertThat(circuitBreaker.isCircuitBreakerTrackedException(new RuntimeException("test"))).isFalse();
        }

        @Test
        @DisplayName("Should support custom tracked exceptions")
        void shouldSupportCustomTrackedException() {
            Set<Class<? extends Throwable>> customExceptions = new HashSet<>();
            customExceptions.add(IllegalStateException.class);
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 5, customExceptions);
            try (CircuitBreakerImpl customCircuitBreaker = new CircuitBreakerImpl(config)) {
                assertThat(customCircuitBreaker.isCircuitBreakerTrackedException(new IllegalStateException("test"))).isTrue();
                assertThat(customCircuitBreaker.isCircuitBreakerTrackedException(new IOException("test"))).isFalse();
            }
        }

        @Test
        @DisplayName("Should track exception subclasses")
        void shouldTrackExceptionSubclasses() {
            // ConnectException is a subclass of IOException
            assertThat(circuitBreaker.isCircuitBreakerTrackedException(new ConnectException("test"))).isTrue();
        }

    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should use default configuration values")
        void shouldUseDefaultConfiguration() {
            CircuitBreaker.CircuitBreakerConfig config = CircuitBreaker.CircuitBreakerConfig.DEFAULT;

            assertThat(config.getFailureRateThreshold()).isEqualTo(10.0f);
            assertThat(config.getMinimumNumberOfFailures()).isEqualTo(1000);
            assertThat(config.getTrackedExceptions()).isNotEmpty();
        }

        @Test
        @DisplayName("Should accept custom configuration")
        void shouldAcceptCustomConfiguration() {
            Set<Class<? extends Throwable>> customExceptions = new HashSet<>();
            customExceptions.add(RuntimeException.class);

            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(25.5f, 100, customExceptions);

            assertThat(config.getFailureRateThreshold()).isEqualTo(25.5f);
            assertThat(config.getMinimumNumberOfFailures()).isEqualTo(100);
            assertThat(config.getTrackedExceptions()).containsExactly(RuntimeException.class);
        }

        @Test
        @DisplayName("Should support zero failure rate threshold")
        void shouldSupportZeroFailureRateThreshold() {
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(0.0f, 10,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());

            assertThat(config.getFailureRateThreshold()).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("Should support zero minimum number of failures")
        void shouldSupportZeroMinimumNumberOfFailures() {
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 0,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());

            assertThat(config.getMinimumNumberOfFailures()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should support high failure rate threshold")
        void shouldSupportHighFailureRateThreshold() {
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(99.9f, 5,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());

            assertThat(config.getFailureRateThreshold()).isEqualTo(99.9f);
        }

    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle exact threshold values")
        void shouldHandleExactThresholdValues() {
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 10,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config)) {
                // Exactly 50% failure rate with exactly 10 failures
                for (int i = 0; i < 10; i++) {
                    circuitBreaker.recordFailure();
                    circuitBreaker.recordSuccess();
                }

                // Should open because both thresholds are met (>= comparison)
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
            }
        }

        @Test
        @DisplayName("Should handle zero total count")
        void shouldHandleZeroTotalCount() {
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 0,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(config)) {
                // No metrics recorded
                circuitBreaker.evaluateMetrics();

                // Should remain closed (0% failure rate, 0 failures)
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);
            }
        }

        @Test
        @DisplayName("Should handle 100% failure rate")
        void shouldHandle100PercentFailureRate() {
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 5,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config)) {
                // 100% failure rate
                for (int i = 0; i < 10; i++) {
                    circuitBreaker.recordFailure();
                }

                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
                assertThat(circuitBreaker.getSnapshot().getFailureRate()).isEqualTo(100.0f);
            }
        }

        @Test
        @DisplayName("Should handle 0% failure rate")
        void shouldHandle0PercentFailureRate() {
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(50.0f, 5,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(config)) {
                // 0% failure rate
                for (int i = 0; i < 100; i++) {
                    circuitBreaker.recordSuccess();
                }
                circuitBreaker.evaluateMetrics();

                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.CLOSED);
                assertThat(circuitBreaker.getSnapshot().getFailureRate()).isEqualTo(0.0f);
            }
        }

        @Test
        @DisplayName("Should handle large number of operations within window")
        void shouldHandleLargeNumberOfOperations() {
            CircuitBreaker.CircuitBreakerConfig config = new CircuitBreaker.CircuitBreakerConfig(0.99f, 1000,
                    CircuitBreaker.CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
            try (CircuitBreaker circuitBreaker = new CircuitBreakerImpl(config)) {
                // Record 100,000 successes and 1,000 failures (0.99% failure rate and meets count threshold)
                // Keep the number reasonable to stay within the sliding window
                for (int i = 0; i < 100_000; i++) {
                    circuitBreaker.recordSuccess();
                }
                for (int i = 0; i < 1_000; i++) {
                    circuitBreaker.recordFailure();
                }

                // Should open because failure count >= 1000 and failure rate >= 1.0%
                assertThat(circuitBreaker.getCurrentState()).isEqualTo(CircuitBreaker.State.OPEN);
                // After state transition, metrics are reset to fresh instance
                assertThat(circuitBreaker.getSnapshot().getFailureCount()).isEqualTo(0);
                assertThat(circuitBreaker.getSnapshot().getSuccessCount()).isEqualTo(0);
            }
        }

    }

}
