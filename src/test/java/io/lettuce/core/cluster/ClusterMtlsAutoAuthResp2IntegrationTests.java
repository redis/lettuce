/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.protocol.ProtocolVersion;

/**
 * Integration tests for Redis 8.6+ mTLS automatic authentication in cluster mode using RESP2 protocol.
 * <p>
 * This class extends {@link ClusterMtlsAutoAuthIntegrationTests} to verify that mTLS auto-authentication works correctly with
 * the RESP2 protocol (which uses AUTH command instead of HELLO).
 *
 * @author Aleksandar Todorov
 */
class ClusterMtlsAutoAuthResp2IntegrationTests extends ClusterMtlsAutoAuthIntegrationTests {

    @Override
    protected ClientOptions.Builder clientOptionsBuilder(SslOptions sslOptions) {
        return ClusterClientOptions.builder().sslOptions(sslOptions).protocolVersion(ProtocolVersion.RESP2);
    }

}
