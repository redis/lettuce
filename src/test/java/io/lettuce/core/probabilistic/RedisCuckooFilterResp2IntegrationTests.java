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
import io.lettuce.core.probabilistic.arguments.CfReserveArgs;
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
 * Under RESP2, the server sends the raw integer {@code -1} for filter-full items. The client maps that to {@code null}
 * (distinct from {@code false} which represents "already exists"). Under RESP3, redis-stack encodes {@code -1} as a boolean
 * {@code false}, so the parent-class tests assert {@code Boolean.FALSE} for filter-full. This subclass overrides the
 * filter-full test to assert {@code null} as expected under RESP2.
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

    /**
     * RESP2 override: verifies that CF.INSERT maps per-item {@code -1} (filter full) to {@code null}.
     *
     * <p>
     * Under RESP2 the server transmits the raw integer {@code -1}, which the output class maps to {@code null} — keeping it
     * distinct from {@code false} ("already exists"). The parent-class test covers the RESP3 path where the server encodes
     * {@code -1} as {@code false}.
     */
    @Test
    @Override
    void cfInsertReturnsFalseWhenFilterIsFull() {
        String key = "cf:full:resp2:insert";
        redis.cfReserve(key, 1000, CfReserveArgs.Builder.bucketSize(1).expansion(0));

        List<Boolean> result = redis.cfInsert(key, "W", "W", "W", "W", "W", "W");

        assertThat(result).hasSize(6);
        assertThat(result).startsWith(true, true);
        for (int i = 2; i < result.size(); i++) {
            assertThat(result.get(i)).as("result[%d] must be null (filter full = -1 under RESP2)", i).isNull();
        }
    }

}
