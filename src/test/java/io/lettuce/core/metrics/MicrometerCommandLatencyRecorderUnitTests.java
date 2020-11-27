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

import com.google.common.primitives.Doubles;
import io.lettuce.core.protocol.CommandType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.channel.local.LocalAddress;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketAddress;

import static io.lettuce.core.metrics.MicrometerCommandLatencyRecorder.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steven Sheehy
 */
@ExtendWith(MockitoExtension.class)
class MicrometerCommandLatencyRecorderUnitTests {

    private static final SocketAddress LOCAL_ADDRESS = new LocalAddress("localhost:54689");

    private static final SocketAddress REMOTE_ADDRESS = new LocalAddress("localhost:6379");

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private MicrometerCommandLatencyRecorder commandLatencyRecorder;

    @Test
    void verifyMetrics() {
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.create();
        commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.BGSAVE, 100, 500);
        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.BGSAVE, 200, 1000);
        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.BGSAVE, 300, 1500);

        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).timers()).hasSize(2);
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).tag(LABEL_COMMAND, CommandType.BGSAVE.name()).timers())
                .hasSize(1)
                .element(0)
                .extracting(Timer::takeSnapshot)
                .hasFieldOrPropertyWithValue("count", 3L)
                .hasFieldOrPropertyWithValue("max", 300.0)
                .hasFieldOrPropertyWithValue("mean", 200.0)
                .hasFieldOrPropertyWithValue("total", 600.0);

        assertThat(meterRegistry.find(COMPLETION_METRIC).timers()).hasSize(2);
        assertThat(meterRegistry.find(COMPLETION_METRIC).tag(LABEL_COMMAND, CommandType.BGSAVE.name()).timers())
                .hasSize(1)
                .element(0)
                .extracting(Timer::takeSnapshot)
                .hasFieldOrPropertyWithValue("count", 3L)
                .hasFieldOrPropertyWithValue("max", 1500.0)
                .hasFieldOrPropertyWithValue("mean", 1000.0)
                .hasFieldOrPropertyWithValue("total", 3000.0);
    }

    @Test
    void disabled() {
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.disabled();
        commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        assertThat(meterRegistry.find(COMPLETION_METRIC).timers()).isEmpty();
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).timers()).isEmpty();
    }

    @Test
    void histogramEnabled() {
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.builder()
                .histogram()
                .build();
        commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 2, 5);

        assertThat(meterRegistry.find(COMPLETION_METRIC).timers())
                .hasSize(1)
                .element(0)
                .extracting(Timer::takeSnapshot)
                .extracting(HistogramSnapshot::percentileValues, InstanceOfAssertFactories.array(ValueAtPercentile[].class))
                .extracting(ValueAtPercentile::percentile)
                .containsExactlyElementsOf(Doubles.asList(options.targetPercentiles()));
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).timers())
                .hasSize(1)
                .element(0)
                .extracting(Timer::takeSnapshot)
                .extracting(HistogramSnapshot::percentileValues, InstanceOfAssertFactories.array(ValueAtPercentile[].class))
                .extracting(ValueAtPercentile::percentile)
                .containsExactlyElementsOf(Doubles.asList(options.targetPercentiles()));
    }

    @Test
    void localDistinctionEnabled() {
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.builder()
                .localDistinction(true)
                .build();
        commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);
        LocalAddress localAddress2 = new LocalAddress("localhost:12345");

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        commandLatencyRecorder.recordCommandLatency(localAddress2, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);

        assertThat(meterRegistry.find(COMPLETION_METRIC).tagKeys(LABEL_LOCAL).timers()).hasSize(2);
        assertThat(meterRegistry.find(COMPLETION_METRIC).tag(LABEL_LOCAL, LOCAL_ADDRESS.toString()).timers()).hasSize(1);
        assertThat(meterRegistry.find(COMPLETION_METRIC).tag(LABEL_LOCAL, localAddress2.toString()).timers()).hasSize(1);
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).tagKeys(LABEL_LOCAL).timers()).hasSize(2);
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).tag(LABEL_LOCAL, LOCAL_ADDRESS.toString()).timers()).hasSize(1);
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).tag(LABEL_LOCAL, localAddress2.toString()).timers()).hasSize(1);
    }

    @Test
    void localDistinctionDisabled() {
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.builder()
                .localDistinction(false)
                .build();
        commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);
        LocalAddress localAddress2 = new LocalAddress("localhost:12345");

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        commandLatencyRecorder.recordCommandLatency(localAddress2, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);

        assertThat(meterRegistry.find(COMPLETION_METRIC).tagKeys(LABEL_LOCAL).timers()).hasSize(1);
        assertThat(meterRegistry.find(COMPLETION_METRIC).tag(LABEL_LOCAL, LocalAddress.ANY.toString()).timers()).hasSize(1);
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).tagKeys(LABEL_LOCAL).timers()).hasSize(1);
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).tag(LABEL_LOCAL, LocalAddress.ANY.toString()).timers()).hasSize(1);
    }

    @Test
    void tags() {
        Tags tags = Tags.of("app", "foo");
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.builder()
                .tags(tags)
                .build();
        commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);

        assertThat(meterRegistry.find(COMPLETION_METRIC).tags(tags).timers()).hasSize(1);
        assertThat(meterRegistry.find(COMPLETION_METRIC).tag(LABEL_COMMAND, CommandType.AUTH.name()).timers()).hasSize(1);
        assertThat(meterRegistry.find(COMPLETION_METRIC).tag(LABEL_REMOTE, REMOTE_ADDRESS.toString()).timers()).hasSize(1);
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).tags(tags).timers()).hasSize(1);
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).tag(LABEL_COMMAND, CommandType.AUTH.name()).timers()).hasSize(1);
        assertThat(meterRegistry.find(FIRST_RESPONSE_METRIC).tag(LABEL_REMOTE, REMOTE_ADDRESS.toString()).timers()).hasSize(1);
    }
}
