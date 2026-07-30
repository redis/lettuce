/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.HashImport;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.command.CommandFailedEvent;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.test.Delay;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.settings.TestSettings;

/**
 * Master/replica connection-session-state coverage for {@code HIMPORT}. A master/replica connection delegates I/O to physical
 * connections created by {@link MasterReplicaConnectionProvider}; each carries the outbound handler, so this test proves the
 * fieldset is re-prepared lazily on the reconnected physical connection after the master connection is bounced (the same
 * write-path a primary failover relies on). A command listener records {@code no such fieldset} failures: a zero count means
 * the handler injected the {@code PREPARE} ahead of the {@code SET} on the fresh connection.
 */
@Tag(INTEGRATION_TEST)
@EnabledOnCommand("HIMPORT")
class HashImportMasterReplicaIntegrationTests extends AbstractRedisClientTest {

    @Test
    void reconnectPreservesFieldsetOnMasterReplica() {

        RedisURI node1 = RedisURI.Builder.redis(host, TestSettings.port(3)).build();
        RedisURI node2 = RedisURI.Builder.redis(host, TestSettings.port(4)).build();

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        client.addListener(listener);

        StatefulRedisMasterReplicaConnection<String, String> connection = MasterReplica.connect(client, StringCodec.UTF8,
                Arrays.asList(node1, node2));
        try {
            RedisCommands<String, String> redis = connection.sync();
            redis.flushall();

            HashImport<String> fieldset = HashImport.of("name", "email");
            assertThat(redis.himportSet("u:1", fieldset, "alice", "a@x.com")).isEqualTo("OK");

            // Bounce the (master) physical connection; its watchdog reconnects the same connection.
            redis.quit();
            Delay.delay(Duration.ofMillis(200));

            // No re-prepare: the reconnected physical connection re-injects PREPARE lazily ahead of this SET.
            assertThat(redis.himportSet("u:2", fieldset, "bob", "b@x.com")).isEqualTo("OK");
            assertThat(redis.hget("u:2", "name")).isEqualTo("bob");

            assertThat(noSuchFieldsetFailures).hasValue(0);
        } finally {
            connection.close();
            client.removeListener(listener);
        }
    }

    @Test
    void concurrentSetsSurviveMasterReconnects() throws Exception {

        RedisURI node1 = RedisURI.Builder.redis(host, TestSettings.port(3)).build();
        RedisURI node2 = RedisURI.Builder.redis(host, TestSettings.port(4)).build();

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        client.addListener(listener);

        StatefulRedisMasterReplicaConnection<String, String> connection = MasterReplica.connect(client, StringCodec.UTF8,
                Arrays.asList(node1, node2));
        try {
            RedisCommands<String, String> redis = connection.sync();
            redis.flushall();

            HashImport<String> fieldset = HashImport.of("name", "email");

            int writerThreads = 6;
            ExecutorService writers = Executors.newFixedThreadPool(writerThreads);
            CountDownLatch started = new CountDownLatch(writerThreads);
            AtomicBoolean running = new AtomicBoolean(true);
            AtomicLong sequence = new AtomicLong();
            AtomicReference<Throwable> firstError = new AtomicReference<>();
            List<Future<?>> tasks = new ArrayList<>();

            for (int t = 0; t < writerThreads; t++) {
                tasks.add(writers.submit(() -> {
                    started.countDown();
                    while (running.get() && firstError.get() == null) {
                        long n = sequence.getAndIncrement();
                        try {
                            String reply = redis.himportSet("u:" + n, fieldset, "name-" + n, "mail-" + n);
                            if (!"OK".equals(reply)) {
                                firstError.compareAndSet(null, new IllegalStateException("unexpected reply: " + reply));
                            }
                        } catch (Throwable ex) {
                            firstError.compareAndSet(null, ex);
                        }
                    }
                    return null;
                }));
            }

            try {
                // Bounce the (master) physical connection several times while the writers hammer it.
                started.await(10, TimeUnit.SECONDS);
                for (int i = 0; i < 5 && firstError.get() == null; i++) {
                    Delay.delay(Duration.ofMillis(150));
                    redis.quit();
                    Wait.untilTrue(connection::isOpen).waitOrTimeout();
                }
                Delay.delay(Duration.ofMillis(150));
            } finally {
                running.set(false);
                for (Future<?> task : tasks) {
                    task.get(30, TimeUnit.SECONDS);
                }
                writers.shutdownNow();
            }

            // Reconnects of the master physical connection were transparent to the concurrent writers.
            assertThat(firstError.get()).isNull();
            assertThat(noSuchFieldsetFailures).hasValue(0);

            // Sanity: writes actually happened across the reconnects and produced ordinary hashes.
            assertThat(sequence.get()).isGreaterThan(0L);
            assertThat(redis.hget("u:0", "name")).isEqualTo("name-0");
        } finally {
            connection.close();
            client.removeListener(listener);
        }
    }

    private static CommandListener countingNoSuchFieldset(AtomicInteger counter) {
        return new CommandListener() {

            @Override
            public void commandFailed(CommandFailedEvent event) {
                Throwable cause = event.getCause();
                if (cause != null && cause.getMessage() != null
                        && cause.getMessage().toLowerCase().contains("no such fieldset")) {
                    counter.incrementAndGet();
                }
            }

        };
    }

}
