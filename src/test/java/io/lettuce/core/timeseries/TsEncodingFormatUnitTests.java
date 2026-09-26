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
 * Unit tests for {@link TsEncodingFormat}.
 * <p>
 * PLAN: Given each of the 2 documented encodings, when {@code toString()} is read, then the wire value equals the enum constant
 * name.
 */
@Tag(UNIT_TEST)
class TsEncodingFormatUnitTests {

    @Test
    void shouldExposeTwoValues() {
        assertThat(TsEncodingFormat.values()).hasSize(2);
    }

    @Test
    void shouldRenderWireValues() {
        assertThat(TsEncodingFormat.COMPRESSED.toString()).isEqualTo("COMPRESSED");
        assertThat(TsEncodingFormat.UNCOMPRESSED.toString()).isEqualTo("UNCOMPRESSED");
    }

    // ---------------------------------------------------------------------------
    // Given: a wire-format string from TS.INFO (server sends lowercase), When: fromWire, Then: round-trips
    // ---------------------------------------------------------------------------

    @Test
    void fromWireRoundTripsLowercaseValue() {
        assertThat(TsEncodingFormat.fromWire("compressed")).isEqualTo(TsEncodingFormat.COMPRESSED);
        assertThat(TsEncodingFormat.fromWire("uncompressed")).isEqualTo(TsEncodingFormat.UNCOMPRESSED);
    }

    @Test
    void fromWireRoundTripsUppercaseValue() {
        assertThat(TsEncodingFormat.fromWire("COMPRESSED")).isEqualTo(TsEncodingFormat.COMPRESSED);
    }

    @Test
    void fromWireReturnsNullForUnknownValue() {
        assertThat(TsEncodingFormat.fromWire("not-a-real-encoding")).isNull();
    }

    @Test
    void fromWireReturnsNullForNull() {
        assertThat(TsEncodingFormat.fromWire(null)).isNull();
    }

}
