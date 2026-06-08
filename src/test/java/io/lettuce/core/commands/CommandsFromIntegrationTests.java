/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.TestSupport;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.test.LettuceExtension;
import reactor.test.StepVerifier;

/**
 * Integration tests demonstrating the static {@code from()} factory methods on command interfaces. These methods provide an
 * alternative way to obtain command API instances from a connection, with automatic caching via {@code CommandsRegistry}.
 *
 * @see RedisAsyncCommands#from(StatefulRedisConnection)
 * @see RedisReactiveCommands#from(StatefulRedisConnection)
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class CommandsFromIntegrationTests extends TestSupport {

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    CommandsFromIntegrationTests(StatefulRedisConnection<String, String> connection) {
        this.connection = connection;
    }

    @BeforeEach
    void setUp() {
        connection.sync().flushdb();
    }

    @Test
    void asyncCommandsFromConnection() {
        RedisAsyncCommands<String, String> async = RedisAsyncCommands.from(connection);

        String result = async.set(key, value).toCompletableFuture().join();
        assertThat(result).isEqualTo("OK");

        String retrieved = async.get(key).toCompletableFuture().join();
        assertThat(retrieved).isEqualTo(value);
    }

    @Test
    void reactiveCommandsFromConnection() {
        RedisReactiveCommands<String, String> reactive = RedisReactiveCommands.from(connection);

        StepVerifier.create(reactive.set(key, value)).expectNext("OK").verifyComplete();

        StepVerifier.create(reactive.get(key)).expectNext(value).verifyComplete();
    }

    @Test
    void fromMethodReturnsSameInstanceAsConnectionMethod() {
        // Both ways of obtaining commands should return the same cached instance
        RedisAsyncCommands<String, String> fromStatic = RedisAsyncCommands.from(connection);
        RedisAsyncCommands<String, String> fromConnection = connection.async();

        assertThat(fromStatic).isSameAs(fromConnection);
    }

    @Test
    void fromMethodReturnsCachedInstance() {
        RedisAsyncCommands<String, String> first = RedisAsyncCommands.from(connection);
        RedisAsyncCommands<String, String> second = RedisAsyncCommands.from(connection);

        assertThat(first).isSameAs(second);
    }

    @Test
    void differentCommandTypesFromSameConnection() {
        RedisAsyncCommands<String, String> async = RedisAsyncCommands.from(connection);
        RedisReactiveCommands<String, String> reactive = RedisReactiveCommands.from(connection);

        // Set via async
        async.set(key, value).toCompletableFuture().join();

        // Get via reactive
        StepVerifier.create(reactive.get(key)).expectNext(value).verifyComplete();

        // Both should reference the same underlying connection
        assertThat(async.getStatefulConnection()).isSameAs(reactive.getStatefulConnection());
    }

    @Test
    void commandsWorkAfterReconnect() throws Exception {
        RedisAsyncCommands<String, String> async = RedisAsyncCommands.from(connection);

        async.set(key, value).toCompletableFuture().join();
        assertThat(async.get(key).toCompletableFuture().join()).isEqualTo(value);

        // Force reconnect
        async.quit().toCompletableFuture().join();
        Thread.sleep(100);

        // Commands should still work after reconnect
        async.set("key2", "value2").toCompletableFuture().join();
        assertThat(async.get("key2").toCompletableFuture().join()).isEqualTo("value2");
    }

}
