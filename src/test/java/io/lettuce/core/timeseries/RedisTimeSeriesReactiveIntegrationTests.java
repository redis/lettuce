/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * Runs {@link RedisTimeSeriesIntegrationTests} through the reactive API.
 */
@Tag(INTEGRATION_TEST)
public class RedisTimeSeriesReactiveIntegrationTests extends RedisTimeSeriesIntegrationTests {

    @Inject
    RedisTimeSeriesReactiveIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }

}
