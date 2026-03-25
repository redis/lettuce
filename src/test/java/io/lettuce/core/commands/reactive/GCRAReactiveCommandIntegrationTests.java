/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands.reactive;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.GCRACommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * Integration tests for GCRA commands using the reactive API.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
@Tag(INTEGRATION_TEST)
class GCRAReactiveCommandIntegrationTests extends GCRACommandIntegrationTests {

    @Inject
    GCRAReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }

}
