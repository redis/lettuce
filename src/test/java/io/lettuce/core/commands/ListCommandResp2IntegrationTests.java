/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisListCommands} using RESP2 protocol.
 * <p>
 * Extends {@link ListCommandIntegrationTests} and runs all the same tests using the RESP2 protocol to ensure backward
 * compatibility and protocol-agnostic behavior.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
class ListCommandResp2IntegrationTests extends ListCommandIntegrationTests {

    @Inject
    public ListCommandResp2IntegrationTests(RedisClient client) {
        super(connectWithResp2(client));
    }

    private static RedisCommands<String, String> connectWithResp2(RedisClient client) {
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        return client.connect().sync();
    }

}
