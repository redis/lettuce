/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import javax.inject.Inject;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.test.ReactiveSyncInvocationHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Reactive integration tests for Redis Count-Min Sketch commands. Re-runs all tests from {@link RedisCMSIntegrationTests}
 * routing every call through the reactive API via {@link ReactiveSyncInvocationHandler}.
 *
 * <p>
 * Overrides verify reactive-specific streaming behavior of {@code CMS.INCRBY} and {@code CMS.QUERY}, whose reactive form is a
 * {@code Flux<Long>} emitting one count per item.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
public class RedisCMSReactiveIntegrationTests extends RedisCMSIntegrationTests {

    private final RedisReactiveCommands<String, String> reactive;

    @Inject
    public RedisCMSReactiveIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
        this.reactive = connection.reactive();
    }

    @Test
    @Override
    void cmsIncrByMultiple() {
        reactive.cmsInitByDim("sketch", 2000, 5).block();

        StepVerifier.create(reactive.cmsIncrBy("sketch", IncrementPair.of("item1", 5), IncrementPair.of("item2", 3)))
                .expectNext(5L).expectNext(3L).verifyComplete();
    }

    @Test
    @Override
    void cmsQuery() {
        reactive.cmsInitByDim("sketch", 2000, 5).block();
        reactive.cmsIncrBy("sketch", IncrementPair.of("item1", 5)).blockLast();

        StepVerifier.create(reactive.cmsQuery("sketch", "item1")).expectNext(5L).verifyComplete();
    }

    @Test
    @Override
    void cmsQueryMultiple() {
        reactive.cmsInitByDim("sketch", 2000, 5).block();
        reactive.cmsIncrBy("sketch", IncrementPair.of("item1", 5), IncrementPair.of("item2", 3)).blockLast();

        StepVerifier.create(reactive.cmsQuery("sketch", "item1", "item2")).expectNext(5L).expectNext(3L).verifyComplete();
    }

}
