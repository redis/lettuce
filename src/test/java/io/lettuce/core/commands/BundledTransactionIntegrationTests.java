/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.test.LettuceExtension;
import reactor.test.StepVerifier;

/**
 * Integration tests for bundled transactions using {@link TransactionBuilder}.
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BundledTransactionIntegrationTests extends TestSupport {

    private final RedisClient client;

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    protected BundledTransactionIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.connection = connection;
    }

    @BeforeEach
    void setUp() {
        this.connection.sync().flushall();
    }

    @Test
    void basicSyncTransaction() {
        TransactionBuilder<String, String> builder = connection.transaction();
        builder.commands().set(key, value);
        builder.commands().get(key);

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(2);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((String) result.get(1)).isEqualTo(value);
    }

    @Test
    void basicAsyncTransaction() throws Exception {
        TransactionBuilder<String, String> builder = connection.transaction();
        builder.commands().set(key, value);
        builder.commands().incr("counter");
        builder.commands().get(key);

        RedisFuture<TransactionResult> future = builder.executeAsync();
        TransactionResult result = future.get(5, TimeUnit.SECONDS);

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(3);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((Long) result.get(1)).isEqualTo(1L);
        assertThat((String) result.get(2)).isEqualTo(value);
    }

    @Test
    void reactiveTransaction() {
        TransactionBuilder<String, String> builder = connection.transaction();
        builder.commands().set(key, value);
        builder.commands().get(key);

        StepVerifier.create(builder.executeReactive()).consumeNextWith(result -> {
            assertThat(result.wasDiscarded()).isFalse();
            assertThat(result).hasSize(2);
            assertThat((String) result.get(0)).isEqualTo("OK");
            assertThat((String) result.get(1)).isEqualTo(value);
        }).verifyComplete();
    }

    @Test
    void transactionWithWatch() throws Exception {
        // In bundled transactions, WATCH+MULTI+commands+EXEC are sent atomically.
        // Therefore, we can't test WATCH abort by modifying a key BEFORE execute().
        // Instead, we test that WATCH works correctly when no modification occurs.

        // Set initial value
        connection.sync().set(key, "initial");

        // Create transaction with WATCH - should succeed since no modification between WATCH and EXEC
        TransactionBuilder<String, String> builder = connection.transaction(key);
        builder.commands().set(key, value);

        TransactionResult result = builder.execute();
        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(1);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat(connection.sync().get(key)).isEqualTo(value);
    }

    @Test
    void transactionWithMultipleCommands() {
        TransactionBuilder<String, String> builder = connection.transaction();

        // String commands
        builder.commands().set("str1", "value1");
        builder.commands().set("str2", "value2");
        builder.commands().mget("str1", "str2");

        // Hash commands
        builder.commands().hset("hash", "field1", "val1");
        builder.commands().hget("hash", "field1");

        // List commands
        builder.commands().lpush("list", "a", "b", "c");
        builder.commands().lrange("list", 0, -1);

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(7);
        assertThat((String) result.get(0)).isEqualTo("OK"); // SET
        assertThat((String) result.get(1)).isEqualTo("OK"); // SET
        // HSET returns 1 (Long) in RESP2 or true (Boolean) in RESP3
        Object hsetResult = result.get(3);
        assertThat(hsetResult).satisfiesAnyOf(r -> assertThat(r).isEqualTo(1L), r -> assertThat(r).isEqualTo(true));
    }

    @Test
    void emptyTransaction() {
        TransactionBuilder<String, String> builder = connection.transaction();

        assertThat(builder.isEmpty()).isTrue();
        assertThat(builder.size()).isEqualTo(0);

        TransactionResult result = builder.execute();
        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).isEmpty();
    }

    @Test
    void transactionWithIncrementAndDecrement() {
        connection.sync().set("counter", "10");

        TransactionBuilder<String, String> builder = connection.transaction();
        builder.commands().incr("counter");
        builder.commands().incrby("counter", 5);
        builder.commands().decr("counter");
        builder.commands().decrby("counter", 3);
        builder.commands().get("counter");

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(5);
        assertThat((Long) result.get(0)).isEqualTo(11L);
        assertThat((Long) result.get(1)).isEqualTo(16L);
        assertThat((Long) result.get(2)).isEqualTo(15L);
        assertThat((Long) result.get(3)).isEqualTo(12L);
        assertThat((String) result.get(4)).isEqualTo("12");
    }

    @Test
    void threadSafetyWithConcurrentTransactions() throws Exception {
        int numThreads = 10;
        int incrementsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicBoolean hasError = new AtomicBoolean(false);

        connection.sync().set("counter", "0");

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < incrementsPerThread; i++) {
                        TransactionBuilder<String, String> builder = connection.transaction();
                        builder.commands().incr("counter");
                        TransactionResult result = builder.execute();
                        if (!result.wasDiscarded()) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    hasError.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(hasError.get()).isFalse();
        assertThat(successCount.get()).isEqualTo(numThreads * incrementsPerThread);

        // Verify final counter value
        String finalValue = connection.sync().get("counter");
        assertThat(Long.parseLong(finalValue)).isEqualTo(numThreads * incrementsPerThread);
    }

    @Test
    void transactionWithSortedSets() {
        TransactionBuilder<String, String> builder = connection.transaction();
        builder.commands().zadd("zset", 1.0, "one");
        builder.commands().zadd("zset", 2.0, "two");
        builder.commands().zadd("zset", 3.0, "three");
        builder.commands().zcard("zset");
        builder.commands().zscore("zset", "two");
        builder.commands().zrange("zset", 0, -1);

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(6);
        assertThat((Long) result.get(3)).isEqualTo(3L); // ZCARD
        assertThat((Double) result.get(4)).isEqualTo(2.0); // ZSCORE
    }

    @Test
    void transactionWithSets() {
        TransactionBuilder<String, String> builder = connection.transaction();
        builder.commands().sadd("set", "a", "b", "c");
        builder.commands().scard("set");
        builder.commands().sismember("set", "b");
        builder.commands().smembers("set");
        builder.commands().srem("set", "a");

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(5);
        assertThat((Long) result.get(0)).isEqualTo(3L); // SADD
        assertThat((Long) result.get(1)).isEqualTo(3L); // SCARD
        assertThat((Boolean) result.get(2)).isTrue(); // SISMEMBER
    }

    @Test
    void transactionWithKeyCommands() {
        connection.sync().set(key, value);

        TransactionBuilder<String, String> builder = connection.transaction();
        builder.commands().exists(key);
        builder.commands().type(key);
        builder.commands().ttl(key);
        builder.commands().expire(key, 3600);
        builder.commands().ttl(key);

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(5);
        assertThat((Long) result.get(0)).isEqualTo(1L); // EXISTS
        assertThat((String) result.get(1)).isEqualTo("string"); // TYPE
        assertThat((Long) result.get(2)).isEqualTo(-1L); // TTL (no expiry)
        assertThat((Boolean) result.get(3)).isTrue(); // EXPIRE
        assertThat((Long) result.get(4)).isGreaterThan(0L); // TTL (now has expiry)
    }

    @Test
    void fullCommandCoverageViaCommandsMethod() {
        // Demonstrates using commands() to access ALL Redis async commands
        // This provides complete coverage of 400+ Redis commands
        TransactionBuilder<String, String> txn = connection.transaction();

        // Use commands() for any Redis command, including those not directly on TransactionBuilder
        txn.commands().set("full-coverage-key1", "value1");
        txn.commands().hset("full-coverage-hash", "field1", "hashvalue");
        txn.commands().lpush("full-coverage-list", "listvalue1", "listvalue2");
        txn.commands().pfadd("full-coverage-hll", "elem1", "elem2"); // HyperLogLog - not on builder
        txn.commands().setex("full-coverage-expiring", 60, "tempvalue"); // Not explicitly on builder

        TransactionResult result = txn.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(5);
        assertThat((String) result.get(0)).isEqualTo("OK"); // SET
        assertThat((Boolean) result.get(1)).isTrue(); // HSET
        assertThat((Long) result.get(2)).isEqualTo(2L); // LPUSH returns new length
        assertThat((Long) result.get(3)).isEqualTo(1L); // PFADD returns 1 if HLL changed
        assertThat((String) result.get(4)).isEqualTo("OK"); // SETEX
    }

    @Test
    void concurrentTransactionsDoNotInterfere() throws Exception {
        // Design: Each thread executes transactions that SET then GET multiple keys with
        // thread-specific values. If transactions interfere, a GET would return another
        // thread's value or results would be in wrong order, failing assertions immediately.
        int numThreads = 10;
        int transactionsPerThread = 50;
        int keysPerTransaction = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicBoolean hasError = new AtomicBoolean(false);
        StringBuilder errorMessages = new StringBuilder();

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start simultaneously
                    for (int txnNum = 0; txnNum < transactionsPerThread; txnNum++) {
                        String prefix = "t" + threadId + "_tx" + txnNum + "_";
                        TransactionBuilder<String, String> builder = connection.transaction();

                        // SET keys with thread-specific values
                        for (int k = 0; k < keysPerTransaction; k++) {
                            String key = prefix + "key" + k;
                            String val = prefix + "val" + k;
                            builder.commands().set(key, val);
                        }
                        // GET the same keys - values must match what we just set
                        for (int k = 0; k < keysPerTransaction; k++) {
                            String key = prefix + "key" + k;
                            builder.commands().get(key);
                        }

                        TransactionResult result = builder.execute();

                        // Verify results
                        if (result.wasDiscarded()) {
                            throw new AssertionError("Transaction was unexpectedly discarded: " + prefix);
                        }
                        if (result.size() != keysPerTransaction * 2) {
                            throw new AssertionError("Wrong result size for " + prefix + ": expected "
                                    + (keysPerTransaction * 2) + " got " + result.size());
                        }

                        // First N results are "OK" from SET
                        for (int k = 0; k < keysPerTransaction; k++) {
                            if (!"OK".equals(result.get(k))) {
                                throw new AssertionError("SET result not OK for " + prefix + "key" + k);
                            }
                        }
                        // Next N results are values from GET - must match our thread's values
                        for (int k = 0; k < keysPerTransaction; k++) {
                            String expected = prefix + "val" + k;
                            String actual = (String) result.get(keysPerTransaction + k);
                            if (!expected.equals(actual)) {
                                throw new AssertionError("GET mismatch for " + prefix + "key" + k + ": expected '" + expected
                                        + "' but got '" + actual + "'");
                            }
                        }
                    }
                } catch (Exception e) {
                    hasError.set(true);
                    synchronized (errorMessages) {
                        errorMessages.append("Thread ").append(threadId).append(": ").append(e.getMessage()).append("\n");
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("All threads should complete within timeout").isTrue();
        assertThat(hasError.get()).as("No errors: " + errorMessages).isFalse();
    }

}
