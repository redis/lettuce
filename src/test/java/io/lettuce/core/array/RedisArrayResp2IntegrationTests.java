/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * RESP2 integration tests for Redis Array commands. Re-runs all tests from {@link RedisArrayIntegrationTests} using RESP2.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
@Tag(INTEGRATION_TEST)
public class RedisArrayResp2IntegrationTests extends RedisArrayIntegrationTests {

    @Inject
    RedisArrayResp2IntegrationTests(RedisClient client) {
        super(connectWithResp2(client));
    }

    private static RedisCommands<String, String> connectWithResp2(RedisClient client) {
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        return client.connect().sync();
    }

}
