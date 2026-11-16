/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.json;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * RESP2 integration tests for Redis JSON commands. Re-runs all tests from {@link RedisJsonIntegrationTests} using RESP2.
 *
 * @author Yordan Tsintsov
 * @since 7.9
 */
@Tag(INTEGRATION_TEST)
public class RedisJsonResp2IntegrationTests extends RedisJsonIntegrationTests {

    @Override
    protected ClientOptions getOptions() {
        return ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
    }

}
