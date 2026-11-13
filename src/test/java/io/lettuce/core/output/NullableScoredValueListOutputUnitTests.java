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

import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link NullableScoredValueListOutput}.
 */
@Tag(UNIT_TEST)
class NullableScoredValueListOutputUnitTests {

    private final NullableScoredValueListOutput<String, String> sut = new NullableScoredValueListOutput<>(StringCodec.UTF8);

    private static ByteBuffer bytes(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void resp2PairsAreDecodedInOrder() {

        sut.multi(2);
        sut.multi(2);
        sut.set(bytes("a"));
        sut.set(bytes("1"));
        sut.multi(2);
        sut.set(bytes("b"));
        sut.set(bytes("2.5"));

        assertThat(sut.get()).containsExactly(ScoredValue.just(1, "a"), ScoredValue.just(2.5, "b"));
    }

    @Test
    void resp3PairsAreDecodedInOrder() {

        sut.multi(1);
        sut.multi(2);
        sut.set(bytes("a"));
        sut.set(Double.POSITIVE_INFINITY);

        assertThat(sut.get()).containsExactly(ScoredValue.just(Double.POSITIVE_INFINITY, "a"));
    }

    @Test
    void resp2NullArrayStaysNull() {

        sut.multi(-1);

        assertThat(sut.get()).isNull();
    }

    @Test
    void resp3NullStaysNull() {

        sut.set((ByteBuffer) null);

        assertThat(sut.get()).isNull();
    }

    @Test
    void emptyArrayIsEmptyList() {

        sut.multi(0);

        assertThat(sut.get()).isEmpty();
    }

}
