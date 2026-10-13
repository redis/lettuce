/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.metrics.MicrometerOptions.DEFAULT_ENABLED;
import static io.lettuce.core.metrics.MicrometerOptions.DEFAULT_HISTOGRAM;
import static io.lettuce.core.metrics.MicrometerOptions.DEFAULT_LOCAL_DISTINCTION;
import static io.lettuce.core.metrics.MicrometerOptions.DEFAULT_MAX_LATENCY;
import static io.lettuce.core.metrics.MicrometerOptions.DEFAULT_MIN_LATENCY;
import static io.lettuce.core.metrics.MicrometerOptions.DEFAULT_QUEUE_LATENCY;
import static io.lettuce.core.metrics.MicrometerOptions.DEFAULT_TARGET_PERCENTILES;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.micrometer.core.instrument.Tags;

/**
 * Unit tests for {@link MicrometerOptions}.
 *
 * @author Steven Sheehy
 * @author André Tibola
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class MicrometerOptionsUnitTests {

    @Test
    void create() {

        MicrometerOptions options = MicrometerOptions.create();

        assertThat(options.isEnabled()).isEqualTo(DEFAULT_ENABLED);
        assertThat(options.isHistogram()).isEqualTo(DEFAULT_HISTOGRAM);
        assertThat(options.isQueueLatencyEnabled()).isEqualTo(DEFAULT_QUEUE_LATENCY).isFalse();
        assertThat(options.localDistinction()).isEqualTo(DEFAULT_LOCAL_DISTINCTION);
        assertThat(options.maxLatency()).isEqualTo(DEFAULT_MAX_LATENCY);
        assertThat(options.minLatency()).isEqualTo(DEFAULT_MIN_LATENCY);
        assertThat(options.tags()).isEqualTo(Tags.empty());
        assertThat(options.targetPercentiles()).isEqualTo(DEFAULT_TARGET_PERCENTILES);
    }

    @Test
    void disabled() {

        MicrometerOptions options = MicrometerOptions.disabled();

        assertThat(options.isEnabled()).isFalse();
    }

    @Test
    void histogram() {

        MicrometerOptions options = MicrometerOptions.builder().histogram(true).build();

        assertThat(options.isHistogram()).isTrue();
    }

    @Test
    void queueLatency() {
        MicrometerOptions options = MicrometerOptions.builder().queueLatency(true).build();

        assertThat(options.isQueueLatencyEnabled()).isTrue();
        assertThat(options.mutate().histogram(true).build().isQueueLatencyEnabled()).isTrue();
    }

    @Test
    void queueLatencyCanBeDisabledWhenMutating() {
        MicrometerOptions options = MicrometerOptions.builder().queueLatency(true).build();

        MicrometerOptions mutated = options.mutate().queueLatency(false).build();

        assertThat(mutated.isQueueLatencyEnabled()).isFalse();
        assertThat(options.isQueueLatencyEnabled()).isTrue();
    }

    @Test
    void localDistinction() {

        MicrometerOptions options = MicrometerOptions.builder().localDistinction(true).build();

        assertThat(options.localDistinction()).isTrue();
    }

    @Test
    void maxLatency() {

        Duration maxLatency = Duration.ofSeconds(2L);
        MicrometerOptions options = MicrometerOptions.builder().maxLatency(maxLatency).build();

        assertThat(options.maxLatency()).isEqualTo(maxLatency);
    }

    @Test
    void minLatency() {

        Duration minLatency = Duration.ofSeconds(2L);
        MicrometerOptions options = MicrometerOptions.builder().minLatency(minLatency).build();

        assertThat(options.minLatency()).isEqualTo(minLatency);
    }

    @Test
    void targetPercentiles() {

        double[] percentiles = new double[] { 0.1, 0.2, 0.3 };
        MicrometerOptions options = MicrometerOptions.builder().targetPercentiles(percentiles).build();

        assertThat(options.targetPercentiles()).hasSize(3).isEqualTo(percentiles);
    }

    @Test
    void enabledCommands() {
        CommandType[] enabledCommands = { CommandType.HSET, CommandType.HGET, CommandType.EXPIRE };
        MicrometerOptions options = MicrometerOptions.builder().enabledCommands(enabledCommands).build();

        assertThat(options.getMetricsFilter()).accepts(new Command<>(CommandType.HSET, null))
                .rejects(new Command<>(CommandType.SET, null));
    }

}
