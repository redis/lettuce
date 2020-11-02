/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
