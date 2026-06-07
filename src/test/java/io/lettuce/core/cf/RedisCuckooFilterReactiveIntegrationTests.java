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
 * @author Yordan Tsintsov
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
     * Reactive counterpart of {@link RedisCuckooFilterIntegrationTests#cfInsertReturnsNullWhenFilterIsFull()}.
     *
     * <p>
     * Verifies that CF.INSERT emits the correct {@link Value} wrappers for a full filter. Redis returns {@code -1} per-item
     * when the filter is full, which maps to {@code Value.empty()}. The first two insertions succeed ({@code Value.just(true)})
     * while the remaining ones return {@code -1} and must map to {@code Value.empty()}.
     */
    @Test
    @Override
    void cfInsertReturnsNullWhenFilterIsFull() {
        String key = "cf:full:reactive:insert";
        // BUCKETSIZE 1 EXPANSION 0: same value can appear at most 2*BUCKETSIZE=2 times; expansion disabled
        reactive.cfReserve(key, 1000, CfReserveArgs.Builder.bucketSize(1).expansion(0)).block();

        List<Value<Boolean>> result = reactive.cfInsert(key, "W", "W", "W", "W", "W", "W").collectList().block();

        assertThat(result).isNotNull().hasSize(6);
        assertThat(result.get(0)).isEqualTo(Value.just(Boolean.TRUE));
        assertThat(result.get(1)).isEqualTo(Value.just(Boolean.TRUE));
        // Elements 2-5: filter full, server returns -1, which must map to Value.empty() (distinct from Value.just(false))
        for (int i = 2; i < result.size(); i++) {
            assertThat(result.get(i)).as("result[%d] must be Value.empty() (filter full = -1)", i).isEqualTo(Value.empty());
        }
    }

    /**
     * Reactive counterpart of {@link RedisCuckooFilterIntegrationTests#cfInsertNxDistinguishesAlreadyExistsFromFilterFull()}.
     *
     * <p>
     * Verifies that CF.INSERTNX emits {@code Value.just(false)} for "already exists" (0), NOT {@code Value.empty()} which
     * represents "filter full" (-1). The -1 → Value.empty() mapping is verified by
     * {@link #cfInsertReturnsNullWhenFilterIsFull()}.
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
