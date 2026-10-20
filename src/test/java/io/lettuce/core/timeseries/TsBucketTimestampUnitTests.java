/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.ProtocolKeyword;

/**
 * Unit tests for {@link TsBucketTimestamp}, the plain enum for BUCKETTIMESTAMP (design §3.1).
 */
@Tag(UNIT_TEST)
class TsBucketTimestampUnitTests {

    private static String token(ProtocolKeyword keyword) {
        return new String(keyword.getBytes(), StandardCharsets.US_ASCII);
    }

    /**
     * Scenario A08 / C09: {@code START} → {@code -}, {@code END} → {@code +}, {@code MID} → {@code ~}.
     */
    @Test
    void wireTokens() {

        assertThat(token(TsBucketTimestamp.START)).isEqualTo("-");
        assertThat(token(TsBucketTimestamp.END)).isEqualTo("+");
        assertThat(token(TsBucketTimestamp.MID)).isEqualTo("~");
    }

    /**
     * Scenario C09: the enum is closed and has exactly the three constants of design §3.1.
     */
    @Test
    void constants() {
        assertThat(TsBucketTimestamp.values()).containsExactly(TsBucketTimestamp.START, TsBucketTimestamp.END,
                TsBucketTimestamp.MID);
    }

}
