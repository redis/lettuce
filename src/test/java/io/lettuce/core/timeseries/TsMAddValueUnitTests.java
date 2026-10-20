/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsMAddValue}: factories, validation and the server-clock marker.
 */
@Tag(UNIT_TEST)
class TsMAddValueUnitTests {

    /**
     * Scenario U14: the getters return the inputs of {@code of(key, timestamp, value)}.
     */
    @Test
    void ofWithTimestamp() {

        TsMAddValue<String> sample = TsMAddValue.of("{ts}:a", 1000L, 2.5);

        assertThat(sample.getKey()).isEqualTo("{ts}:a");
        assertThat(sample.getTimestamp()).isEqualTo(1000L);
        assertThat(sample.getValue()).isEqualTo(2.5);
    }

    /**
     * Scenario U14: timestamp 0 is allowed; NaN and infinite values are not checked by the client (the server decides).
     */
    @Test
    void ofAcceptsZeroTimestampAndAnyValue() {

        assertThat(TsMAddValue.of("k", 0L, 1.0).getTimestamp()).isEqualTo(0L);
        assertThat(TsMAddValue.of("k", 1L, Double.NaN).getValue()).isNaN();
        assertThat(TsMAddValue.of("k", 1L, Double.NEGATIVE_INFINITY).getValue()).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    /**
     * Scenario U14: {@code of(key, value)} carries the server-clock marker: no timestamp ({@code null}), sent as {@code *}.
     */
    @Test
    void ofWithoutTimestampHasServerClockMarker() {

        TsMAddValue<String> sample = TsMAddValue.of("{ts}:a", 3.0);

        assertThat(sample.getKey()).isEqualTo("{ts}:a");
        assertThat(sample.getTimestamp()).isNull();
        assertThat(sample.getValue()).isEqualTo(3.0);
    }

    /**
     * Scenario U14: a {@code null} key throws {@link IllegalArgumentException} on both factories.
     */
    @Test
    void nullKeyThrows() {

        assertThatThrownBy(() -> TsMAddValue.of(null, 1000L, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsMAddValue.of(null, 1.0)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario U14: a negative timestamp throws {@link IllegalArgumentException}.
     */
    @Test
    void negativeTimestampThrows() {

        assertThatThrownBy(() -> TsMAddValue.of("k", -1L, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsMAddValue.of("k", Long.MIN_VALUE, 1.0)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Design §3.2: {@link TsMAddValue} is an immutable value; equal inputs give equal values.
     */
    @Test
    void valueEquality() {

        assertThat(TsMAddValue.of("k", 1L, 2.0)).isEqualTo(TsMAddValue.of("k", 1L, 2.0))
                .hasSameHashCodeAs(TsMAddValue.of("k", 1L, 2.0));
        assertThat(TsMAddValue.of("k", 2.0)).isEqualTo(TsMAddValue.of("k", 2.0));
        assertThat(TsMAddValue.of("k", 1L, 2.0)).isNotEqualTo(TsMAddValue.of("k", 2.0))
                .isNotEqualTo(TsMAddValue.of("k", 1L, 3.0)).isNotEqualTo(TsMAddValue.of("j", 1L, 2.0));
    }

}
