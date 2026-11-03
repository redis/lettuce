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
 * Integration tests for {@link io.lettuce.core.api.sync.RedisStringCommands} using RESP2 protocol.
 * <p>
 * Extends {@link StringCommandIntegrationTests} and runs all the same tests using the RESP2 protocol to ensure backward
 * compatibility and protocol-agnostic behavior.
 */
@Tag(INTEGRATION_TEST)
class StringCommandResp2IntegrationTests extends StringCommandIntegrationTests {

    @Inject
    public StringCommandResp2IntegrationTests(RedisClient client) {
        super(connectWithResp2(client));
    }

    private static RedisCommands<String, String> connectWithResp2(RedisClient client) {
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        return client.connect().sync();
    }

}
