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

import io.micrometer.core.instrument.Tags;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static io.lettuce.core.metrics.MicrometerCommandLatencyCollectorOptions.*;

/**
 * @author Steven Sheehy
 */
class MicrometerCommandLatencyCollectorOptionsUnitTests {

    @Test
    void create() {
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.create();

        assertThat(options.isEnabled()).isEqualTo(DEFAULT_ENABLED);
        assertThat(options.isHistogram()).isEqualTo(DEFAULT_HISTOGRAM);
        assertThat(options.localDistinction()).isEqualTo(DEFAULT_LOCAL_DISTINCTION);
        assertThat(options.maxLatency()).isEqualTo(DEFAULT_MAX_LATENCY);
        assertThat(options.minLatency()).isEqualTo(DEFAULT_MIN_LATENCY);
        assertThat(options.tags()).isEqualTo(Tags.empty());
        assertThat(options.targetPercentiles()).isEqualTo(DEFAULT_TARGET_PERCENTILES);
    }

    @Test
    void disabled() {
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.disabled();

        assertThat(options.isEnabled()).isFalse();
    }

    @Test
    void histogram() {
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.builder().histogram().build();

        assertThat(options.isHistogram()).isTrue();
    }

    @Test
    void localDistinction() {
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.builder()
                .localDistinction(true)
                .build();

        assertThat(options.localDistinction()).isTrue();
    }

    @Test
    void maxLatency() {
        Duration maxLatency = Duration.ofSeconds(2L);
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.builder()
                .maxLatency(maxLatency)
                .build();

        assertThat(options.maxLatency()).isEqualTo(maxLatency);
    }

    @Test
    void minLatency() {
        Duration minLatency = Duration.ofSeconds(2L);
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.builder()
                .minLatency(minLatency)
                .build();

        assertThat(options.minLatency()).isEqualTo(minLatency);
    }

    @Test
    void targetPercentiles() {
        double[] percentiles = new double[] { 0.1, 0.2, 0.3 };
        MicrometerCommandLatencyCollectorOptions options = MicrometerCommandLatencyCollectorOptions.builder()
                .targetPercentiles(percentiles).build();

        assertThat(options.targetPercentiles()).hasSize(3).isEqualTo(percentiles);
    }
}
