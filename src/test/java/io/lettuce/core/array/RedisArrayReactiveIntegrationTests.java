/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import javax.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import reactor.test.StepVerifier;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Reactive integration tests for Redis Array commands. Re-runs all tests from {@link RedisArrayIntegrationTests} routing every
 * call through the reactive API via {@link ReactiveSyncInvocationHandler}.
 * <p>
 * Tests for {@code armget} and {@code argetrange} are overridden because the reactive API returns {@code Flux<Value<V>>}
 * (wrapping nulls in {@link Value#empty()}) while the sync API returns {@code List<V>} with raw nulls.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
@Tag(INTEGRATION_TEST)
public class RedisArrayReactiveIntegrationTests extends RedisArrayIntegrationTests {

    private final RedisReactiveCommands<String, String> reactive;

    @Inject
    RedisArrayReactiveIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.reactive();
    }

    @Test
    @Override
    void armsetAndArmget() {
        Map<Long, String> map = new LinkedHashMap<>();
        map.put(0L, "a");
        map.put(5L, "b");
        map.put(10L, "c");
        redis.armset(KEY, map);

        StepVerifier.create(reactive.armget(KEY, 0, 5, 10, 3)).expectNext(Value.just("a")).expectNext(Value.just("b"))
                .expectNext(Value.just("c")).expectNext(Value.empty()).verifyComplete();
    }

    @Test
    @Override
    void argetrange() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 2, "c");
        redis.arset(KEY, 4, "e");

        StepVerifier.create(reactive.argetrange(KEY, 0, 4)).expectNext(Value.just("a")).expectNext(Value.empty())
                .expectNext(Value.just("c")).expectNext(Value.empty()).expectNext(Value.just("e")).verifyComplete();
    }

    @Test
    @Override
    void argetrangeReversed() {
        redis.arset(KEY, 0, "a");
        redis.arset(KEY, 2, "c");
        redis.arset(KEY, 4, "e");

        StepVerifier.create(reactive.argetrange(KEY, 4, 0)).expectNext(Value.just("e")).expectNext(Value.empty())
                .expectNext(Value.just("c")).expectNext(Value.empty()).expectNext(Value.just("a")).verifyComplete();
    }

}
