package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.*;
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
            assertThat(options.getFailbackCheckInterval()).isEqualTo(120000L);
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
            MultiDbOptions options = MultiDbOptions.builder().failbackCheckInterval(60000L).build();

            // Then: Interval should be set
            assertThat(options.getFailbackCheckInterval()).isEqualTo(60000L);
        }

    }

    @Nested
    @DisplayName("Builder Chaining Tests")
    class BuilderChainingTests {

        @Test
        @DisplayName("Should support method chaining")
        void shouldSupportMethodChaining() {
            // When: Chain builder methods
            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(false).failbackCheckInterval(30000L).build();

            // Then: All settings should be applied
            assertThat(options.isFailbackSupported()).isFalse();
            assertThat(options.getFailbackCheckInterval()).isEqualTo(30000L);
        }

        @Test
        @DisplayName("Should allow overriding values in chain")
        void shouldAllowOverridingValuesInChain() {
            // When: Override values in chain
            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true).failbackSupported(false)
                    .failbackCheckInterval(60000L).failbackCheckInterval(90000L).build();

            // Then: Last values should be used
            assertThat(options.isFailbackSupported()).isFalse();
            assertThat(options.getFailbackCheckInterval()).isEqualTo(90000L);
        }

    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle zero failback check interval")
        void shouldHandleZeroFailbackCheckInterval() {
            // When: Set interval to zero
            MultiDbOptions options = MultiDbOptions.builder().failbackCheckInterval(0L).build();

            // Then: Interval should be zero (though not recommended in practice)
            assertThat(options.getFailbackCheckInterval()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should handle negative failback check interval")
        void shouldHandleNegativeFailbackCheckInterval() {
            // When: Set interval to negative value
            MultiDbOptions options = MultiDbOptions.builder().failbackCheckInterval(-1000L).build();

            // Then: Interval should be negative (though not recommended in practice)
            assertThat(options.getFailbackCheckInterval()).isEqualTo(-1000L);
        }

        @Test
        @DisplayName("Should handle maximum long value for failback check interval")
        void shouldHandleMaxLongValueForFailbackCheckInterval() {
            // When: Set interval to max long value
            MultiDbOptions options = MultiDbOptions.builder().failbackCheckInterval(Long.MAX_VALUE).build();

            // Then: Interval should be max long value
            assertThat(options.getFailbackCheckInterval()).isEqualTo(Long.MAX_VALUE);
        }

    }

    @Nested
    @DisplayName("Grace Period Configuration Tests")
    class GracePeriodConfigurationTests {

        @Test
        @DisplayName("Should have default grace period of 30 seconds (30000ms)")
        void shouldHaveDefaultGracePeriod() {
            // When: Build with defaults
            MultiDbOptions options = MultiDbOptions.builder().build();

            // Then: Grace period should be 30000ms (30 seconds)
            assertThat(options.getGracePeriod()).isEqualTo(30000L);
        }

        @Test
        @DisplayName("Should allow custom grace period in milliseconds")
        void shouldAllowCustomGracePeriod() {
            // When: Set custom grace period to 60 seconds (60000ms)
            MultiDbOptions options = MultiDbOptions.builder().gracePeriod(60000L).build();

            // Then: Grace period should be set
            assertThat(options.getGracePeriod()).isEqualTo(60000L);
        }

        @Test
        @DisplayName("Should allow zero grace period for immediate failback")
        void shouldAllowZeroGracePeriod() {
            // When: Set grace period to zero (disables grace period)
            MultiDbOptions options = MultiDbOptions.builder().gracePeriod(0L).build();

            // Then: Grace period should be zero
            assertThat(options.getGracePeriod()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should support method chaining with grace period")
        void shouldSupportMethodChainingWithGracePeriod() {
            // When: Chain builder methods including grace period
            MultiDbOptions options = MultiDbOptions.builder().failbackSupported(true).gracePeriod(45000L)
                    .failbackCheckInterval(60000L).build();

            // Then: All settings should be applied
            assertThat(options.isFailbackSupported()).isTrue();
            assertThat(options.getGracePeriod()).isEqualTo(45000L);
            assertThat(options.getFailbackCheckInterval()).isEqualTo(60000L);
        }

    }

}
