/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;

/**
 * Runs {@link RedisTimeSeriesIntegrationTests} on a RESP2 connection.
 */
@Tag(INTEGRATION_TEST)
public class RedisTimeSeriesResp2IntegrationTests extends RedisTimeSeriesIntegrationTests {

    @Inject
    RedisTimeSeriesResp2IntegrationTests(RedisClient client) {
        super(connectWithResp2(client));
    }

    private static RedisCommands<String, String> connectWithResp2(RedisClient client) {
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        return client.connect().sync();
    }

}
