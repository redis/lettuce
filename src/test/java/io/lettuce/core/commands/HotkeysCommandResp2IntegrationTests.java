/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import javax.inject.Inject;

import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Tag;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.protocol.ProtocolVersion;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisServerCommands} HOTKEYS commands using RESP2 protocol.
 * <p>
 * This test class extends {@link HotkeysCommandIntegrationTests} and runs all the same tests but using the RESP2 protocol
 * instead of the default RESP3 protocol.
 * <p>
 * The tests verify that HOTKEYS functionality works correctly with both RESP2 and RESP3 protocols, ensuring backward
 * compatibility and protocol-agnostic behavior.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
public class HotkeysCommandResp2IntegrationTests extends HotkeysCommandIntegrationTests {

    @Inject
    public HotkeysCommandResp2IntegrationTests(RedisClient client) {
        super(connectWithResp2(client));
    }

    private static RedisCommands<String, String> connectWithResp2(RedisClient client) {
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        return client.connect().sync();
    }

}
