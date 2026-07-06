/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic.topk;

import io.lettuce.core.probabilistic.IncrementPair;
import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import javax.inject.Inject;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Reactive integration tests for Redis Top-K commands. Re-runs all tests from {@link RedisTopKIntegrationTests} routing every
 * call through the reactive API via {@link ReactiveSyncInvocationHandler}.
 * <p>
 * Tests the reactive API returns {@code Flux<Value<V>>} are overridden. (wrapping nulls in {@link Value#empty()}) while the
 * sync API returns {@code List<V>} with raw nulls.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
public class RedisTopKReactiveIntegrationTests extends RedisTopKIntegrationTests {

    private final RedisReactiveCommands<String, String> reactive;

    @Inject
    public RedisTopKReactiveIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.reactive();
    }

    @Test
    @Override
    void topKAdd() {
        reactive.topKReserve(MY_KEY, 3).block();

        StepVerifier.create(reactive.topKAdd(MY_KEY, MY_VALUE)).expectNext(Value.empty()).verifyComplete();
        StepVerifier.create(reactive.topKQuery(MY_KEY, MY_VALUE)).expectNext(Boolean.TRUE).verifyComplete();
    }

    @Test
    @Override
    void topKAddVararg() {
        reactive.topKReserve(MY_KEY, 3).block();

        StepVerifier.create(reactive.topKAdd(MY_KEY, MY_VALUE, MY_VALUE_2)).expectNext(Value.empty()).expectNext(Value.empty())
                .verifyComplete();
        StepVerifier.create(reactive.topKQuery(MY_KEY, MY_VALUE, MY_VALUE_2)).expectNext(Boolean.TRUE).expectNext(Boolean.TRUE)
                .verifyComplete();
    }

    @Test
    @Override
    void topKIncrBy() {
        reactive.topKReserve(MY_KEY, 3).block();

        StepVerifier.create(reactive.topKIncrBy(MY_KEY, MY_VALUE, 3L)).expectNext(Value.empty()).verifyComplete();
        StepVerifier.create(reactive.topKQuery(MY_KEY, MY_VALUE)).expectNext(Boolean.TRUE).verifyComplete();
    }

    @Test
    @Override
    void topKIncrByVararg() {
        reactive.topKReserve(MY_KEY, 3).block();

        StepVerifier.create(reactive.topKIncrBy(MY_KEY, IncrementPair.of(MY_VALUE, 3L), IncrementPair.of(MY_VALUE_2, 5L)))
                .expectNext(Value.empty()).expectNext(Value.empty()).verifyComplete();
        StepVerifier.create(reactive.topKQuery(MY_KEY, MY_VALUE, MY_VALUE_2)).expectNext(Boolean.TRUE).expectNext(Boolean.TRUE)
                .verifyComplete();
    }

}
