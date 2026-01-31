/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands.transactional;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import javax.inject.Inject;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.commands.HotkeysCommandIntegrationTests;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisServerCommands} HOTKEYS commands in transactional mode.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
@Disabled("HOTKEYS not yet supported in transactions - https://github.com/redis/redis/pull/14756")
class HotkeysTxCommandIntegrationTests extends HotkeysCommandIntegrationTests {

    @Inject
    HotkeysTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }

}
