/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.pubsub;

import static io.lettuce.TestTags.INTEGRATION_TEST;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;

/**
 * Subkey Notifications integration tests using RESP2.
 * <p>
 * The notification wire format is independent of the protocol version, so the inherited tests are sufficient to validate that a
 * RESP2 client receives the same notifications.
 *
 * @author Aleksandar Todorov
 */
@Tag(INTEGRATION_TEST)
class SubkeyNotificationsResp2IntegrationTests extends SubkeyNotificationsIntegrationTests {

    @Override
    protected ClientOptions getOptions() {
        return ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
    }

}
