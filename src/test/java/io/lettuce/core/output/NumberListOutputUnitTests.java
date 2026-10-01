/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NumberListOutput}.
 */
@Tag(UNIT_TEST)
class NumberListOutputUnitTests {

    @Test
    void set() {
        NumberListOutput<String, String> sut = new NumberListOutput<>(StringCodec.UTF8);
        sut.multi(4);
        sut.set(ByteBuffer.wrap((String.valueOf(Double.MAX_VALUE)).getBytes()));
        sut.set(1.2);
        sut.set(1L);
        sut.setBigNumber(ByteBuffer.wrap(String.valueOf(Double.MAX_VALUE).getBytes()));

        assertThat(sut.get().isEmpty()).isFalse();
        assertThat(sut.get().size()).isEqualTo(4);
        assertThat(sut.get().get(0)).isEqualTo(Double.MAX_VALUE);
        assertThat(sut.get().get(1)).isEqualTo(1.2);
        assertThat(sut.get().get(2)).isEqualTo(1L);
        assertThat(sut.get().get(3)).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void setNegative() {
        NumberListOutput<String, String> sut = new NumberListOutput<>(StringCodec.UTF8);
        sut.multi(1);
        sut.set(ByteBuffer.wrap("Not a number".getBytes()));

        assertThat(sut.get().isEmpty()).isFalse();
        assertThat(sut.get().size()).isEqualTo(1);
        assertThat(sut.get().get(0)).isEqualTo(0);
    }

    @Test
    void setResp2JsonArray() {
        NumberListOutput<String, String> sut = new NumberListOutput<>(StringCodec.UTF8);
        sut.set(ByteBuffer.wrap("[1933,7.5,-2,1.5E2, null]".getBytes()));

        assertThat(sut.get()).containsExactly(1933L, 7.5, -2L, 150.0, null);
    }

    @Test
    void setResp2EmptyJsonArray() {
        NumberListOutput<String, String> sut = new NumberListOutput<>(StringCodec.UTF8);
        sut.set(ByteBuffer.wrap("[]".getBytes()));

        assertThat(sut.get()).isEmpty();
    }

    @Test
    void setResp2LegacyPathNumber() {
        NumberListOutput<String, String> sut = new NumberListOutput<>(StringCodec.UTF8);
        sut.set(ByteBuffer.wrap("1933".getBytes()));

        assertThat(sut.get()).containsExactly(1933L);
    }

    @Test
    void setNullBulkString() {
        NumberListOutput<String, String> sut = new NumberListOutput<>(StringCodec.UTF8);
        sut.multi(1);
        sut.set(null);

        assertThat(sut.get()).containsExactly((Number) null);
    }

}
