/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.probabilistic;

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
 * <p>
 * CF.INSERT uses {@link io.lettuce.core.output.ErrorTolerantBooleanListOutput} which maps any non-{@code 1} integer to
 * {@code false}, so behavior is identical across RESP2 and RESP3. CF.INSERTNX uses
 * {@link io.lettuce.core.output.CuckooInsertBooleanListOutput} which distinguishes {@code 0} (false) from {@code -1} (null)
 * under RESP2; under RESP3 the server collapses both to boolean {@code false}.
 *
 * @author Gyumin Hwang
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
