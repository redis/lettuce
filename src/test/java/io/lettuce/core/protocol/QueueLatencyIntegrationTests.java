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
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.ConnectionTestUtil;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Tag(INTEGRATION_TEST)
class QueueLatencyIntegrationTests {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static final String QUEUEING = "lettuce.command.queueing";

    private static final String END_TO_END = "lettuce.command.endtoend";

    private static final String COMPLETION = "lettuce.command.completion";

    private SimpleMeterRegistry registry;

    private ClientResources resources;

    private RedisClient client;

    private StatefulRedisConnection<String, String> connection;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        MicrometerCommandLatencyRecorder recorder = new MicrometerCommandLatencyRecorder(registry,
                MicrometerOptions.builder().queueLatency(true).enabledCommands(CommandType.ECHO).build());
        resources = ClientResources.builder().commandLatencyRecorder(recorder).build();
        client = RedisClient.create(resources, RedisURI.create(TestSettings.host(), TestSettings.port()));
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        connection = client.connect();
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            FastShutdown.shutdown(client);
        }
        if (resources != null) {
            FastShutdown.shutdown(resources);
        }
        if (registry != null) {
            registry.close();
        }
    }

    @Test
    void recordsConcurrentSubmissionsExactlyOnce() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<CompletableFuture<Void>> tasks = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) {
                tasks.add(CompletableFuture.runAsync(() -> {
                    List<RedisFuture<String>> pending = new ArrayList<>();
                    for (int i = 0; i < 64; i++) {
                        pending.add(connection.async().echo("concurrent"));
                    }
                    for (RedisFuture<String> future : pending) {
                        assertThat(future.toCompletableFuture().join()).isEqualTo("concurrent");
                    }
                }, executor));
            }
            CompletableFuture.allOf(tasks.toArray(new CompletableFuture<?>[0])).get(10, TimeUnit.SECONDS);
            awaitSamples(512);
            assertAdditiveLatencies();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void recordsReactiveTransactionalCommandAfterExec() {
        assertThat(connection.reactive().multi().block(TIMEOUT)).isEqualTo("OK");
        CompletableFuture<String> pending = connection.reactive().echo("reactive-transaction").toFuture();
        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(registry.find(COMPLETION).timer()).isNotNull();
            assertThat(timer(COMPLETION).count()).isEqualTo(1);
        });
        assertThat(pending).isNotDone();
        assertThat(registry.find(END_TO_END).timer()).isNull();

        assertThat(connection.reactive().exec().block(TIMEOUT)).containsExactly("reactive-transaction");
        assertThat(pending.join()).isEqualTo("reactive-transaction");
        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(timer(QUEUEING).count()).isEqualTo(1);
            assertThat(timer(END_TO_END).count()).isEqualTo(1);
        });
    }

    @Test
    void recordsSyncAsyncAndReactiveCommands() throws Exception {
        assertThat(connection.sync().echo("sync")).isEqualTo("sync");
        assertThat(connection.async().echo("async").get(10, TimeUnit.SECONDS)).isEqualTo("async");
        assertThat(connection.reactive().echo("reactive").block(TIMEOUT)).isEqualTo("reactive");

        awaitSamples(3);
        assertAdditiveLatencies();
    }

    @Test
    void includesManualFlushBufferingInQueueAndEndToEndLatency() throws Exception {
        connection.setAutoFlushCommands(false);
        RedisFuture<String> first = connection.async().echo("first");
        RedisFuture<String> second = connection.async().echo("second");
        long submittedAt = AsyncCommand.queuedAt((RedisCommand<?, ?, ?>) first);
        assertThat(submittedAt).isNotZero();

        await().during(Duration.ofMillis(100)).atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(first.isDone()).isFalse();
            assertThat(second.isDone()).isFalse();
            assertThat(registry.find(QUEUEING).timer()).isNull();
        });
        long minimumQueueTime = System.nanoTime() - submittedAt;
        connection.flushCommands();

        assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo("first");
        assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo("second");
        awaitSamples(2);
        assertThat(timer(QUEUEING).totalTime(TimeUnit.NANOSECONDS)).isGreaterThanOrEqualTo((double) minimumQueueTime);
        assertAdditiveLatencies();
    }

    @Test
    void cancelledBufferedCommandsDoNotRecordLatency() throws Exception {
        connection.setAutoFlushCommands(false);
        RedisFuture<String> cancelled = connection.async().echo("cancelled");
        assertThat(cancelled.cancel(false)).isTrue();
        RedisFuture<String> barrier = connection.async().echo("barrier");
        connection.flushCommands();

        assertThat(barrier.get(10, TimeUnit.SECONDS)).isEqualTo("barrier");
        awaitSamples(1);
        assertThat(cancelled.isCancelled()).isTrue();
        assertAdditiveLatencies();
    }

    @Test
    void disconnectedCommandRetainsSubmissionTimeAfterRealReconnect() throws Exception {
        ConnectionWatchdog watchdog = ConnectionTestUtil.getChannel(connection).pipeline().get(ConnectionWatchdog.class);
        assertThat(watchdog).isNotNull();
        watchdog.setListenOnChannelInactive(false);
        ConnectionTestUtil.getChannel(connection).close().sync();
        await().atMost(TIMEOUT).until(() -> !connection.isOpen());

        RedisFuture<String> pending = connection.async().echo("reconnected");
        long submittedAt = AsyncCommand.queuedAt((RedisCommand<?, ?, ?>) pending);
        assertThat(submittedAt).isNotZero();
        await().during(Duration.ofMillis(100)).atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(pending.isDone()).isFalse();
            assertThat(connection.isOpen()).isFalse();
        });
        long minimumQueueTime = System.nanoTime() - submittedAt;
        watchdog.setListenOnChannelInactive(true);
        watchdog.scheduleReconnect();

        assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo("reconnected");
        await().atMost(TIMEOUT).until(connection::isOpen);
        awaitSamples(1);
        assertThat(AsyncCommand.queuedAt((RedisCommand<?, ?, ?>) pending)).isEqualTo(submittedAt);
        assertThat(timer(QUEUEING).totalTime(TimeUnit.NANOSECONDS)).isGreaterThanOrEqualTo((double) minimumQueueTime);
        assertAdditiveLatencies();
    }

    @Test
    void recordsTransactionalCommandOnlyAfterExec() throws Exception {
        assertThat(connection.sync().multi()).isEqualTo("OK");
        RedisFuture<String> pending = connection.async().echo("transactional");

        // The existing network timer confirms that the initial QUEUED reply was decoded.
        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(registry.find(COMPLETION).timer()).isNotNull();
            assertThat(timer(COMPLETION).count()).isEqualTo(1);
        });
        assertThat(pending.isDone()).isFalse();
        assertThat(registry.find(QUEUEING).timer()).isNull();
        assertThat(registry.find(END_TO_END).timer()).isNull();

        connection.sync().exec();
        assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo("transactional");
        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(registry.find(QUEUEING).timer()).isNotNull();
            assertThat(registry.find(END_TO_END).timer()).isNotNull();
            assertThat(timer(QUEUEING).count()).isEqualTo(1);
            assertThat(timer(END_TO_END).count()).isEqualTo(1);
        });
        assertThat(timer(END_TO_END).totalTime(TimeUnit.NANOSECONDS))
                .isGreaterThanOrEqualTo(timer(QUEUEING).totalTime(TimeUnit.NANOSECONDS));
    }

    @Test
    void replayedInFlightCommandKeepsInitialQueueLatencyAndIncludesReconnectTime() throws Exception {
        RedisFuture<KeyValue<String, String>> blocked = connection.async().blpop(0, UUID.randomUUID().toString());
        RedisFuture<String> pending = connection.async().echo("replayed");
        RedisCommand<?, ?, ?> command = (RedisCommand<?, ?, ?>) pending;
        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(ConnectionTestUtil.getChannel(connection).eventLoop()
                    .submit(() -> ConnectionTestUtil.getStack(connection).size()).get(10, TimeUnit.SECONDS))
                            .isGreaterThanOrEqualTo(2);
            assertThat(AsyncCommand.firstSent(command)).isNotZero();
            assertThat(pending.isDone()).isFalse();
        });
        long submittedAt = AsyncCommand.queuedAt(command);
        long firstSent = AsyncCommand.firstSent(command);
        assertThat(submittedAt).isNotZero();
        ConnectionWatchdog watchdog = ConnectionTestUtil.getChannel(connection).pipeline().get(ConnectionWatchdog.class);
        watchdog.setListenOnChannelInactive(false);
        assertThat(blocked.cancel(false)).isTrue();
        ConnectionTestUtil.getChannel(connection).close().sync();
        await().atMost(TIMEOUT).until(() -> !connection.isOpen());
        await().during(Duration.ofMillis(100)).atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(pending.isDone()).isFalse();
            assertThat(registry.find(QUEUEING).timer()).isNull();
            assertThat(registry.find(END_TO_END).timer()).isNull();
        });
        long minimumEndToEndTime = System.nanoTime() - submittedAt;
        watchdog.setListenOnChannelInactive(true);
        watchdog.scheduleReconnect();

        assertThat(pending.get(10, TimeUnit.SECONDS)).isEqualTo("replayed");
        awaitSamples(1);
        assertThat(blocked.isCancelled()).isTrue();
        assertThat(AsyncCommand.queuedAt(command)).isEqualTo(submittedAt);
        assertThat(AsyncCommand.firstSent(command)).isEqualTo(firstSent);
        assertThat(timer(QUEUEING).totalTime(TimeUnit.NANOSECONDS)).isEqualTo((double) (firstSent - submittedAt));
        assertThat(timer(END_TO_END).totalTime(TimeUnit.NANOSECONDS)).isGreaterThanOrEqualTo((double) minimumEndToEndTime);
    }

    @Test
    void cancelledTransactionalCommandDoesNotRecordQueueLatencyAfterExec() {
        assertThat(connection.sync().multi()).isEqualTo("OK");
        RedisFuture<String> pending = connection.async().echo("cancelled-transaction");
        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(registry.find(COMPLETION).timer()).isNotNull();
            assertThat(timer(COMPLETION).count()).isEqualTo(1);
        });
        assertThat(pending.cancel(false)).isTrue();
        connection.sync().exec();

        await().during(Duration.ofMillis(100)).atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(pending.isCancelled()).isTrue();
            assertThat(registry.find(QUEUEING).timer()).isNull();
            assertThat(registry.find(END_TO_END).timer()).isNull();
        });
    }

    private void awaitSamples(long expected) {
        await().atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(registry.find(QUEUEING).timer()).isNotNull();
            assertThat(registry.find(END_TO_END).timer()).isNotNull();
            assertThat(registry.find(COMPLETION).timer()).isNotNull();
            assertThat(timer(QUEUEING).count()).isEqualTo(expected);
            assertThat(timer(END_TO_END).count()).isEqualTo(expected);
            assertThat(timer(COMPLETION).count()).isEqualTo(expected);
        });
    }

    private void assertAdditiveLatencies() {
        double queue = timer(QUEUEING).totalTime(TimeUnit.NANOSECONDS);
        double completion = timer(COMPLETION).totalTime(TimeUnit.NANOSECONDS);
        assertThat(queue).isGreaterThanOrEqualTo(0);
        assertThat(completion).isGreaterThanOrEqualTo(0);
        assertThat(timer(END_TO_END).totalTime(TimeUnit.NANOSECONDS)).isCloseTo(queue + completion, within(0.001));
    }

    private Timer timer(String name) {
        return registry.get(name).tag("command", CommandType.ECHO.name()).timer();
    }

}
