package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import io.lettuce.TestTags;
import io.lettuce.core.failover.api.CircuitBreakerConfig;

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
        @DisplayName("Should reject null tracked exceptions")
        void shouldRejectNullTrackedExceptions() {
            // When/Then: Attempting to set null tracked exceptions should throw
            assertThatThrownBy(() -> CircuitBreakerConfig.builder().trackedExceptions(null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Tracked exceptions must not be null");
        }

        @Test
        @DisplayName("Should accept minimum window size of 2 seconds")
        void shouldAcceptMinimumWindowSize() {
            // When: Build with minimum window size of 2 seconds
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().metricsWindowSize(2).build();

            // Then: Should accept 2 seconds
            assertThat(config.getMetricsWindowSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should reject window size less than 2 seconds")
        void shouldRejectWindowSizeLessThan2() {
            // When/Then: Build with window size less than 2 should throw exception
            assertThatThrownBy(() -> CircuitBreakerConfig.builder().metricsWindowSize(1).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Metrics window size must be at least 2 seconds");
        }

    }

    @Nested
    @DisplayName("Tracked Exceptions Modification Tests")
    class TrackedExceptionsModificationTests {

        @Test
        @DisplayName("Should add tracked exceptions to defaults")
        void shouldAddTrackedExceptions() {
            // When: Add custom exceptions to defaults
            CircuitBreakerConfig config = CircuitBreakerConfig.builder()
                    .addTrackedExceptions(RuntimeException.class, IllegalArgumentException.class).build();

            // Then: Should contain both defaults and added exceptions
            assertThat(config.getTrackedExceptions()).contains(RuntimeException.class, IllegalArgumentException.class);
            // Should still contain default exceptions
            assertThat(config.getTrackedExceptions()).isNotEmpty();
        }

        @Test
        @DisplayName("Should remove tracked exceptions from defaults")
        void shouldRemoveTrackedExceptions() {
            // Given: Get a default exception to remove
            Class<? extends Throwable> exceptionToRemove = CircuitBreakerConfig.DEFAULT.getTrackedExceptions().iterator()
                    .next();

            // When: Remove an exception from defaults
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().removeTrackedExceptions(exceptionToRemove).build();

            // Then: Should not contain the removed exception
            assertThat(config.getTrackedExceptions()).doesNotContain(exceptionToRemove);
            // Should still contain other default exceptions
            assertThat(config.getTrackedExceptions()).isNotEmpty();
        }

        @Test
        @DisplayName("Should add and remove tracked exceptions")
        void shouldAddAndRemoveTrackedExceptions() {
            // Given: Get a default exception to remove
            Class<? extends Throwable> exceptionToRemove = CircuitBreakerConfig.DEFAULT.getTrackedExceptions().iterator()
                    .next();

            // When: Add and remove exceptions
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().addTrackedExceptions(RuntimeException.class)
                    .removeTrackedExceptions(exceptionToRemove).build();

            // Then: Should contain added exception
            assertThat(config.getTrackedExceptions()).contains(RuntimeException.class);
            // Should not contain removed exception
            assertThat(config.getTrackedExceptions()).doesNotContain(exceptionToRemove);
        }

        @Test
        @DisplayName("Should replace all tracked exceptions using trackedExceptions()")
        void shouldReplaceAllTrackedExceptions() {
            // Given: Custom exception set
            Set<Class<? extends Throwable>> customExceptions = new HashSet<>();
            customExceptions.add(RuntimeException.class);
            customExceptions.add(IllegalArgumentException.class);

            // When: Replace all tracked exceptions
            CircuitBreakerConfig config = CircuitBreakerConfig.builder().trackedExceptions(customExceptions).build();

            // Then: Should only contain the specified exceptions
            assertThat(config.getTrackedExceptions()).containsExactlyInAnyOrder(RuntimeException.class,
                    IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject null when adding tracked exceptions")
        void shouldRejectNullWhenAdding() {
            // When/Then: Attempting to add null should throw
            assertThatThrownBy(() -> CircuitBreakerConfig.builder().addTrackedExceptions((Class<? extends Throwable>[]) null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Exception classes must not be null");
        }

        @Test
        @DisplayName("Should reject null elements when adding tracked exceptions")
        void shouldRejectNullElementsWhenAdding() {
            // When/Then: Attempting to add null elements should throw
            assertThatThrownBy(
                    () -> CircuitBreakerConfig.builder().addTrackedExceptions(RuntimeException.class, null, IOException.class))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("Exception classes must not contain null elements");
        }

        @Test
        @DisplayName("Should reject empty array when adding tracked exceptions")
        void shouldRejectEmptyArrayWhenAdding() {
            // When/Then: Attempting to add empty array should throw
            assertThatThrownBy(() -> CircuitBreakerConfig.builder().addTrackedExceptions())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Exception classes must contain at least one element");
        }

        @Test
        @DisplayName("Should reject null when removing tracked exceptions")
        void shouldRejectNullWhenRemoving() {
            // When/Then: Attempting to remove null should throw
            assertThatThrownBy(
                    () -> CircuitBreakerConfig.builder().removeTrackedExceptions((Class<? extends Throwable>[]) null))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("Exception classes must not be null");
        }

        @Test
        @DisplayName("Should reject null elements when removing tracked exceptions")
        void shouldRejectNullElementsWhenRemoving() {
            // When/Then: Attempting to remove null elements should throw
            assertThatThrownBy(() -> CircuitBreakerConfig.builder().removeTrackedExceptions(RuntimeException.class, null,
                    IOException.class)).isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("Exception classes must not contain null elements");
        }

        @Test
        @DisplayName("Should reject empty array when removing tracked exceptions")
        void shouldRejectEmptyArrayWhenRemoving() {
            // When/Then: Attempting to remove empty array should throw
            assertThatThrownBy(() -> CircuitBreakerConfig.builder().removeTrackedExceptions())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Exception classes must contain at least one element");
        }

    }

}
