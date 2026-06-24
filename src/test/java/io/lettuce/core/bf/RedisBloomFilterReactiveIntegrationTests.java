/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.bf;

import javax.inject.Inject;

import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.bf.arguments.BfInsertArgs;
import io.lettuce.core.bf.arguments.BfReserveArgs;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Reactive integration tests for Redis Bloom Filter commands. Re-runs all tests from {@link RedisBloomFilterIntegrationTests}
 * routing every call through the reactive API via {@link ReactiveSyncInvocationHandler}.
 * <p>
 * Tests the reactive API returns {@code Flux<Value<V>>} are overridden. (wrapping nulls in {@link Value#empty()}) while the
 * sync API returns {@code List<V>} with raw nulls.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
public class RedisBloomFilterReactiveIntegrationTests extends RedisBloomFilterIntegrationTests {

    private final RedisReactiveCommands<String, String> reactive;

    @Inject
    public RedisBloomFilterReactiveIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.commands(RedisReactiveCommands.factory());
    }

    @Test
    @Override
    void bfInsertNonScaling() {
        BfInsertArgs insertArgs = BfInsertArgs.Builder.capacity(4).nonScaling();

        StepVerifier.create(reactive.bfInsert("nonscaling_err", insertArgs, "a", "b", "c")).expectNext(Value.just(Boolean.TRUE))
                .expectNext(Value.just(Boolean.TRUE)).expectNext(Value.just(Boolean.TRUE)).verifyComplete();

        StepVerifier.create(reactive.bfInsert("nonscaling_err", "d", "e")).expectNext(Value.just(Boolean.TRUE))
                .expectNext(Value.empty()).verifyComplete();
    }

    @Test
    @Override
    void bfReserveNonScaling() {
        reactive.bfReserve("nonscaling", 0.001, 2, BfReserveArgs.Builder.nonScaling()).block();

        StepVerifier.create(reactive.bfInsert("nonscaling", "a")).expectNext(Value.just(Boolean.TRUE)).verifyComplete();
        StepVerifier.create(reactive.bfInsert("nonscaling", "b")).expectNext(Value.just(Boolean.TRUE)).verifyComplete();
        StepVerifier.create(reactive.bfInsert("nonscaling", "c")).expectNext(Value.empty()).verifyComplete();
    }

}
