/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.protocol.ProtocolVersion;

/**
 * Integration tests for Redis 8.6+ mTLS client authentication using RESP2 protocol.
 * <p>
 * This class extends {@link MtlsClientAuthIntegrationTests} to verify that mTLS client authentication works correctly with the
 * RESP2 protocol (which uses AUTH command instead of HELLO).
 *
 * @author Aleksandar Todorov
 */
class MtlsClientAuthResp2IntegrationTests extends MtlsClientAuthIntegrationTests {

    @Override
    protected ClientOptions.Builder clientOptionsBuilder(SslOptions sslOptions) {
        return super.clientOptionsBuilder(sslOptions).protocolVersion(ProtocolVersion.RESP2);
    }

}
