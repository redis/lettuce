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
 * Integration tests for Redis Vector Search functionality using FT.SEARCH command with vector fields and RESP2 protocol.
 * <p>
 * This test class extends {@link RediSearchVectorIntegrationTests} and runs all the same tests but using the RESP2 protocol
 * instead of the default RESP3 protocol.
 * <p>
 * The tests verify that Redis Vector Search functionality works correctly with both RESP2 and RESP3 protocols, ensuring
 * backward compatibility and protocol-agnostic behavior for vector operations including:
 * <ul>
 * <li>FLAT and HNSW vector index creation and management</li>
 * <li>KNN (k-nearest neighbor) vector searches with various parameters</li>
 * <li>Vector range queries with distance thresholds</li>
 * <li>Vector search with metadata filtering (text, numeric, tag fields)</li>
 * <li>Different distance metrics (L2, COSINE, IP)</li>
 * <li>Various vector types (FLOAT32, FLOAT64) and precision handling</li>
 * <li>JSON vector storage and retrieval as arrays</li>
 * <li>Advanced vector search features like hybrid policies and runtime parameters</li>
 * <li>Vector search error handling and edge cases</li>
 * <li>Runtime query parameters (EF_RUNTIME, EPSILON, BATCH_SIZE, HYBRID_POLICY)</li>
 * </ul>
 * <p>
 * These tests are based on the examples from the Redis documentation:
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/vectors/">Vector Search</a>
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
public class RediSearchVectorResp2IntegrationTests extends RediSearchVectorIntegrationTests {

    @Override
    protected ClientOptions getOptions() {
        return ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
    }

}
