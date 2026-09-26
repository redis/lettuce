/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TsDuplicatePolicy}.
 * <p>
 * PLAN: Given each of the 6 documented policies, when {@code toString()} is read, then the wire value equals the enum constant
 * name (no dotted/aliased values here, unlike {@link TsAggregationType}).
 */
@Tag(UNIT_TEST)
class TsDuplicatePolicyUnitTests {

    @Test
    void shouldExposeSixValues() {
        assertThat(TsDuplicatePolicy.values()).hasSize(6);
    }

    @Test
    void shouldRenderWireValues() {
        assertThat(TsDuplicatePolicy.BLOCK.toString()).isEqualTo("BLOCK");
        assertThat(TsDuplicatePolicy.FIRST.toString()).isEqualTo("FIRST");
        assertThat(TsDuplicatePolicy.LAST.toString()).isEqualTo("LAST");
        assertThat(TsDuplicatePolicy.MIN.toString()).isEqualTo("MIN");
        assertThat(TsDuplicatePolicy.MAX.toString()).isEqualTo("MAX");
        assertThat(TsDuplicatePolicy.SUM.toString()).isEqualTo("SUM");
    }

    // ---------------------------------------------------------------------------
    // Given: a wire-format string from TS.INFO (server sends lowercase), When: fromWire, Then: round-trips
    // ---------------------------------------------------------------------------

    @Test
    void fromWireRoundTripsLowercaseValue() {
        assertThat(TsDuplicatePolicy.fromWire("last")).isEqualTo(TsDuplicatePolicy.LAST);
        assertThat(TsDuplicatePolicy.fromWire("block")).isEqualTo(TsDuplicatePolicy.BLOCK);
    }

    @Test
    void fromWireRoundTripsUppercaseValue() {
        assertThat(TsDuplicatePolicy.fromWire("LAST")).isEqualTo(TsDuplicatePolicy.LAST);
    }

    @Test
    void fromWireReturnsNullForUnknownValue() {
        assertThat(TsDuplicatePolicy.fromWire("not-a-real-policy")).isNull();
    }

    @Test
    void fromWireReturnsNullForNull() {
        assertThat(TsDuplicatePolicy.fromWire(null)).isNull();
    }

}
