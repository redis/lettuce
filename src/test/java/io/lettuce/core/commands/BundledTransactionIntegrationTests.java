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
import io.lettuce.core.api.TransactionCommands;
import io.lettuce.core.api.reactive.ReactiveTransactionBuilder;
import io.lettuce.test.LettuceExtension;
import reactor.core.publisher.Mono;
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
        builder.queue().set(key, value);
        builder.queue().get(key);

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(2);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((String) result.get(1)).isEqualTo(value);
    }

    @Test
    void basicAsyncTransaction() throws Exception {
        TransactionBuilder<String, String> builder = connection.transaction();
        builder.queue().set(key, value);
        builder.queue().incr("counter");
        builder.queue().get(key);

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
        ReactiveTransactionBuilder<String, String> builder = connection.reactive().transaction();
        builder.queue().set(key, value);
        builder.queue().get(key);

        StepVerifier.create(builder.executeReactive()).consumeNextWith(result -> {
            assertThat(result.wasDiscarded()).isFalse();
            assertThat(result).hasSize(2);
            assertThat((String) result.get(0)).isEqualTo("OK");
            assertThat((String) result.get(1)).isEqualTo(value);
        }).verifyComplete();
    }

    @Test
    void reactiveTransactional_functionalApi() {
        // Test the functional API pattern
        StepVerifier.create(connection.reactive().transactional(txn -> {
            txn.set(key, value);
            txn.incr("counter");
            txn.get(key);
        })).consumeNextWith(result -> {
            assertThat(result.wasDiscarded()).isFalse();
            assertThat(result).hasSize(3);
            assertThat((String) result.get(0)).isEqualTo("OK");
            assertThat((Long) result.get(1)).isEqualTo(1L);
            assertThat((String) result.get(2)).isEqualTo(value);
        }).verifyComplete();
    }

    @Test
    void reactiveTransactional_withReactiveDataSources() {
        // Test composing with reactive data sources
        Mono<String> keyMono = Mono.just(key);
        Mono<String> valueMono = Mono.just(value);

        Mono<TransactionResult> result = keyMono
                .flatMap(k -> valueMono.flatMap(v -> connection.reactive().transactional(txn -> {
                    txn.set(k, v);
                    txn.get(k);
                })));

        StepVerifier.create(result).consumeNextWith(r -> {
            assertThat(r.wasDiscarded()).isFalse();
            assertThat(r).hasSize(2);
            assertThat((String) r.get(0)).isEqualTo("OK");
            assertThat((String) r.get(1)).isEqualTo(value);
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
        builder.queue().set(key, value);

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
        builder.queue().set("str1", "value1");
        builder.queue().set("str2", "value2");
        builder.queue().mget("str1", "str2");

        // Hash commands
        builder.queue().hset("hash", "field1", "val1");
        builder.queue().hget("hash", "field1");

        // List commands
        builder.queue().lpush("list", "a", "b", "c");
        builder.queue().lrange("list", 0, -1);

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
        builder.queue().incr("counter");
        builder.queue().incrby("counter", 5);
        builder.queue().decr("counter");
        builder.queue().decrby("counter", 3);
        builder.queue().get("counter");

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
                        builder.queue().incr("counter");
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
        builder.queue().zadd("zset", 1.0, "one");
        builder.queue().zadd("zset", 2.0, "two");
        builder.queue().zadd("zset", 3.0, "three");
        builder.queue().zcard("zset");
        builder.queue().zscore("zset", "two");
        builder.queue().zrange("zset", 0, -1);

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(6);
        assertThat((Long) result.get(3)).isEqualTo(3L); // ZCARD
        assertThat((Double) result.get(4)).isEqualTo(2.0); // ZSCORE
    }

    @Test
    void transactionWithSets() {
        TransactionBuilder<String, String> builder = connection.transaction();
        builder.queue().sadd("set", "a", "b", "c");
        builder.queue().scard("set");
        builder.queue().sismember("set", "b");
        builder.queue().smembers("set");
        builder.queue().srem("set", "a");

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
        builder.queue().exists(key);
        builder.queue().type(key);
        builder.queue().ttl(key);
        builder.queue().expire(key, 3600);
        builder.queue().ttl(key);

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
        txn.queue().set("full-coverage-key1", "value1");
        txn.queue().hset("full-coverage-hash", "field1", "hashvalue");
        txn.queue().lpush("full-coverage-list", "listvalue1", "listvalue2");
        txn.queue().pfadd("full-coverage-hll", "elem1", "elem2"); // HyperLogLog - not on builder
        txn.queue().setex("full-coverage-expiring", 60, "tempvalue"); // Not explicitly on builder

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
                            builder.queue().set(key, val);
                        }
                        // GET the same keys - values must match what we just set
                        for (int k = 0; k < keysPerTransaction; k++) {
                            String key = prefix + "key" + k;
                            builder.queue().get(key);
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

    @Test // WI-7/A5: functional transactional() on the sync command interface
    void functionalTransactionalSync() {
        TransactionResult result = connection.sync().transactional(txn -> {
            txn.set(key, value);
            txn.incr("counter");
        });

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(2);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((Long) result.get(1)).isEqualTo(1L);
    }

    @Test // WI-7/A5: functional transactional() on the async command interface
    void functionalTransactionalAsync() throws Exception {
        RedisFuture<TransactionResult> future = connection.async().transactional(txn -> {
            txn.set(key, value);
            txn.get(key);
        });

        TransactionResult result = future.get(5, TimeUnit.SECONDS);
        assertThat(result.wasDiscarded()).isFalse();
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((String) result.get(1)).isEqualTo(value);
    }

    @Test // WI-7/A5/D4: functional transactional() on the reactive command interface - no cast required
    void functionalTransactionalReactiveWithoutCast() {
        Mono<TransactionResult> mono = connection.reactive().transactional(txn -> {
            txn.set(key, value);
            txn.get(key);
        });

        StepVerifier.create(mono).consumeNextWith(result -> {
            assertThat(result.wasDiscarded()).isFalse();
            assertThat((String) result.get(0)).isEqualTo("OK");
            assertThat((String) result.get(1)).isEqualTo(value);
        }).verifyComplete();
    }

    @Test // WI-7/A1: acquire the transaction entry point through commands(factory); it is cached per connection
    void acquireViaCommandsFactory() {
        TransactionCommands<String, String> tx = connection.commands(TransactionCommands.factory());

        // commands(factory) caches one instance per connection
        assertThat(connection.commands(TransactionCommands.factory())).isSameAs(tx);

        TransactionResult result = tx.transactional(txn -> {
            txn.set(key, value);
            txn.incr("counter");
        });

        assertThat(result.wasDiscarded()).isFalse();
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat((Long) result.get(1)).isEqualTo(1L);

        // each create() still mints a fresh, single-use builder
        assertThat(tx.create()).isNotSameAs(tx.create());
    }

}
