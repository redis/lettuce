/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.timeseries.arguments.TsIncrByArgs;
import io.lettuce.core.timeseries.arguments.TsReadArgs;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * TS.READ with BLOCK (Redis 8.10+, test plan F04–F10). Every test opens its own reader connection and, where a write must wake
 * the reader, a second producer connection, and closes both at the end (test plan §3). The producer waits until the server
 * reports the reader as blocked ({@code CLIENT LIST}, flag {@code b}) before it writes, so no test relies on a sleep. Block
 * times are at most 1 s, except F10.
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledOnCommand("TS.ADD")
public class RedisTimeSeriesBlockingIntegrationTests {

    private static final String KEY = "{ts}:BLOCK";

    private final RedisClient client;

    private StatefulRedisConnection<String, String> reader;

    private StatefulRedisConnection<String, String> producer;

    @Inject
    RedisTimeSeriesBlockingIntegrationTests(RedisClient client) {
        this.client = client;
    }

    @BeforeEach
    void connect() {
        reader = client.connect();
        producer = client.connect();
        producer.sync().flushall();
    }

    @AfterEach
    void close() {
        reader.close();
        producer.close();
    }

    /**
     * Wait until the server reports the client {@code clientId} as blocked in TS.READ. Polls {@code CLIENT LIST} on the
     * producer connection; fails after 5 s.
     */
    private void awaitBlocked(long clientId) {

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            String line = producer.sync().clientList().trim();
            for (String client : line.split("\\r?\\n")) {
                if (client.startsWith("id=" + clientId + " ") && client.contains(" flags=b ")
                        && client.contains(" cmd=ts.read ")) {
                    return;
                }
            }
        }
        throw new AssertionError("Reader " + clientId + " did not block in TS.READ");
    }

    /**
     * Scenario F04: {@code block(300 ms, minCount 5)} with 2 samples from the offset on returns the 2 samples, a partial
     * result, after the block time.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void blockTimeoutReturnsPartialResult() {

        RedisCommands<String, String> sync = producer.sync();
        sync.tsCreate(KEY);
        sync.tsAdd(KEY, 1000L, 1.0);
        sync.tsAdd(KEY, 2000L, 2.0);

        long start = System.nanoTime();
        List<TsSample> samples = reader.sync().tsRead(KEY, TsReadOffset.from(0), new TsReadArgs().block(300, 5));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(samples).containsExactly(TsSample.of(1000, 1.0), TsSample.of(2000, 2.0));
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(250);
    }

    /**
     * Scenario F04: a block timeout without any sample returns an empty list.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void blockTimeoutWithoutSamplesReturnsEmpty() {

        producer.sync().tsCreate(KEY);

        assertThat(reader.sync().tsRead(KEY, TsReadOffset.newSamples(), new TsReadArgs().block(200, 1))).isEmpty();
    }

    /**
     * Scenario F05: when {@code minCount} samples already exist, TS.READ BLOCK returns at once, also with {@code BLOCK 0}.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void thresholdAlreadyMetReturnsImmediately() {

        RedisCommands<String, String> sync = producer.sync();
        sync.tsCreate(KEY);
        sync.tsAdd(KEY, 1000L, 1.0);
        sync.tsAdd(KEY, 2000L, 2.0);
        sync.tsAdd(KEY, 3000L, 3.0);
        List<TsSample> all = Arrays.asList(TsSample.of(1000, 1.0), TsSample.of(2000, 2.0), TsSample.of(3000, 3.0));

        long start = System.nanoTime();
        assertThat(reader.sync().tsRead(KEY, TsReadOffset.from(0), new TsReadArgs().block(1000, 2))).isEqualTo(all);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(1000);

        reader.setTimeout(Duration.ofSeconds(5));
        assertThat(reader.sync().tsRead(KEY, TsReadOffset.earliest(), new TsReadArgs().block(0, 3))).isEqualTo(all);
    }

    /**
     * Scenario F06: {@code newSamples()} with BLOCK on a dedicated connection is woken by a TS.ADD from a second connection;
     * the reply holds exactly the new sample and arrives before the block time ends. Key {@code {ts}:BLOCK}.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void wokenByWrite() throws Exception {

        RedisCommands<String, String> sync = producer.sync();
        sync.tsCreate(KEY);
        sync.tsAdd(KEY, 1000L, 1.0);
        long readerId = reader.sync().clientId();

        CountDownLatch done = new CountDownLatch(1);
        RedisFuture<List<TsSample>> read = reader.async().tsRead(KEY, TsReadOffset.newSamples(),
                new TsReadArgs().block(1000, 1));
        read.thenRun(done::countDown);

        awaitBlocked(readerId);
        long written = System.nanoTime();
        sync.tsAdd(KEY, 5000L, 5.0);

        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - written)).isLessThan(900);
        assertThat(read.get()).containsExactly(TsSample.of(5000, 5.0));
    }

    /**
     * Scenario F06: TS.INCRBY also wakes a blocked reader (TIMESERIES-CANONICAL.md, TS.READ: writes that unblock).
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void wokenByIncrBy() throws Exception {

        RedisCommands<String, String> sync = producer.sync();
        sync.tsCreate(KEY);
        sync.tsAdd(KEY, 1000L, 1.0);
        long readerId = reader.sync().clientId();

        RedisFuture<List<TsSample>> read = reader.async().tsRead(KEY, TsReadOffset.newSamples(),
                new TsReadArgs().block(1000, 1));
        awaitBlocked(readerId);
        sync.tsIncrBy(KEY, 2.0, new TsIncrByArgs().timestamp(2000));

        assertThat(read.get(5, TimeUnit.SECONDS)).containsExactly(TsSample.of(2000, 3.0));
    }

    /**
     * Scenario F07: BLOCK with MAX_COUNT. One TS.MADD of 5 samples wakes the reader, and {@code maxCount(3)} returns the 3
     * oldest.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void blockWithMaxCount() throws Exception {

        RedisCommands<String, String> sync = producer.sync();
        sync.tsCreate(KEY);
        long readerId = reader.sync().clientId();

        RedisFuture<List<TsSample>> read = reader.async().tsRead(KEY, TsReadOffset.newSamples(),
                new TsReadArgs().block(1000, 1).maxCount(3));
        awaitBlocked(readerId);
        assertThat(sync.tsMAdd(Arrays.asList(TsMAddValue.of(KEY, 1000L, 1.0), TsMAddValue.of(KEY, 2000L, 2.0),
                TsMAddValue.of(KEY, 3000L, 3.0), TsMAddValue.of(KEY, 4000L, 4.0), TsMAddValue.of(KEY, 5000L, 5.0))))
                        .containsExactly(1000L, 2000L, 3000L, 4000L, 5000L);

        assertThat(read.get(5, TimeUnit.SECONDS)).containsExactly(TsSample.of(1000, 1.0), TsSample.of(2000, 2.0),
                TsSample.of(3000, 3.0));
    }

    /**
     * Scenario F08: deleting the key while a reader is blocked wakes it with an empty list, before the block time ends.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void keyDeletedWhileBlocked() throws Exception {

        RedisCommands<String, String> sync = producer.sync();
        sync.tsCreate(KEY);
        sync.tsAdd(KEY, 1000L, 1.0);
        long readerId = reader.sync().clientId();

        RedisFuture<List<TsSample>> read = reader.async().tsRead(KEY, TsReadOffset.newSamples(),
                new TsReadArgs().block(1000, 5));
        awaitBlocked(readerId);
        long deleted = System.nanoTime();
        assertThat(sync.del(KEY)).isEqualTo(1);

        assertThat(read.get(5, TimeUnit.SECONDS)).isEmpty();
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - deleted)).isLessThan(900);
    }

    /**
     * Scenario F08: with {@code BLOCK 0} (no time limit), deleting the key also wakes the reader with an empty list.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void keyDeletedWhileBlockedForever() throws Exception {

        RedisCommands<String, String> sync = producer.sync();
        sync.tsCreate(KEY);
        sync.tsAdd(KEY, 1000L, 1.0);
        long readerId = reader.sync().clientId();

        RedisFuture<List<TsSample>> read = reader.async().tsRead(KEY, TsReadOffset.newSamples(), new TsReadArgs().block(0, 1));
        awaitBlocked(readerId);
        assertThat(sync.del(KEY)).isEqualTo(1);

        assertThat(read.get(5, TimeUnit.SECONDS)).isEmpty();
    }

    /**
     * Scenario F09: inside MULTI, a TS.READ BLOCK that would have to wait fails with
     * {@code TSDB: blocking TS.READ (with BLOCK) is not allowed inside MULTI, EVAL, or a deny-blocking context}; one whose
     * threshold is already met ({@code from(0)}) returns the sample (observed on 8.10.0).
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void blockInsideMulti() {

        RedisCommands<String, String> sync = reader.sync();
        sync.tsCreate(KEY);
        sync.tsAdd(KEY, 1000L, 1.0);

        sync.multi();
        sync.tsRead(KEY, TsReadOffset.newSamples(), new TsReadArgs().block(100, 1));
        sync.tsRead(KEY, TsReadOffset.from(0), new TsReadArgs().block(100, 1));
        TransactionResult result = sync.exec();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(2);
        assertThat((Object) result.get(0)).isInstanceOf(RedisCommandExecutionException.class);
        assertThat(((RedisCommandExecutionException) result.get(0)))
                .hasMessageContaining("TSDB: blocking TS.READ (with BLOCK) is not allowed inside MULTI");
        assertThat(result.<List<TsSample>> get(1)).containsExactly(TsSample.of(1000, 1.0));

        assertThat(sync.tsGet(KEY)).isEqualTo(TsSample.of(1000, 1.0));
    }

    /**
     * Scenario F10: a command timeout shorter than the block time fails the call with {@link RedisCommandTimeoutException}; the
     * connection stays usable, and the next command on it gets its own reply.
     */
    @Test
    @EnabledOnCommand("TS.READ")
    void commandTimeoutShorterThanBlock() {

        producer.sync().tsCreate(KEY);
        producer.sync().tsAdd(KEY, 1000L, 1.0);

        reader.setTimeout(Duration.ofMillis(200));
        assertThatThrownBy(() -> reader.sync().tsRead(KEY, TsReadOffset.newSamples(), new TsReadArgs().block(1500, 1)))
                .isInstanceOf(RedisCommandTimeoutException.class);

        reader.setTimeout(Duration.ofSeconds(10));
        assertThat(reader.sync().tsGet(KEY)).isEqualTo(TsSample.of(1000, 1.0));
        assertThat(reader.sync().tsRange(KEY, Range.unbounded())).containsExactly(TsSample.of(1000, 1.0));
        assertThat(reader.sync().ping()).isEqualTo("PONG");
    }

}
