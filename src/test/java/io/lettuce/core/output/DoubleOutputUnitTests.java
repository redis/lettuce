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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link DoubleOutput}.
 * <p>
 * Covers the two decode paths ({@link DoubleOutput#set(ByteBuffer)} for RESP2 bulk strings and {@link DoubleOutput#set(double)}
 * for the RESP3 native double) and the Redis {@code nan}/{@code inf}/{@code -inf} spellings, which are decoded via
 * {@code LettuceStrings.toDouble}.
 */
@Tag(UNIT_TEST)
class DoubleOutputUnitTests {

    private final DoubleOutput<String, String> sut = new DoubleOutput<>(StringCodec.UTF8);

    private static ByteBuffer buf(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesPlainDouble() {
        sut.set(buf("3.14"));
        assertThat(sut.get()).isEqualTo(3.14);
    }

    @Test
    void parsesNegativeDouble() {
        sut.set(buf("-2.5"));
        assertThat(sut.get()).isEqualTo(-2.5);
    }

    @Test
    void parsesNan() {
        sut.set(buf("nan"));
        assertThat(sut.get()).isNaN();
    }

    @Test
    void parsesPositiveInfinity() {
        sut.set(buf("inf"));
        assertThat(sut.get()).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void parsesSignedPositiveInfinity() {
        sut.set(buf("+inf"));
        assertThat(sut.get()).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void parsesNegativeInfinity() {
        sut.set(buf("-inf"));
        assertThat(sut.get()).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    @Test
    void handlesNativeDoublePath() {
        sut.set(9.0);
        assertThat(sut.get()).isEqualTo(9.0);
    }

    @Test
    void nullBulkStringMappedToNull() {
        sut.set(null);
        assertThat(sut.get()).isNull();
    }

}
