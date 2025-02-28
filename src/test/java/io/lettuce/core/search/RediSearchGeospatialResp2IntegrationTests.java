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

import static io.lettuce.TestTags.INTEGRATION_TEST;

import org.junit.jupiter.api.Tag;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;

/**
 * Integration tests for Redis Search geospatial functionality using GEO and GEOSHAPE fields with RESP2 protocol.
 * <p>
 * This test class extends {@link RediSearchGeospatialIntegrationTests} and runs all the same tests but using the RESP2 protocol
 * instead of the default RESP3 protocol.
 * <p>
 * The tests verify that Redis Search geospatial functionality works correctly with both RESP2 and RESP3 protocols, ensuring
 * backward compatibility and protocol-agnostic behavior for geospatial operations including:
 * <ul>
 * <li>GEO fields for simple longitude-latitude point storage and radius queries</li>
 * <li>GEOSHAPE fields for advanced point and polygon storage with spatial relationship queries</li>
 * <li>Geographical coordinates (spherical) and Cartesian coordinates (flat)</li>
 * <li>Spatial relationship queries: WITHIN, CONTAINS, INTERSECTS, DISJOINT</li>
 * <li>Point-in-polygon and polygon-polygon spatial operations</li>
 * <li>Well-Known Text (WKT) format support for POINT and POLYGON primitives</li>
 * <li>Complex geospatial queries combining multiple field types</li>
 * <li>Different distance units (km, mi, m) and coordinate systems</li>
 * <li>Geospatial error handling and edge cases</li>
 * </ul>
 * <p>
 * These tests are based on the examples from the Redis documentation:
 * <a href="https://redis.io/docs/latest/develop/interact/search-and-query/advanced-concepts/geo/">Geospatial</a>
 *
 * @author Tihomir Mateev
 */
@Tag(INTEGRATION_TEST)
public class RediSearchGeospatialResp2IntegrationTests extends RediSearchGeospatialIntegrationTests {

    @Override
    protected ClientOptions getOptions() {
        return ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
    }

}
