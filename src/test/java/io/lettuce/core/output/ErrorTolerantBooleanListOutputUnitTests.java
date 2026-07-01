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
 * Unit tests for {@link ErrorTolerantBooleanListOutput}.
 *
 * Verifies the mapping used by BF.INSERT, BF.MADD, and CF.INSERT:
 * <ul>
 * <li>{@code 1} &rarr; {@code Boolean.TRUE} (item added)</li>
 * <li>{@code 0} or other non-{@code 1} integer &rarr; {@code Boolean.FALSE} (item may already exist)</li>
 * <li>simple string / inline error / nil &rarr; {@code null} (filter full or error)</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class ErrorTolerantBooleanListOutputUnitTests {

    private final ErrorTolerantBooleanListOutput<String, String> sut = new ErrorTolerantBooleanListOutput<>(StringCodec.UTF8);

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
    void setNegativeOneMappedToFalse() {
        sut.multi(1);
        sut.set(-1L);

        List<Boolean> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Boolean.FALSE);
    }

    @Test
    void setBooleanPassthrough() {
        sut.multi(2);
        sut.set(true);
        sut.set(false);

        List<Boolean> result = sut.get();
        assertThat(result).containsExactly(Boolean.TRUE, Boolean.FALSE);
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

    @Test
    void setSimpleStringPushesNullWhenInitialized() {
        sut.multi(1);
        sut.set(ByteBuffer.wrap("Filter is full".getBytes()));

        List<Boolean> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNull();
    }

}
