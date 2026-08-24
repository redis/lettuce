/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.test.ReactiveSyncInvocationHandler;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Reactive integration tests for Redis T-Digest commands. Re-runs all tests from {@link RedisTDigestIntegrationTests} routing
 * every call through the reactive API via {@link ReactiveSyncInvocationHandler}.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
public class RedisTDigestReactiveIntegrationTests extends RedisTDigestIntegrationTests {

    @Inject
    public RedisTDigestReactiveIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }

}
