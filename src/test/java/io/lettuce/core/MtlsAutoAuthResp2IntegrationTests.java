/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.protocol.ProtocolVersion;

/**
 * Integration tests for Redis 8.6+ mTLS automatic authentication using RESP2 protocol.
 * <p>
 * This class extends {@link MtlsAutoAuthIntegrationTests} to verify that mTLS auto-authentication works correctly with the
 * RESP2 protocol (which uses AUTH command instead of HELLO).
 *
 * @author Aleksandar Todorov
 */
class MtlsAutoAuthResp2IntegrationTests extends MtlsAutoAuthIntegrationTests {

    @Override
    protected ClientOptions.Builder clientOptionsBuilder(SslOptions sslOptions) {
        return super.clientOptionsBuilder(sslOptions).protocolVersion(ProtocolVersion.RESP2);
    }

}
