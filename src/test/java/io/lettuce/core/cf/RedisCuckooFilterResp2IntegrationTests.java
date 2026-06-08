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
import io.lettuce.core.cf.arguments.CfReserveArgs;
import io.lettuce.core.protocol.ProtocolVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RESP2 integration tests for Redis Cuckoo Filter commands. Re-runs all tests from {@link RedisCuckooFilterIntegrationTests}
 * using RESP2.
 *
 * <p>
 * RESP3 differs: the server returns boolean {@code false} instead of integer {@code -1} for a full filter, so the filter-full
 * (-1 → {@code null}) distinction is only observable over RESP2. This class verifies it explicitly.
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

    @Test
    void cfInsertReturnsNullWhenFilterIsFull() {
        String key = "cf:full:resp2:insert";
        // RESP2: server returns integer -1 (filter full). BUCKETSIZE 1 EXPANSION 0: same value fits at most 2*1=2 slots.
        redis.cfReserve(key, 1000, CfReserveArgs.Builder.bucketSize(1).expansion(0));
        List<Boolean> result = redis.cfInsert(key, "W", "W", "W", "W", "W", "W");
        assertThat(result).hasSize(6);
        assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
        assertThat(result.get(1)).isEqualTo(Boolean.TRUE);
        // -1 (filter full) must map to null, distinct from false (0 = already exists)
        for (int i = 2; i < result.size(); i++) {
            assertThat(result.get(i)).as("result[%d] must be null (filter full = -1, RESP2)", i).isNull();
        }
    }

}
