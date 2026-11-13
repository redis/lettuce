/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.TestSupport;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.ZMoveArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for the sorted-set move commands through the asynchronous API, in a transaction and in a pipeline.
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class SortedSetMoveCommandIntegrationTests extends TestSupport {

    private static final String SRC = "{z}s";

    private static final String DST = "{z}d";

    private final RedisClient client;

    private final StatefulRedisConnection<String, String> connection;

    @Inject
    SortedSetMoveCommandIntegrationTests(RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.client = client;
        this.connection = connection;
    }

    @BeforeEach
    void setUp() {
        connection.sync().flushall();
    }

    private void fixtureF1(RedisCommands<String, String> redis) {
        redis.zadd(SRC, 1.0, "a", 2.0, "b", 3.0, "c");
    }

    private static <T> T await(RedisFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS);
    }

    /**
     * Scenario ZMOV-C01: through the asynchronous API a missing source completes the future with {@code null}; moving the two
     * lowest members completes it with them in ascending order.
     */
    @Test
    @EnabledOnCommand("ZMOVE")
    void zmoveAsync() throws Exception {
        RedisAsyncCommands<String, String> async = connection.async();

        assertThat(await(async.zmove(SRC, DST, ZMoveArgs.Builder.min()))).isNull();

        fixtureF1(connection.sync());

        assertThat(await(async.zmove(SRC, DST, ZMoveArgs.Builder.min().count(2)))).containsExactly(ScoredValue.just(1.0, "a"),
                ScoredValue.just(2.0, "b"));
    }

    /**
     * Scenario ZMOV-C05: in a transaction each move's result is in the transaction result, {@code null} for the move that
     * selected nothing, on a RESP3 and on a RESP2 connection.
     */
    @Test
    @EnabledOnCommand("ZMOVEMEMBER")
    void movesInTransaction() {
        try (StatefulRedisConnection<String, String> tx = client.connect()) {
            assertMovesInTransaction(tx.sync());
        }

        RedisClient resp2Client = newClient(ProtocolVersion.RESP2);
        try (StatefulRedisConnection<String, String> tx = resp2Client.connect()) {
            tx.sync().flushall();
            assertMovesInTransaction(tx.sync());
        } finally {
            FastShutdown.shutdown(resp2Client);
        }
    }

    private void assertMovesInTransaction(RedisCommands<String, String> redis) {
        fixtureF1(redis);

        redis.multi();
        redis.zmove(SRC, DST, ZMoveArgs.Builder.min());
        redis.zmovemember(SRC, DST, "c");
        redis.zmove(SRC, DST, ZMoveArgs.Builder.byScore(Range.create(5, 6)));
        TransactionResult result = redis.exec();

        assertThat(result.wasDiscarded()).isFalse();
        assertThat(result).hasSize(3);
        assertThat(result.<List<ScoredValue<String>>> get(0)).containsExactly(ScoredValue.just(1.0, "a"));
        assertThat(result.<List<ScoredValue<String>>> get(1)).containsExactly(ScoredValue.just(3.0, "c"));
        assertThat((Object) result.get(2)).isNull();
    }

    private static RedisClient newClient(ProtocolVersion protocolVersion) {
        RedisURI.Builder builder = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port());
        if (TestSettings.tls()) {
            builder.withSsl(true).withVerifyPeer(false);
        }
        Endpoints.Endpoint endpoint = TestSettings.endpoint();
        if (endpoint != null && endpoint.getPassword() != null && !endpoint.getPassword().isEmpty()) {
            builder.withAuthentication(TestSettings.username(), endpoint.getPassword());
        }
        RedisClient client = RedisClient.create(TestClientResources.get(), builder.build());
        client.setOptions(ClientOptions.builder().protocolVersion(protocolVersion).build());
        return client;
    }

    /**
     * Scenario ZMOV-C06: pipelined moves complete in order, the last one with {@code null} once the source is gone.
     */
    @Test
    @EnabledOnCommand("ZMOVE")
    void movesInPipeline() throws Exception {
        try (StatefulRedisConnection<String, String> pipeline = client.connect()) {
            fixtureF1(pipeline.sync());

            pipeline.setAutoFlushCommands(false);
            RedisAsyncCommands<String, String> async = pipeline.async();
            RedisFuture<List<ScoredValue<String>>> first = async.zmove(SRC, DST, ZMoveArgs.Builder.min());
            RedisFuture<List<ScoredValue<String>>> second = async.zmove(SRC, DST, ZMoveArgs.Builder.min());
            RedisFuture<List<ScoredValue<String>>> third = async.zmove(SRC, DST, ZMoveArgs.Builder.min());
            RedisFuture<List<ScoredValue<String>>> fourth = async.zmove(SRC, DST, ZMoveArgs.Builder.min());
            pipeline.flushCommands();

            assertThat(await(first)).containsExactly(ScoredValue.just(1.0, "a"));
            assertThat(await(second)).containsExactly(ScoredValue.just(2.0, "b"));
            assertThat(await(third)).containsExactly(ScoredValue.just(3.0, "c"));
            assertThat(await(fourth)).isNull();
        }
    }

}
