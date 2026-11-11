/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.protocol.ProtocolVersion;
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

}
