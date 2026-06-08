/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cf;

import javax.inject.Inject;
import java.util.List;

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
 *
 * <p>
 * Overrides that verify reactive-specific streaming behaviour (Flux) for cfInsert ({@code Flux<Boolean>}) and cfInsertNx
 * ({@code Flux<Long>}).
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

        StepVerifier.create(reactive.cfInsertNx("books:name", "Dune", "Dune Messiah")).expectNext(0L).expectNext(1L)
                .verifyComplete();
    }

    @Test
    @Override
    void cfInsertNxWithArgs() {
        CfInsertArgs insertArgs = CfInsertArgs.Builder.capacity(100);
        reactive.cfAdd("books:name", "Dune").block();

        StepVerifier.create(reactive.cfInsertNx("books:name", insertArgs, "Dune", "Dune Messiah")).expectNext(0L).expectNext(1L)
                .verifyComplete();
    }

    @Test
    @Override
    void cfInsertNxSingleValue() {
        StepVerifier.create(reactive.cfInsertNx("books:name", "Dune")).expectNext(1L).verifyComplete();
    }

    /**
     * Reactive counterpart of {@link RedisCuckooFilterIntegrationTests#cfInsertNxDistinguishesAlreadyExistsFromFilterFull()}.
     *
     * <p>
     * Verifies that CF.INSERTNX emits {@code 0L} for "already exists", distinct from {@code -1L} for "filter full".
     */
    @Test
    @Override
    void cfInsertNxDistinguishesAlreadyExistsFromFilterFull() {
        String key = "cf:full:reactive:insertnx";
        reactive.cfReserve(key, 100, CfReserveArgs.Builder.bucketSize(2).expansion(1)).block();

        reactive.cfAdd(key, "known").block();

        List<Long> existing = reactive.cfInsertNx(key, "known").collectList().block();
        assertThat(existing).isNotNull().hasSize(1);
        assertThat(existing.get(0)).isEqualTo(0L);
    }

}
