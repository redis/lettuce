package io.lettuce.core.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Unit tests for {@link TimeoutProvider}.
 *
 * @author Mark Paluch
 */
class TimeoutProviderUnitTests {

    @Test
    void shouldReturnConfiguredTimeout() {

        TimeoutProvider provider = new TimeoutProvider(() -> TimeoutOptions.enabled(Duration.ofSeconds(10)),
                () -> TimeUnit.SECONDS.toNanos(100));

        long timeout = provider.getTimeoutNs(mock(RedisCommand.class));

        assertThat(timeout).isEqualTo(Duration.ofSeconds(10).toNanos());
    }

    @Test
    void shouldReturnDefaultTimeout() {

        TimeoutProvider provider = new TimeoutProvider(() -> TimeoutOptions.enabled(Duration.ofSeconds(-1)),
                () -> TimeUnit.SECONDS.toNanos(100));

        long timeout = provider.getTimeoutNs(mock(RedisCommand.class));

        assertThat(timeout).isEqualTo(Duration.ofSeconds(100).toNanos());
    }

    @Test
    void shouldReturnNoTimeout() {

        TimeoutProvider provider = new TimeoutProvider(() -> TimeoutOptions.enabled(Duration.ZERO),
                () -> TimeUnit.SECONDS.toNanos(100));

        long timeout = provider.getTimeoutNs(mock(RedisCommand.class));

        assertThat(timeout).isEqualTo(0);
    }
}
