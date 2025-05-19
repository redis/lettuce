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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.metrics.MicrometerConnectionMonitor.LABEL_EPID;
import static io.lettuce.core.metrics.MicrometerConnectionMonitor.METRIC_RECONNECTION_INACTIVE_TIME;
import static org.assertj.core.api.Assertions.assertThat;

@Tag(UNIT_TEST)
class MicrometerConnectionMonitorUnitTest {

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Test
    void disabled() {

        MicrometerOptions options = MicrometerOptions.disabled();
        MicrometerConnectionMonitor monitor = new MicrometerConnectionMonitor(meterRegistry, options);

        monitor.recordDisconnectedTime("1", 1);
        assertThat(meterRegistry.find(METRIC_RECONNECTION_INACTIVE_TIME).timers()).isEmpty();
        assertThat(meterRegistry.find(METRIC_RECONNECTION_INACTIVE_TIME).timers()).isEmpty();
    }

    @Test
    void tags() {

        Tags tags = Tags.of("app", "foo");
        MicrometerOptions options = MicrometerOptions.builder().tags(tags).build();
        MicrometerConnectionMonitor monitor = new MicrometerConnectionMonitor(meterRegistry, options);

        monitor.recordDisconnectedTime("1", 1);

        assertThat(meterRegistry.find(METRIC_RECONNECTION_INACTIVE_TIME).tags(tags).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_RECONNECTION_INACTIVE_TIME).tag(LABEL_EPID, "1").timers()).hasSize(1);
    }

}
