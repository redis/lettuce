package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.TimeoutOptions.TimeoutSource;
import static org.assertj.core.api.Assertions.assertThat;

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
        final long MINUTES = 1;
        final long MINUTE_IN_NANOS = TimeUnit.MINUTES.toNanos(MINUTES);

        TimeoutOptions timeoutOptions = TimeoutOptions.enabled(Duration.ofMinutes(MINUTES));

        TimeoutSource source = timeoutOptions.getSource();
        assertThat(timeoutOptions.isTimeoutCommands()).isTrue();
        assertThat(timeoutOptions.isApplyConnectionTimeout()).isFalse();
        assertThat(source.getTimeout(null)).isEqualTo(MINUTE_IN_NANOS);
    }
}