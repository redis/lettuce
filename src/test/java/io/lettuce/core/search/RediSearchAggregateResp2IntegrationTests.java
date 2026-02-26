/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.junit.jupiter.api.Tag;

import static io.lettuce.TestTags.INTEGRATION_TEST;

/**
 * Integration tests for Redis FT.AGGREGATE command with RESP2 protocol.
 * <p>
 * This test class extends {@link RediSearchAggregateIntegrationTests} and runs all the same tests but using the RESP2 protocol
 * instead of the default RESP3 protocol.
 * <p>
 * The tests verify that Redis Search aggregation functionality, including cursor-based pagination, works correctly with both
 * RESP2 and RESP3 protocols, ensuring backward compatibility and protocol-agnostic behavior.
 * <p>
 * This includes comprehensive testing of:
 * <ul>
 * <li>Basic aggregation operations with RESP2</li>
 * <li>FT.CURSOR READ and FT.CURSOR DEL commands with RESP2</li>
 * <li>Cursor-based pagination with different read sizes and timeouts</li>
 * <li>Complex aggregation operations (GROUPBY, SORTBY, APPLY, FILTER) with cursors</li>
 * <li>Edge cases like empty results and cursor cleanup</li>
 * </ul>
 *
 * @author Tihomir Mateev
 * @see RediSearchAggregateIntegrationTests
 * @see RediSearchResp2IntegrationTests
 */
@Tag(INTEGRATION_TEST)
public class RediSearchAggregateResp2IntegrationTests extends RediSearchAggregateIntegrationTests {

    @Override
    protected ClientOptions getOptions() {
        return ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build();
    }

}
