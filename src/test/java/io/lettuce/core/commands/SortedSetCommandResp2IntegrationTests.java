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
 * Integration tests for {@link io.lettuce.core.api.sync.RedisSortedSetCommands} using the RESP2 protocol.
 * <p>
 * Extends {@link SortedSetCommandIntegrationTests} and runs all the same tests using RESP2 to ensure backward compatibility and
 * protocol-agnostic behavior.
 */
@Tag(INTEGRATION_TEST)
class SortedSetCommandResp2IntegrationTests extends SortedSetCommandIntegrationTests {

    @Inject
    SortedSetCommandResp2IntegrationTests(RedisClient client) {
        super(connectResp2(client));
    }

    private static RedisCommands<String, String> connectResp2(RedisClient client) {
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        return client.connect().sync();
    }

}
