/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cf;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import org.junit.jupiter.api.Tag;

import javax.inject.Inject;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * RESP2 integration tests for Redis Cuckoo Filter commands. Re-runs all tests from {@link RedisCuckooFilterIntegrationTests}
 * using RESP2.
 *
 * @author Yordan Tsintsov
 * @since 7.7
 */
@Tag(INTEGRATION_TEST)
public class RedisCuckooFilterResp2IntegrationTests extends RedisCuckooFilterIntegrationTests {

    @Inject
    RedisCuckooFilterResp2IntegrationTests(RedisClient client) {
        super(connectWithResp2(client));
    }

    private static RedisCommands<String, String> connectWithResp2(RedisClient client) {
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        return client.connect().sync();
    }

}
