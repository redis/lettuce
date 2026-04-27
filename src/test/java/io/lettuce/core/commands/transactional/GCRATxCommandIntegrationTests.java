/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands.transactional;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.GCRACommandIntegrationTests;

/**
 * Integration tests for GCRA commands in transactional (MULTI/EXEC) mode.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
@Tag(INTEGRATION_TEST)
class GCRATxCommandIntegrationTests extends GCRACommandIntegrationTests {

    @Inject
    GCRATxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

}
