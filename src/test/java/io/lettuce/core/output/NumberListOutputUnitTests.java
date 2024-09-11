/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.json.JsonType;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NumberListOutput}.
 */
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
    void testSet() {
    }

    @Test
    void testSet1() {
    }

    @Test
    void setBigNumber() {
    }

    @Test
    void multi() {
    }

}
