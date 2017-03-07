/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.metrics;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.lettuce.core.protocol.CommandType;

import io.netty.channel.local.LocalAddress;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultCommandLatencyCollectorTest {

    private DefaultCommandLatencyCollector sut;

    @Test
    public void shutdown() {

        sut = new DefaultCommandLatencyCollector(DefaultCommandLatencyCollectorOptions.create());

        sut.shutdown();

        assertThat(sut.isEnabled()).isFalse();
    }

    @Test
    public void verifyMetrics() {

        sut = new DefaultCommandLatencyCollector(DefaultCommandLatencyCollectorOptions.create());

        setupData();

        Map<CommandLatencyId, CommandMetrics> latencies = sut.retrieveMetrics();
        assertThat(latencies).hasSize(1);

        Map.Entry<CommandLatencyId, CommandMetrics> entry = latencies.entrySet().iterator().next();

        assertThat(entry.getKey().commandType()).isSameAs(CommandType.BGSAVE);

        CommandMetrics metrics = entry.getValue();

        assertThat(metrics.getCount()).isEqualTo(3);
        assertThat(metrics.getCompletion().getMin()).isBetween(990000L, 1100000L);
        assertThat(metrics.getCompletion().getPercentiles()).hasSize(5);

        assertThat(metrics.getFirstResponse().getMin()).isBetween(90000L, 110000L);
        assertThat(metrics.getFirstResponse().getMax()).isBetween(290000L, 310000L);
        assertThat(metrics.getCompletion().getPercentiles()).containsKey(50.0d);

        assertThat(metrics.getFirstResponse().getPercentiles().get(50d)).isLessThanOrEqualTo(
                metrics.getCompletion().getPercentiles().get(50d));

        assertThat(metrics.getTimeUnit()).isEqualTo(MICROSECONDS);

        assertThat(sut.retrieveMetrics()).isEmpty();
    }

    @Test
    public void verifyCummulativeMetrics() {

        sut = new DefaultCommandLatencyCollector(DefaultCommandLatencyCollectorOptions.builder()
                .resetLatenciesAfterEvent(false).build());

        setupData();

        assertThat(sut.retrieveMetrics()).hasSize(1);
        assertThat(sut.retrieveMetrics()).hasSize(1);
    }

    private void setupData() {
        sut.recordCommandLatency(LocalAddress.ANY, LocalAddress.ANY, CommandType.BGSAVE, MILLISECONDS.toNanos(100),
                MILLISECONDS.toNanos(1000));
        sut.recordCommandLatency(LocalAddress.ANY, LocalAddress.ANY, CommandType.BGSAVE, MILLISECONDS.toNanos(200),
                MILLISECONDS.toNanos(1000));
        sut.recordCommandLatency(LocalAddress.ANY, LocalAddress.ANY, CommandType.BGSAVE, MILLISECONDS.toNanos(300),
                MILLISECONDS.toNanos(1000));
    }
}
