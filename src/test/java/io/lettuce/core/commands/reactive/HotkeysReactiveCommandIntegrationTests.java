/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands.reactive;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.HotkeysCommandIntegrationTests;
import io.lettuce.test.ReactiveSyncInvocationHandler;

/**
 * Integration tests for {@link io.lettuce.core.api.reactive.RedisServerReactiveCommands} HOTKEYS commands.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
class HotkeysReactiveCommandIntegrationTests extends HotkeysCommandIntegrationTests {

    @Inject
    HotkeysReactiveCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }

}
