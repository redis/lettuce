/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands.reactive;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.HashImportIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Runs the {@code HIMPORT} command-flow tests through the reactive API.
 */
@Tag(INTEGRATION_TEST)
class HashImportReactiveCommandIntegrationTests extends HashImportIntegrationTests {

    @Inject
    HashImportReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }

}
