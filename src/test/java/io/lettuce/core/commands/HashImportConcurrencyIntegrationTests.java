/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
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

import javax.enterprise.inject.New;
import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.HashImport;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.event.command.CommandFailedEvent;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.test.Delay;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Concurrency-under-reconnect coverage for {@code HIMPORT}: while several threads continuously issue {@code himportSet} on a
 * shared fieldset, the connection is repeatedly bounced. Every {@code SET} must still succeed and no {@code no such fieldset}
 * error may reach the wire — the outbound handler re-injects the fieldset's {@code PREPARE} ahead of the first write on each
 * fresh connection (including buffered/requeued writes flushed as a batch), so concurrent SETs straddling a reconnect are
 * always preceded by their {@code PREPARE}.
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("HIMPORT")
class HashImportConcurrencyIntegrationTests {

    private final RedisClient client;

    @Inject
    HashImportConcurrencyIntegrationTests(@New RedisClient client) {
        this.client = client;
    }

    @Test
    void concurrentSetsSurviveReconnects() throws Exception {

        AtomicInteger noSuchFieldsetFailures = new AtomicInteger();
        CommandListener listener = countingNoSuchFieldset(noSuchFieldsetFailures);
        client.addListener(listener);

        StatefulRedisConnection<String, String> connection = client.connect();
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
                // Bounce the connection several times while the writers hammer it.
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

            // The whole point: reconnects were transparent to the concurrent writers.
            assertThat(firstError.get()).isNull();
            assertThat(noSuchFieldsetFailures).hasValue(0);

            // Sanity: writes actually happened across the reconnects and produced ordinary hashes.
            assertThat(sequence.get()).isGreaterThan(0L);
            assertThat(redis.dbsize()).isGreaterThan(0L);
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
