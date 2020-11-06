/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.metrics;

import io.lettuce.core.protocol.CommandType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.channel.local.LocalAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.lettuce.core.metrics.MicrometerCommandLatencyCollector.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steven Sheehy
 */
@ExtendWith(MockitoExtension.class)
public class MicrometerCommandLatencyCollectorUnitTests {

    private MicrometerCommandLatencyCollector collector;

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setup() {
        meterRegistry = new SimpleMeterRegistry();
        collector = new MicrometerCommandLatencyCollector(meterRegistry, true);
    }

    @Test
    void verifyMetrics() {
        recordCommand(CommandType.AUTH, 1, 10);
        recordCommand(CommandType.BGSAVE, 100, 500);
        recordCommand(CommandType.BGSAVE, 200, 1000);
        recordCommand(CommandType.BGSAVE, 300, 1500);

        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).timers()).hasSize(2);
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).tag(COMMAND_LABEL, CommandType.BGSAVE.name()).timers())
                .hasSize(1)
                .element(0)
                .extracting(Timer::takeSnapshot)
                .hasFieldOrPropertyWithValue("count", 3L)
                .hasFieldOrPropertyWithValue("max", 300_000_000.0)
                .hasFieldOrPropertyWithValue("mean", 200_000_000.0)
                .hasFieldOrPropertyWithValue("total", 600_000_000.0);

        assertThat(meterRegistry.find(COMPLETION_METRIC).timers()).hasSize(2);
        assertThat(meterRegistry.find(COMPLETION_METRIC).tag(COMMAND_LABEL, CommandType.BGSAVE.name()).timers())
                .hasSize(1)
                .element(0)
                .extracting(Timer::takeSnapshot)
                .hasFieldOrPropertyWithValue("count", 3L)
                .hasFieldOrPropertyWithValue("max", 1500_000_000.0)
                .hasFieldOrPropertyWithValue("mean", 1000_000_000.0)
                .hasFieldOrPropertyWithValue("total", 3000_000_000.0);
    }

    private void recordCommand(CommandType commandType, long firstResponseLatency, long completionLatency) {
        collector.recordCommandLatency(LocalAddress.ANY, LocalAddress.ANY, commandType,
                MILLISECONDS.toNanos(firstResponseLatency), MILLISECONDS.toNanos(completionLatency));
    }
}
