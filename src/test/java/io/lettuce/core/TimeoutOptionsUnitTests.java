package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.TimeoutOptions.TimeoutSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class TimeoutOptionsUnitTests {

    @Test
    void noTimeoutByDefault() {

        TimeoutOptions timeoutOptions = TimeoutOptions.create();

        assertThat(timeoutOptions.isTimeoutCommands()).isFalse();
        assertThat(timeoutOptions.getSource()).isNull();
    }

    @Test
    void defaultConnectionTimeout() {

        TimeoutOptions timeoutOptions = TimeoutOptions.enabled();

        TimeoutSource source = timeoutOptions.getSource();
        assertThat(timeoutOptions.isTimeoutCommands()).isTrue();
        assertThat(timeoutOptions.isApplyConnectionTimeout()).isTrue();
        assertThat(source.getTimeout(null)).isEqualTo(-1);
    }

    @Test
    void fixedConnectionTimeout() {

        TimeoutOptions timeoutOptions = TimeoutOptions.enabled(Duration.ofMinutes(1));

        TimeoutSource source = timeoutOptions.getSource();
        assertThat(timeoutOptions.isTimeoutCommands()).isTrue();
        assertThat(timeoutOptions.isApplyConnectionTimeout()).isFalse();
        assertThat(source.getTimeout(null)).isEqualTo(TimeUnit.MINUTES.toNanos(1));
    }

    @Test
    void testTimeoutsRelaxingDuringMaintenanceDefault() {
        TimeoutOptions timeoutOptions = TimeoutOptions.create();
        assertThat(timeoutOptions.getRelaxedTimeout()).isEqualTo(TimeoutOptions.DEFAULT_RELAXED_TIMEOUT);
        assertThat(timeoutOptions.getRelaxedTimeout()).isEqualTo(TimeoutOptions.DISABLED_TIMEOUT);
    }

    @Test
    void testTimeoutsRelaxingDuringMaintenanceBuilder() {
        Duration relaxDuration = Duration.ofMillis(500);
        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
                .timeoutCommands()
                .fixedTimeout(Duration.ofMillis(100))
                .timeoutsRelaxingDuringMaintenance(relaxDuration)
                .build();

        assertThat(timeoutOptions.getRelaxedTimeout()).isEqualTo(relaxDuration);
        assertThat(timeoutOptions.isTimeoutCommands()).isTrue();
    }

    @Test
    void testTimeoutsRelaxingDuringMaintenanceBuilderChaining() {
        Duration fixedTimeout = Duration.ofMillis(250);
        Duration relaxDuration = Duration.ofMillis(750);

        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
                .timeoutCommands()
                .fixedTimeout(fixedTimeout)
                .timeoutsRelaxingDuringMaintenance(relaxDuration)
                .build();

        assertThat(timeoutOptions.isTimeoutCommands()).isTrue();
        assertThat(timeoutOptions.isApplyConnectionTimeout()).isFalse();
        assertThat(timeoutOptions.getRelaxedTimeout()).isEqualTo(relaxDuration);
        assertThat(timeoutOptions.getSource().getTimeout(null)).isEqualTo(fixedTimeout.toNanos());
    }

    @Test
    void testTimeoutsRelaxingDuringMaintenanceWithoutTimeoutCommands() {
        // Test that timeout relaxing during maintenance can be set even when timeout commands are disabled
        Duration relaxDuration = Duration.ofMillis(500);
        TimeoutOptions timeoutOptions = TimeoutOptions.builder()
                .timeoutsRelaxingDuringMaintenance(relaxDuration)
                .build();

        assertThat(timeoutOptions.getRelaxedTimeout()).isEqualTo(relaxDuration);
        assertThat(timeoutOptions.isTimeoutCommands()).isFalse();
        assertThat(timeoutOptions.getSource()).isNull();
    }

    @Test
    void testTimeoutsRelaxingDuringMaintenanceNullDuration() {
        // Test that null duration throws IllegalArgumentException
        assertThatThrownBy(() -> TimeoutOptions.builder().timeoutsRelaxingDuringMaintenance(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration must not be null");
    }
}
