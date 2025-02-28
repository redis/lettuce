/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.lettuce.core.search;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration tests for Redis JSON indexing functionality using RESP2 protocol.
 * <p>
 * This test class extends {@link RedisJsonIndexingIntegrationTests} and runs all the same tests but using the RESP2 protocol
 * instead of the default RESP3 protocol.
 * <p>
 * The tests verify that Redis JSON indexing functionality works correctly with both RESP2 and RESP3 protocols, ensuring
 * backward compatibility and protocol-agnostic behavior.
 * <p>
 * Based on the Redis documentation tutorial: https://redis.io/docs/latest/develop/interact/search-and-query/indexing/
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
public class RedisJsonIndexingResp2IntegrationTests extends RedisJsonIndexingIntegrationTests {

    @Override
    protected ClientOptions getOptions() {
        return ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
    }

}
