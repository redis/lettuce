/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.timeseries.TsReadOffset;

/**
 * Unit tests for {@link TsReadOffset}: the server sentinels and literal timestamps (design §3.3, H18).
 */
@Tag(UNIT_TEST)
class TsReadOffsetUnitTests {

    /**
     * Scenario A11: {@code earliest()} → {@code -}, {@code latest()} → {@code +}, {@code newSamples()} → {@code $},
     * {@code from(0)} → {@code 0}.
     */
    @Test
    void wireForms() {

        assertThat(TsReadOffset.earliest()).hasToString("-");
        assertThat(TsReadOffset.latest()).hasToString("+");
        assertThat(TsReadOffset.newSamples()).hasToString("$");
        assertThat(TsReadOffset.from(0)).hasToString("0");
        assertThat(TsReadOffset.from(1700000000123L)).hasToString("1700000000123");
        assertThat(TsReadOffset.from(Long.MAX_VALUE)).hasToString(Long.toString(Long.MAX_VALUE));
    }

    /**
     * Scenario A11: a negative timestamp throws {@link IllegalArgumentException}.
     */
    @Test
    void negativeTimestamp() {

        assertThatThrownBy(() -> TsReadOffset.from(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsReadOffset.from(Long.MIN_VALUE)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A11: offsets are values: equal wire forms are equal, with a consistent hash code.
     */
    @Test
    void equality() {

        assertThat(TsReadOffset.from(5)).isEqualTo(TsReadOffset.from(5)).hasSameHashCodeAs(TsReadOffset.from(5));
        assertThat(TsReadOffset.earliest()).isEqualTo(TsReadOffset.earliest());
        assertThat(TsReadOffset.latest()).isEqualTo(TsReadOffset.latest());
        assertThat(TsReadOffset.newSamples()).isEqualTo(TsReadOffset.newSamples());
        assertThat(TsReadOffset.from(5)).isNotEqualTo(TsReadOffset.from(6));
        assertThat(TsReadOffset.earliest()).isNotEqualTo(TsReadOffset.latest()).isNotEqualTo(TsReadOffset.newSamples())
                .isNotEqualTo(TsReadOffset.from(0));
    }

}
