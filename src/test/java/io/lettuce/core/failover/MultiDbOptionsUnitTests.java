package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.awaitility.Durations;

import static io.lettuce.TestTags.UNIT_TEST;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

/**
 * Unit tests for {@link MultiDbOptions}.
 * <p>
 * Tests the builder pattern, default values, and configuration options for multi-database client behavior.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag(UNIT_TEST)
class MultiDbOptionsUnitTests {

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have failback enabled by default.Should have default failback check interval of 120 seconds.")
        void shouldHaveFailbackEnabledByDefault() {
            // When: Build with defaults
            MultiDbOptions options = MultiDbOptions.builder().build();

            // Then: Failback should be enabled
            assertThat(options.isFailbackSupported()).isTrue();

            // Then: Check interval should be 120000ms (120 seconds)
            assertThat(options.getFailbackCheckInterval()).isEqualTo(Durations.TWO_MINUTES);
        }

    }

    @Nested
    @DisplayName("Failback Configuration Tests")
    class FailbackConfigurationTests {

        @Test
        @DisplayName("Should allow disabling failback")
        void shouldAllowDisablingFailback() {
            // When: Disable failback
            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(false).build();

            // Then: Failback should be disabled
            assertThat(options.isFailbackSupported()).isFalse();
        }

        @Test
        @DisplayName("Should allow enabling failback explicitly")
        void shouldAllowEnablingFailback() {
            // When: Enable failback explicitly
            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true).build();

            // Then: Failback should be enabled
            assertThat(options.isFailbackSupported()).isTrue();
        }

        @Test
        @DisplayName("Should allow custom failback check interval")
        void shouldAllowCustomFailbackCheckInterval() {
            // When: Set custom interval
            MultiDbOptions options = MultiDbOptions.builder().failbackCheckInterval(Durations.ONE_MINUTE).build();

            // Then: Interval should be set
            assertThat(options.getFailbackCheckInterval()).isEqualTo(Durations.ONE_MINUTE);
        }

    }

    @Nested
    @DisplayName("Builder Chaining Tests")
    class BuilderChainingTests {

        @Test
        @DisplayName("Should support method chaining")
        void shouldSupportMethodChaining() {
            // When: Chain builder methods
            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(false)
                    .failbackCheckInterval(Duration.ofSeconds(30)).build();

            // Then: All settings should be applied
            assertThat(options.isFailbackSupported()).isFalse();
            assertThat(options.getFailbackCheckInterval()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        @DisplayName("Should allow overriding values in chain")
        void shouldAllowOverridingValuesInChain() {
            // When: Override values in chain
            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true).failbackSupported(false)
                    .failbackCheckInterval(Durations.ONE_MINUTE).failbackCheckInterval(Duration.ofSeconds(90)).build();

            // Then: Last values should be used
            assertThat(options.isFailbackSupported()).isFalse();
            assertThat(options.getFailbackCheckInterval()).isEqualTo(Duration.ofSeconds(90));
        }

    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle zero failback check interval")
        void shouldHandleZeroFailbackCheckInterval() {
            // should throw exception
            assertThatThrownBy(() -> MultiDbOptions.builder().failbackCheckInterval(Duration.ZERO).build())
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must be greater than 0");
        }

        @Test
        @DisplayName("Should handle negative failback check interval")
        void shouldHandleNegativeFailbackCheckInterval() {
            // should throw exception
            assertThatThrownBy(() -> MultiDbOptions.builder().failbackCheckInterval(Duration.ofMillis(-1000L)).build())
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must be greater than 0");
        }

        @Test
        @DisplayName("Should handle maximum long value for failback check interval")
        void shouldHandleMaxLongValueForFailbackCheckInterval() {
            // When: Set interval to max long value
            MultiDbOptions options = MultiDbOptions.builder().failbackCheckInterval(Duration.ofMillis(Long.MAX_VALUE)).build();

            // Then: Interval should be max long value
            assertThat(options.getFailbackCheckInterval()).isEqualTo(Duration.ofMillis(Long.MAX_VALUE));
        }

    }

}
