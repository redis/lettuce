/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.commands;

import javax.enterprise.inject.New;
import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.api.sync.RedisCommands;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Runs the {@code HIMPORT} command-flow tests under the RESP2 protocol.
 */
@Tag(INTEGRATION_TEST)
class HashImportResp2IntegrationTests extends HashImportIntegrationTests {

    @Inject
    HashImportResp2IntegrationTests(@New RedisClient client) {
        super(connectResp2(client));
    }

    private static RedisCommands<String, String> connectResp2(RedisClient client) {
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        return client.connect().sync();
    }

}
