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

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Reactive integration tests for Redis Count-Min Sketch commands. Re-runs all tests from {@link RedisCMSIntegrationTests}
 * routing every call through the reactive API via {@link ReactiveSyncInvocationHandler}.
 *
 * <p>
 * Overrides verify reactive-specific streaming behavior of {@code CMS.INCRBY} and {@code CMS.QUERY}, whose reactive form is a
 * {@code Flux<Long>}.
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

}
