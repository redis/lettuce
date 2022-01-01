/*
 * Copyright 2011-2022 the original author or authors.
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

import static io.lettuce.core.metrics.MicrometerCommandLatencyRecorder.*;
import static org.assertj.core.api.Assertions.*;

import java.net.SocketAddress;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.protocol.CommandType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.channel.local.LocalAddress;

/**
 * Unit tests for {@link MicrometerCommandLatencyRecorder}.
 *
 * @author Steven Sheehy
 */
@ExtendWith(MockitoExtension.class)
class MicrometerCommandLatencyRecorderUnitTests {

    private static final SocketAddress LOCAL_ADDRESS = new LocalAddress("localhost:54689");

    private static final SocketAddress REMOTE_ADDRESS = new LocalAddress("localhost:6379");

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void verifyMetrics() {

        MicrometerOptions options = MicrometerOptions.create();
        MicrometerCommandLatencyRecorder commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.BGSAVE, 100, 500);
        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.BGSAVE, 200, 1000);
        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.BGSAVE, 300, 1500);

        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).timers()).hasSize(2);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).tag(LABEL_COMMAND, CommandType.BGSAVE.name()).timers()).hasSize(1)
                .element(0).extracting(Timer::takeSnapshot).hasFieldOrPropertyWithValue("count", 3L)
                .hasFieldOrPropertyWithValue("max", 300.0)
                .hasFieldOrPropertyWithValue("total", 600.0);

        assertThat(meterRegistry.find(METRIC_COMPLETION).timers()).hasSize(2);
        assertThat(meterRegistry.find(METRIC_COMPLETION).tag(LABEL_COMMAND, CommandType.BGSAVE.name()).timers()).hasSize(1)
                .element(0).extracting(Timer::takeSnapshot).hasFieldOrPropertyWithValue("count", 3L)
                .hasFieldOrPropertyWithValue("max", 1500.0)
                .hasFieldOrPropertyWithValue("total", 3000.0);
    }

    @Test
    void disabled() {

        MicrometerOptions options = MicrometerOptions.disabled();
        MicrometerCommandLatencyRecorder commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        assertThat(meterRegistry.find(METRIC_COMPLETION).timers()).isEmpty();
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).timers()).isEmpty();
    }

    @Test
    void histogramEnabled() {

        MicrometerOptions options = MicrometerOptions.builder().histogram(true).build();
        MicrometerCommandLatencyRecorder commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 2, 5);

        assertThat(meterRegistry.find(METRIC_COMPLETION).timers()).hasSize(1).element(0).extracting(Timer::takeSnapshot)
                .extracting(HistogramSnapshot::percentileValues, InstanceOfAssertFactories.array(ValueAtPercentile[].class))
                .extracting(ValueAtPercentile::percentile)
                .containsExactlyElementsOf(Arrays.asList(ArrayUtils.toObject(options.targetPercentiles())));
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).timers()).hasSize(1).element(0).extracting(Timer::takeSnapshot)
                .extracting(HistogramSnapshot::percentileValues, InstanceOfAssertFactories.array(ValueAtPercentile[].class))
                .extracting(ValueAtPercentile::percentile)
                .containsExactlyElementsOf(Arrays.asList(ArrayUtils.toObject(options.targetPercentiles())));
    }

    @Test
    void localDistinctionEnabled() {

        MicrometerOptions options = MicrometerOptions.builder().localDistinction(true).build();
        MicrometerCommandLatencyRecorder commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);
        LocalAddress localAddress2 = new LocalAddress("localhost:12345");

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        commandLatencyRecorder.recordCommandLatency(localAddress2, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);

        assertThat(meterRegistry.find(METRIC_COMPLETION).tagKeys(LABEL_LOCAL).timers()).hasSize(2);
        assertThat(meterRegistry.find(METRIC_COMPLETION).tag(LABEL_LOCAL, LOCAL_ADDRESS.toString()).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_COMPLETION).tag(LABEL_LOCAL, localAddress2.toString()).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).tagKeys(LABEL_LOCAL).timers()).hasSize(2);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).tag(LABEL_LOCAL, LOCAL_ADDRESS.toString()).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).tag(LABEL_LOCAL, localAddress2.toString()).timers()).hasSize(1);
    }

    @Test
    void localDistinctionDisabled() {

        MicrometerOptions options = MicrometerOptions.builder().localDistinction(false).build();
        MicrometerCommandLatencyRecorder commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);
        LocalAddress localAddress2 = new LocalAddress("localhost:12345");

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        commandLatencyRecorder.recordCommandLatency(localAddress2, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);

        assertThat(meterRegistry.find(METRIC_COMPLETION).tagKeys(LABEL_LOCAL).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_COMPLETION).tag(LABEL_LOCAL, LocalAddress.ANY.toString()).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).tagKeys(LABEL_LOCAL).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).tag(LABEL_LOCAL, LocalAddress.ANY.toString()).timers()).hasSize(1);
    }

    @Test
    void tags() {

        Tags tags = Tags.of("app", "foo");
        MicrometerOptions options = MicrometerOptions.builder().tags(tags).build();
        MicrometerCommandLatencyRecorder commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);

        assertThat(meterRegistry.find(METRIC_COMPLETION).tags(tags).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_COMPLETION).tag(LABEL_COMMAND, CommandType.AUTH.name()).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_COMPLETION).tag(LABEL_REMOTE, REMOTE_ADDRESS.toString()).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).tags(tags).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).tag(LABEL_COMMAND, CommandType.AUTH.name()).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).tag(LABEL_REMOTE, REMOTE_ADDRESS.toString()).timers()).hasSize(1);
    }

}
