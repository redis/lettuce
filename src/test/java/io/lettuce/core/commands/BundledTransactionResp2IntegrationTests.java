/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import reactor.test.StepVerifier;

/**
 * Integration tests for bundled transactions using RESP2 protocol.
 * <p>
 * This test class verifies that bundled transactions work correctly with the RESP2 protocol, ensuring backward compatibility.
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
public class BundledTransactionResp2IntegrationTests extends TestSupport {

    private static RedisClient client;

    private static StatefulRedisConnection<String, String> connection;

    private static RedisCommands<String, String> sync;

    public BundledTransactionResp2IntegrationTests() {
        RedisURI redisURI = RedisURI.Builder.redis("127.0.0.1").withPort(16379).build();
        client = RedisClient.create(redisURI);
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        connection = client.connect();
        sync = connection.sync();
    }

    @AfterAll
    static void tearDown() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        sync.flushall();
    }

    @Test
    void basicSyncTransactionResp2() {
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
    void basicAsyncTransactionResp2() throws Exception {
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
    void reactiveTransactionResp2() {
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
    void transactionWithWatchResp2() throws Exception {
        // In bundled transactions, WATCH+MULTI+commands+EXEC are sent atomically.
        // Test that WATCH works correctly when no modification occurs.

        // Set initial value
        sync.set(key, "initial");

        // Create transaction with WATCH - should succeed since no modification between WATCH and EXEC
        TransactionBuilder<String, String> builder = connection.transaction(key);
        builder.commands().set(key, value);

        TransactionResult result = builder.execute();
        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(1);
        assertThat((String) result.get(0)).isEqualTo("OK");
        assertThat(sync.get(key)).isEqualTo(value);
    }

    @Test
    void multipleCommandsResp2() {
        TransactionBuilder<String, String> builder = connection.transaction();

        builder.commands().set("key1", "val1");
        builder.commands().set("key2", "val2");
        builder.commands().incr("counter");
        builder.commands().hset("hash", "field", "value");
        builder.commands().lpush("list", "a", "b");

        TransactionResult result = builder.execute();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(5);
    }

}
