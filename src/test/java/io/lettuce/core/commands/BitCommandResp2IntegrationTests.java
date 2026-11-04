/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.BitFieldArgs.unsigned;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.BitRangeIntArrayArgs;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.env.Endpoints;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * Integration tests for the bit commands of {@link io.lettuce.core.api.sync.RedisStringCommands} using the RESP2 protocol.
 * <p>
 * Extends {@link BitCommandIntegrationTests} and runs all the same tests using the RESP2 protocol.
 */
@Tag(INTEGRATION_TEST)
class BitCommandResp2IntegrationTests extends BitCommandIntegrationTests {

    private static final RedisClient RESP2_CLIENT = newClient(ProtocolVersion.RESP2);

    @Inject
    BitCommandResp2IntegrationTests() {
        super(RESP2_CLIENT, RESP2_CLIENT.connect().sync());
    }

    private static RedisClient newClient(ProtocolVersion protocolVersion) {
        RedisURI.Builder builder = RedisURI.Builder.redis(TestSettings.host(), TestSettings.port());
        if (TestSettings.tls()) {
            builder.withSsl(true).withVerifyPeer(false);
        }
        Endpoints.Endpoint endpoint = TestSettings.endpoint();
        if (endpoint != null && endpoint.getPassword() != null && !endpoint.getPassword().isEmpty()) {
            builder.withAuthentication(TestSettings.username(), endpoint.getPassword());
        }
        RedisClient client = RedisClient.create(TestClientResources.get(), builder.build());
        client.setOptions(ClientOptions.builder().protocolVersion(protocolVersion).build());
        return client;
    }

    @AfterAll
    void shutdownClient() {
        FastShutdown.shutdown(RESP2_CLIENT);
    }

    private static List<List<Long>> protocolScenarios(RedisCommands<String, String> commands) {
        commands.flushall();
        commands.bitfield("bria:f1", BitFieldArgs.Builder.set(unsigned(32), 0, 2172748161L));
        commands.bitfield("bria:f2", BitFieldArgs.Builder.set(unsigned(16), 0, 65409));

        List<List<Long>> results = new ArrayList<>();
        results.add(commands.bitrangeintarray("bria:f1", 0, -1));
        results.add(commands.bitrangeintarray("bria:missing", 0, -1));
        results.add(commands.bitrangeintarray("bria:f2", 0, -1, BitRangeIntArrayArgs.Builder.limit(1)));
        results.add(commands.bitrangeintarray("bria:f2", 0, -1, BitRangeIntArrayArgs.Builder.limit(9)));
        results.add(commands.bitrangeintarray("bria:f2", 0, -1, BitRangeIntArrayArgs.Builder.bit().limit(1)));
        results.add(commands.bitrangeintarray("bria:f2", 0, -1, BitRangeIntArrayArgs.Builder.bit().limit(9)));
        return results;
    }

    /**
     * Scenario P01: the whole value, a missing key and limited pages return identical results on RESP2 and RESP3.
     */
    @Test
    @EnabledOnCommand("BITRANGEINTARRAY")
    void bitrangeintarraySameResultOnResp2AndResp3() {

        RedisClient resp3Client = newClient(ProtocolVersion.RESP3);
        try (StatefulRedisConnection<String, String> resp2 = RESP2_CLIENT.connect();
                StatefulRedisConnection<String, String> resp3 = resp3Client.connect()) {

            List<List<Long>> onResp2 = protocolScenarios(resp2.sync());
            List<List<Long>> onResp3 = protocolScenarios(resp3.sync());

            assertThat(onResp2).isEqualTo(onResp3);
            assertThat(onResp3).containsExactly(Arrays.asList(0L, 7L, 8L, 15L, 16L, 23L, 24L, 31L), Arrays.asList(),
                    Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L), Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 15L),
                    Arrays.asList(0L), Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L));
        } finally {
            FastShutdown.shutdown(resp3Client);
        }
    }

}
