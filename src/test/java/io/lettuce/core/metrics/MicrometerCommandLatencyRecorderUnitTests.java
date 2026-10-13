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
import static io.lettuce.core.metrics.MicrometerCommandLatencyRecorder.LABEL_COMMAND;
import static io.lettuce.core.metrics.MicrometerCommandLatencyRecorder.LABEL_LOCAL;
import static io.lettuce.core.metrics.MicrometerCommandLatencyRecorder.LABEL_REMOTE;
import static io.lettuce.core.metrics.MicrometerCommandLatencyRecorder.METRIC_COMPLETION;
import static io.lettuce.core.metrics.MicrometerCommandLatencyRecorder.METRIC_END_TO_END;
import static io.lettuce.core.metrics.MicrometerCommandLatencyRecorder.METRIC_FIRST_RESPONSE;
import static io.lettuce.core.metrics.MicrometerCommandLatencyRecorder.METRIC_QUEUEING;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
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
@Tag(UNIT_TEST)
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
                .hasFieldOrPropertyWithValue("max", 300.0).hasFieldOrPropertyWithValue("total", 600.0);

        assertThat(meterRegistry.find(METRIC_COMPLETION).timers()).hasSize(2);
        assertThat(meterRegistry.find(METRIC_COMPLETION).tag(LABEL_COMMAND, CommandType.BGSAVE.name()).timers()).hasSize(1)
                .element(0).extracting(Timer::takeSnapshot).hasFieldOrPropertyWithValue("count", 3L)
                .hasFieldOrPropertyWithValue("max", 1500.0).hasFieldOrPropertyWithValue("total", 3000.0);
        assertThat(meterRegistry.find(METRIC_QUEUEING).timers()).isEmpty();
        assertThat(meterRegistry.find(METRIC_END_TO_END).timers()).isEmpty();
    }

    @Test
    void recordsQueueingAndEndToEndLatency() {
        MicrometerCommandLatencyRecorder recorder = new MicrometerCommandLatencyRecorder(meterRegistry,
                MicrometerOptions.builder().queueLatency(true).build());
        assertThat(recorder.isQueueLatencyEnabled()).isTrue();

        recorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 20, 60);
        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 40, 100);

        Timer queueing = meterRegistry.get(METRIC_QUEUEING).tag(LABEL_COMMAND, CommandType.GET.name()).timer();
        Timer endToEnd = meterRegistry.get(METRIC_END_TO_END).tag(LABEL_COMMAND, CommandType.GET.name()).timer();
        assertThat(queueing.count()).isEqualTo(1);
        assertThat(queueing.totalTime(TimeUnit.NANOSECONDS)).isEqualTo(40);
        assertThat(endToEnd.count()).isEqualTo(1);
        assertThat(endToEnd.totalTime(TimeUnit.NANOSECONDS)).isEqualTo(100);
        assertThat(meterRegistry.get(METRIC_COMPLETION).tag(LABEL_COMMAND, CommandType.GET.name()).timer()
                .totalTime(TimeUnit.NANOSECONDS)).isEqualTo(60);
    }

    @Test
    void queueingMetricsRespectEnabledCommands() {
        MicrometerCommandLatencyRecorder recorder = new MicrometerCommandLatencyRecorder(meterRegistry,
                MicrometerOptions.builder().queueLatency(true).enabledCommands(CommandType.GET).build());

        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.AUTH, null), 40, 100);
        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 40, 100);

        assertThat(meterRegistry.getMeters()).hasSize(2);
        for (String metric : Arrays.asList(METRIC_QUEUEING, METRIC_END_TO_END)) {
            assertThat(meterRegistry.find(metric).tag(LABEL_COMMAND, CommandType.AUTH.name()).timers()).isEmpty();
            assertThat(meterRegistry.get(metric).tag(LABEL_COMMAND, CommandType.GET.name()).timer().count()).isEqualTo(1);
        }
    }

    @Test
    void queueingMetricsAreDisabledByDefault() {
        AtomicInteger evaluations = new AtomicInteger();
        MicrometerCommandLatencyRecorder recorder = new MicrometerCommandLatencyRecorder(meterRegistry,
                MicrometerOptions.builder().metricsFilter(command -> {
                    evaluations.incrementAndGet();
                    return true;
                }).build());

        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 40, 100);

        assertThat(recorder.isQueueLatencyEnabled()).isFalse();
        assertThat(evaluations.get()).isZero();
        assertThat(meterRegistry.getMeters()).isEmpty();

        recorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 20, 60);

        assertThat(evaluations.get()).isEqualTo(1);
        assertThat(meterRegistry.getMeters()).hasSize(2);
        assertThat(meterRegistry.get(METRIC_COMPLETION).timer().count()).isEqualTo(1);
        assertThat(meterRegistry.find(METRIC_QUEUEING).timers()).isEmpty();
        assertThat(meterRegistry.find(METRIC_END_TO_END).timers()).isEmpty();
    }

    @Test
    void queueingMetricsEvaluateCustomFilterOncePerCommand() {
        AtomicInteger evaluations = new AtomicInteger();
        Command<String, String, String> accepted = new Command<>(CommandType.GET, null);
        MicrometerCommandLatencyRecorder recorder = new MicrometerCommandLatencyRecorder(meterRegistry,
                MicrometerOptions.builder().queueLatency(true).metricsFilter(command -> {
                    evaluations.incrementAndGet();
                    return command == accepted;
                }).build());

        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 40, 100);
        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, accepted, 40, 100);

        assertThat(evaluations.get()).isEqualTo(2);
        assertThat(meterRegistry.getMeters()).hasSize(2);
        for (String metric : Arrays.asList(METRIC_QUEUEING, METRIC_END_TO_END)) {
            assertThat(meterRegistry.get(metric).timer().count()).isEqualTo(1);
        }
    }

    @Test
    void disabledRecorderDoesNotEvaluateFilterOrRegisterQueueingMetrics() {
        AtomicInteger evaluations = new AtomicInteger();
        MicrometerCommandLatencyRecorder recorder = new MicrometerCommandLatencyRecorder(meterRegistry,
                MicrometerOptions.builder().queueLatency(true).disable().metricsFilter(command -> {
                    evaluations.incrementAndGet();
                    return true;
                }).build());

        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 40, 100);

        assertThat(recorder.isQueueLatencyEnabled()).isFalse();
        assertThat(evaluations.get()).isZero();
        assertThat(meterRegistry.getMeters()).isEmpty();
    }

    @Test
    void queueingMetricsUseConfiguredTagsAndHistograms() {
        Tags tags = Tags.of("app", "queue-tests");
        MicrometerOptions options = MicrometerOptions.builder().queueLatency(true).tags(tags).histogram(true).build();
        MicrometerCommandLatencyRecorder recorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 40, 100);

        for (String metric : Arrays.asList(METRIC_QUEUEING, METRIC_END_TO_END)) {
            Timer timer = meterRegistry.get(metric).tags(tags).tag(LABEL_COMMAND, CommandType.GET.name())
                    .tag(LABEL_REMOTE, REMOTE_ADDRESS.toString()).timer();
            assertThat(timer.getId().getTags()).hasSize(4);
            assertThat(timer.takeSnapshot().percentileValues()).extracting(ValueAtPercentile::percentile)
                    .containsExactlyElementsOf(Arrays.asList(ArrayUtils.toObject(options.targetPercentiles())));
        }
    }

    @Test
    void queueingMetricsAggregateLocalAddressesByDefault() {
        MicrometerCommandLatencyRecorder recorder = new MicrometerCommandLatencyRecorder(meterRegistry,
                MicrometerOptions.builder().queueLatency(true).build());

        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 40, 100);
        recorder.recordCommandQueueLatency(new LocalAddress("localhost:12345"), REMOTE_ADDRESS,
                new Command<>(CommandType.GET, null), 40, 100);

        for (String metric : Arrays.asList(METRIC_QUEUEING, METRIC_END_TO_END)) {
            assertThat(meterRegistry.find(metric).timers()).hasSize(1);
            assertThat(meterRegistry.get(metric).tag(LABEL_LOCAL, LocalAddress.ANY.toString()).timer().count()).isEqualTo(2);
        }
    }

    @Test
    void queueingMetricsRespectLocalDistinction() {
        MicrometerCommandLatencyRecorder recorder = new MicrometerCommandLatencyRecorder(meterRegistry,
                MicrometerOptions.builder().queueLatency(true).localDistinction(true).build());
        LocalAddress secondLocal = new LocalAddress("localhost:12345");

        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 40, 100);
        recorder.recordCommandQueueLatency(secondLocal, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 40, 100);

        for (String metric : Arrays.asList(METRIC_QUEUEING, METRIC_END_TO_END)) {
            assertThat(meterRegistry.find(metric).timers()).hasSize(2);
            assertThat(meterRegistry.get(metric).tag(LABEL_LOCAL, LOCAL_ADDRESS.toString()).timer().count()).isEqualTo(1);
            assertThat(meterRegistry.get(metric).tag(LABEL_LOCAL, secondLocal.toString()).timer().count()).isEqualTo(1);
        }
    }

    @Test
    void concurrentQueueingMeasurementsRegisterOneTimerAndPreserveCounts() throws Exception {
        MicrometerCommandLatencyRecorder recorder = new MicrometerCommandLatencyRecorder(meterRegistry,
                MicrometerOptions.builder().queueLatency(true).build());
        int workers = 4;
        int measurements = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < workers; i++) {
                tasks.add(executor.submit(() -> {
                    start.await();
                    for (int j = 0; j < measurements; j++) {
                        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null),
                                40, 100);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> task : tasks) {
                task.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(meterRegistry.getMeters()).hasSize(2);
        for (String metric : Arrays.asList(METRIC_QUEUEING, METRIC_END_TO_END)) {
            assertThat(meterRegistry.get(metric).timer().count()).isEqualTo(workers * measurements);
        }
        assertThat(meterRegistry.get(METRIC_QUEUEING).timer().totalTime(TimeUnit.NANOSECONDS))
                .isEqualTo(workers * measurements * 40);
        assertThat(meterRegistry.get(METRIC_END_TO_END).timer().totalTime(TimeUnit.NANOSECONDS))
                .isEqualTo(workers * measurements * 100);
    }

    @Test
    void queueingCallbackIsNoOpForLegacyRecorders() {
        AtomicInteger invocations = new AtomicInteger();
        AtomicReference<ProtocolKeyword> recordedType = new AtomicReference<>();
        CommandLatencyRecorder recorder = (local, remote, type, firstResponse, completion) -> {
            invocations.incrementAndGet();
            assertThat(local).isEqualTo(LOCAL_ADDRESS);
            assertThat(remote).isEqualTo(REMOTE_ADDRESS);
            recordedType.set(type);
            assertThat(firstResponse).isEqualTo(20);
            assertThat(completion).isEqualTo(60);
        };

        recorder.recordCommandQueueLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 40, 100);

        assertThat(recorder.isQueueLatencyEnabled()).isFalse();
        assertThat(invocations.get()).isZero();
        assertThat(recordedType.get()).isNull();

        recorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.GET, null), 20, 60);

        assertThat(invocations.get()).isEqualTo(1);
        assertThat(recordedType.get()).isEqualTo(CommandType.GET);
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

    @Test
    void enabledCommandsEmpty() {
        MicrometerOptions options = MicrometerOptions.builder().build();
        MicrometerCommandLatencyRecorder commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.AUTH, 1, 10);
        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, CommandType.CLUSTER, 1, 10);

        assertThat(meterRegistry.find(METRIC_COMPLETION).timers()).hasSize(2);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).timers()).hasSize(2);
    }

    @Test
    void enabledCommandsNotEmpty() {
        MicrometerOptions options = MicrometerOptions.builder().enabledCommands(CommandType.CLUSTER).build();
        MicrometerCommandLatencyRecorder commandLatencyRecorder = new MicrometerCommandLatencyRecorder(meterRegistry, options);

        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.AUTH, null), 1,
                10);
        commandLatencyRecorder.recordCommandLatency(LOCAL_ADDRESS, REMOTE_ADDRESS, new Command<>(CommandType.CLUSTER, null), 1,
                10);

        assertThat(meterRegistry.find(METRIC_COMPLETION).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).timers()).hasSize(1);

        assertThat(meterRegistry.find(METRIC_COMPLETION).tag(LABEL_COMMAND, CommandType.CLUSTER.name()).timers()).hasSize(1);
        assertThat(meterRegistry.find(METRIC_FIRST_RESPONSE).tag(LABEL_COMMAND, CommandType.CLUSTER.name()).timers())
                .hasSize(1);
    }

}
