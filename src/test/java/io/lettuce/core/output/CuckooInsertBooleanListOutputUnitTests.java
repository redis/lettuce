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
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link CuckooInsertBooleanListOutput}.
 *
 * Verifies the 3-state mapping required by CF.INSERT / CF.INSERTNX:
 * <ul>
 * <li>1 → {@code Boolean.TRUE} (item added)</li>
 * <li>0 → {@code Boolean.FALSE} (item already exists, INSERTNX only)</li>
 * <li>-1 → {@code null} (filter is full)</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class CuckooInsertBooleanListOutputUnitTests {

    private final CuckooInsertBooleanListOutput<String, String> sut = new CuckooInsertBooleanListOutput<>(StringCodec.UTF8);

    @Test
    void defaultSubscriberIsSet() {
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    void set1MappedToTrue() {
        sut.multi(1);
        sut.set(1L);

        List<Boolean> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void set0MappedToFalse() {
        sut.multi(1);
        sut.set(0L);

        List<Boolean> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Boolean.FALSE);
    }

    @Test
    void setNegativeOneMappedToNull() {
        sut.multi(1);
        sut.set(-1L);

        List<Boolean> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNull();
    }

    @Test
    void otherNegativeValueMappedToNull() {
        sut.multi(1);
        sut.set(-2L);

        List<Boolean> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNull();
    }

    @Test
    void mixedResponsesAllDistinct() {
        sut.multi(3);
        sut.set(1L);
        sut.set(0L);
        sut.set(-1L);

        List<Boolean> result = sut.get();
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(Boolean.TRUE);
        assertThat(result.get(1)).isEqualTo(Boolean.FALSE);
        assertThat(result.get(2)).isNull();
    }

    @Test
    void setErrorPushesNullWhenInitialized() {
        sut.multi(1);
        sut.setError(ByteBuffer.wrap("ERR filter full".getBytes()));

        List<Boolean> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNull();
    }

    @Test
    void setNullByteBufferPushesNullWhenInitialized() {
        sut.multi(1);
        sut.set((ByteBuffer) null);

        List<Boolean> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNull();
    }

}
