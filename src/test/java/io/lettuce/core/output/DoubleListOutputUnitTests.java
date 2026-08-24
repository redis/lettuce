/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link DoubleListOutput}.
 * <p>
 * Covers the two decode paths ({@link DoubleListOutput#set(ByteBuffer)} for RESP2 bulk strings and
 * {@link DoubleListOutput#set(double)} for the RESP3 native double), the Redis {@code nan}/{@code inf}/{@code -inf} spellings
 * (decoded via {@code LettuceStrings.toDouble}), and {@code null} element handling.
 */
@Tag(UNIT_TEST)
class DoubleListOutputUnitTests {

    private final DoubleListOutput<String, String> sut = new DoubleListOutput<>(StringCodec.UTF8);

    private static ByteBuffer buf(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesPlainDoublesFromBulkStrings() {
        sut.multi(2);
        sut.set(buf("3.14"));
        sut.set(buf("-2.5"));

        assertThat(sut.get()).containsExactly(3.14, -2.5);
    }

    @Test
    void parsesNanFromBulkString() {
        sut.multi(1);
        sut.set(buf("nan"));

        List<Double> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNaN();
    }

    @Test
    void parsesPositiveInfinityFromBulkString() {
        sut.multi(1);
        sut.set(buf("inf"));

        assertThat(sut.get().get(0)).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void parsesNegativeInfinityFromBulkString() {
        sut.multi(1);
        sut.set(buf("-inf"));

        assertThat(sut.get().get(0)).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @Test
    void handlesNativeDoublePath() {
        sut.multi(2);
        sut.set(1.5);
        sut.set(Double.NaN);

        List<Double> result = sut.get();
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(1.5);
        assertThat(result.get(1)).isNaN();
    }

    @Test
    void nullBulkStringMappedToNull() {
        sut.multi(1);
        sut.set(null);

        List<Double> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNull();
    }

    @Test
    void mixedSpecialAndPlainValues() {
        sut.multi(4);
        sut.set(buf("nan"));
        sut.set(buf("inf"));
        sut.set(buf("-inf"));
        sut.set(buf("42"));

        List<Double> result = sut.get();
        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isNaN();
        assertThat(result.get(1)).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(result.get(2)).isEqualTo(Double.NEGATIVE_INFINITY);
        assertThat(result.get(3)).isEqualTo(42.0);
    }

}
