package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import io.lettuce.TestTags;
import io.lettuce.core.failover.CircuitBreaker.CircuitBreakerConfig;

/**
 * Unit tests for {@link CircuitBreakerConfig.Builder}.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag(TestTags.UNIT_TEST)
class CircuitBreakerConfigBuilderUnitTests {

    @Nested
    @DisplayName("Builder Creation Tests")
    class BuilderCreationTests {

        @Test
        @DisplayName("Should create builder with default values")
        void shouldCreateBuilderWithDefaults() {
            // When: Create config with builder using defaults
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().build();

            // Then: Should have default values
            assertThat(config.getFailureRateThreshold()).isEqualTo(10.0f);
            assertThat(config.getMinimumNumberOfFailures()).isEqualTo(1000);
            assertThat(config.getMetricsWindowSize()).isEqualTo(2);
            assertThat(config.getTrackedExceptions()).isNotEmpty();
        }

        @Test
        @DisplayName("Should set custom failure rate threshold")
        void shouldSetCustomFailureRateThreshold() {
            // When: Build with custom failure rate threshold
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().failureRateThreshold(25.5f).build();

            // Then: Failure rate threshold should be set
            assertThat(config.getFailureRateThreshold()).isEqualTo(25.5f);
            // Other values should be defaults
            assertThat(config.getMinimumNumberOfFailures()).isEqualTo(1000);
        }

        @Test
        @DisplayName("Should set custom minimum number of failures")
        void shouldSetCustomMinimumNumberOfFailures() {
            // When: Build with custom minimum number of failures
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().minimumNumberOfFailures(500).build();

            // Then: Minimum number of failures should be set
            assertThat(config.getMinimumNumberOfFailures()).isEqualTo(500);
            // Other values should be defaults
            assertThat(config.getFailureRateThreshold()).isEqualTo(10.0f);
        }

        @Test
        @DisplayName("Should set custom tracked exceptions")
        void shouldSetCustomTrackedExceptions() {
            // Given: Custom exception set
            Set<Class<? extends Throwable>> customExceptions = new HashSet<>();
            customExceptions.add(RuntimeException.class);
            customExceptions.add(IllegalArgumentException.class);

            // When: Build with custom tracked exceptions
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().trackedExceptions(customExceptions).build();

            // Then: Tracked exceptions should be set
            assertThat(config.getTrackedExceptions()).containsExactlyInAnyOrder(RuntimeException.class,
                    IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should set custom metrics window size")
        void shouldSetCustomMetricsWindowSize() {
            // When: Build with custom metrics window size
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().metricsWindowSize(5).build();

            // Then: Metrics window size should be set
            assertThat(config.getMetricsWindowSize()).isEqualTo(5);
            // Other values should be defaults
            assertThat(config.getFailureRateThreshold()).isEqualTo(10.0f);
        }

        @Test
        @DisplayName("Should set all custom values")
        void shouldSetAllCustomValues() {
            // Given: Custom values
            Set<Class<? extends Throwable>> customExceptions = new HashSet<>();
            customExceptions.add(RuntimeException.class);

            // When: Build with all custom values
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().failureRateThreshold(15.5f)
                    .minimumNumberOfFailures(200).trackedExceptions(customExceptions).metricsWindowSize(3).build();

            // Then: All values should be set
            assertThat(config.getFailureRateThreshold()).isEqualTo(15.5f);
            assertThat(config.getMinimumNumberOfFailures()).isEqualTo(200);
            assertThat(config.getTrackedExceptions()).containsExactly(RuntimeException.class);
            assertThat(config.getMetricsWindowSize()).isEqualTo(3);
        }

    }

    @Nested
    @DisplayName("Builder Edge Cases")
    class BuilderEdgeCaseTests {

        @Test
        @DisplayName("Should support zero failure rate threshold")
        void shouldSupportZeroFailureRateThreshold() {
            // When: Build with zero failure rate threshold
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().failureRateThreshold(0.0f).build();

            // Then: Should accept zero
            assertThat(config.getFailureRateThreshold()).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("Should support zero minimum number of failures")
        void shouldSupportZeroMinimumNumberOfFailures() {
            // When: Build with zero minimum number of failures
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().minimumNumberOfFailures(0).build();

            // Then: Should accept zero
            assertThat(config.getMinimumNumberOfFailures()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should use default tracked exceptions when null is provided")
        void shouldUseDefaultTrackedExceptionsWhenNull() {
            // When: Build with null tracked exceptions
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().trackedExceptions(null).build();

            // Then: Should use default tracked exceptions
            assertThat(config.getTrackedExceptions()).isNotEmpty();
            assertThat(config.getTrackedExceptions()).isEqualTo(CircuitBreakerConfig.DEFAULT.getTrackedExceptions());
        }

    }

}
