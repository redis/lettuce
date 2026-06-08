/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cf;

import javax.inject.Inject;
import java.util.List;

import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.cf.arguments.CfInsertArgs;
import io.lettuce.core.cf.arguments.CfReserveArgs;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reactive integration tests for Redis Cuckoo Filter commands. Re-runs all tests from {@link RedisCuckooFilterIntegrationTests}
 * routing every call through the reactive API via {@link ReactiveSyncInvocationHandler}.
 * <p>
 * Tests the reactive API returns {@code Flux<Value<V>>} are overridden. (wrapping nulls in {@link Value#empty()}) while the
 * sync API returns {@code List<V>} with raw booleans.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
public class RedisCuckooFilterReactiveIntegrationTests extends RedisCuckooFilterIntegrationTests {

    private final RedisReactiveCommands<String, String> reactive;

    @Inject
    public RedisCuckooFilterReactiveIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.reactive();
    }

    @Test
    @Override
    void cfInsert() {
        StepVerifier.create(reactive.cfInsert("books:name", "Dune", "Dune Messiah")).expectNext(Value.just(Boolean.TRUE))
                .expectNext(Value.just(Boolean.TRUE)).verifyComplete();
    }

    @Test
    @Override
    void cfInsertNx() {
        reactive.cfAdd("books:name", "Dune").block();

        StepVerifier.create(reactive.cfInsertNx("books:name", "Dune", "Dune Messiah")).expectNext(Value.just(Boolean.FALSE))
                .expectNext(Value.just(Boolean.TRUE)).verifyComplete();
    }

    @Test
    @Override
    void cfInsertWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(100);

        StepVerifier.create(reactive.cfInsert("books:name", insertArgs, "Dune", "Dune Messiah"))
                .expectNext(Value.just(Boolean.TRUE)).expectNext(Value.just(Boolean.TRUE)).verifyComplete();
    }

    @Test
    @Override
    void cfInsertNxWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(100);
        reactive.cfAdd("books:name", "Dune").block();

        StepVerifier.create(reactive.cfInsertNx("books:name", insertArgs, "Dune", "Dune Messiah"))
                .expectNext(Value.just(Boolean.FALSE)).expectNext(Value.just(Boolean.TRUE)).verifyComplete();
    }

    /**
     * Reactive counterpart of {@link RedisCuckooFilterIntegrationTests#cfInsertNxDistinguishesAlreadyExistsFromFilterFull()}.
     *
     * <p>
     * Verifies that CF.INSERTNX emits {@code Value.just(false)} for "already exists" (0), NOT {@code Value.empty()} which
     * represents "filter full" (-1). The -1 → Value.empty() mapping is verified by
     * {@link RedisCuckooFilterResp2IntegrationTests#cfInsertReturnsNullWhenFilterIsFull()} (RESP2) and
     * CuckooInsertBooleanValueListOutputUnitTests (unit tests).
     */
    @Test
    @Override
    void cfInsertNxDistinguishesAlreadyExistsFromFilterFull() {
        String key = "cf:full:reactive:insertnx";
        reactive.cfReserve(key, 100, CfReserveArgs.Builder.bucketSize(2).expansion(1)).block();

        reactive.cfAdd(key, "known").block();

        // existing item must return Value.just(false), NOT Value.empty() — 0 and -1 must remain distinguishable
        List<Value<Boolean>> existing = reactive.cfInsertNx(key, "known").collectList().block();
        assertThat(existing).isNotNull().hasSize(1);
        assertThat(existing.get(0)).isEqualTo(Value.just(Boolean.FALSE));
    }

}
