/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import javax.inject.Inject;
import java.util.List;

import io.lettuce.core.Value;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.probabilistic.arguments.CfInsertArgs;
import io.lettuce.core.probabilistic.arguments.CfReserveArgs;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reactive integration tests for Redis Cuckoo Filter commands. Re-runs all tests from {@link RedisCuckooFilterIntegrationTests}
 * routing every call through the reactive API via {@link ReactiveSyncInvocationHandler}.
 *
 * <p>
 * Overrides verify reactive-specific streaming behaviour:
 * <ul>
 * <li>cfInsert: {@code Flux<Boolean>} &ndash; {@code true} if added, {@code false} if filter full</li>
 * <li>cfInsertNx: {@code Flux<Value<Boolean>>} &ndash; {@code Value.just(true)} if added, {@code Value.just(false)} if already
 * exists, {@code Value.empty()} if filter full</li>
 * </ul>
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
        StepVerifier.create(reactive.cfInsert("books:name", "Dune", "Dune Messiah")).expectNext(Boolean.TRUE)
                .expectNext(Boolean.TRUE).verifyComplete();
    }

    @Test
    @Override
    void cfInsertWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(100);

        StepVerifier.create(reactive.cfInsert("books:name", insertArgs, "Dune", "Dune Messiah")).expectNext(Boolean.TRUE)
                .expectNext(Boolean.TRUE).verifyComplete();
    }

    @Test
    @Override
    void cfInsertSingleValue() {
        StepVerifier.create(reactive.cfInsert("books:name", "Dune")).expectNext(Boolean.TRUE).verifyComplete();
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
    void cfInsertNxWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(100);
        reactive.cfAdd("books:name", "Dune").block();

        StepVerifier.create(reactive.cfInsertNx("books:name", insertArgs, "Dune", "Dune Messiah"))
                .expectNext(Value.just(Boolean.FALSE)).expectNext(Value.just(Boolean.TRUE)).verifyComplete();
    }

    @Test
    @Override
    void cfInsertNxSingleValue() {
        StepVerifier.create(reactive.cfInsertNx("books:name", "Dune")).expectNext(Value.just(Boolean.TRUE)).verifyComplete();
    }

    /**
     * Reactive counterpart of {@link RedisCuckooFilterIntegrationTests#cfInsertNxDistinguishesAlreadyExistsFromFilterFull()}.
     *
     * <p>
     * Verifies that CF.INSERTNX emits {@code Value.just(false)} for "already exists" (0), NOT {@code Value.empty()} which
     * represents "filter full" (-1).
     */
    @Test
    @Override
    void cfInsertNxDistinguishesAlreadyExistsFromFilterFull() {
        String key = "cf:full:reactive:insertnx";
        reactive.cfReserve(key, 100, CfReserveArgs.Builder.bucketSize(2).expansion(1)).block();

        reactive.cfAdd(key, "known").block();

        List<Value<Boolean>> existing = reactive.cfInsertNx(key, "known").collectList().block();
        assertThat(existing).isNotNull().hasSize(1);
        assertThat(existing.get(0)).isEqualTo(Value.just(Boolean.FALSE));
    }

    /**
     * Reactive counterpart of {@link RedisCuckooFilterIntegrationTests#cfInsertReturnsFalseWhenFilterIsFull()}.
     *
     * <p>
     * CF.INSERT uses {@link io.lettuce.core.output.ErrorTolerantBooleanListOutput} which maps any non-{@code 1} integer to
     * {@code false}. The reactive API emits plain {@code Flux<Boolean>}, so filter-full slots are {@code false}.
     */
    @Test
    @Override
    void cfInsertReturnsFalseWhenFilterIsFull() {
        String key = "cf:full:reactive:insert";
        reactive.cfReserve(key, 1000, CfReserveArgs.Builder.bucketSize(1).expansion(0)).block();

        List<Boolean> result = reactive.cfInsert(key, "W", "W", "W", "W", "W", "W").collectList().block();

        assertThat(result).isNotNull().hasSize(6);
        assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
        assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
        for (int i = 2; i < result.size(); i++) {
            assertThat(result.get(i)).as("result[%d] must be false (filter full)", i).isEqualTo(Boolean.FALSE);
        }
    }

}
