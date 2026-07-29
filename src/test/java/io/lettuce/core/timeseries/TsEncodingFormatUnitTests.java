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

}
