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

import io.lettuce.core.Value;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link ErrorTolerantBooleanValueListOutput}.
 *
 * Verifies the {@link Value}-wrapped mapping used by reactive BF.INSERT, BF.MADD, and CF.INSERT:
 * <ul>
 * <li>{@code 1} &rarr; {@code Value.just(Boolean.TRUE)} (item added)</li>
 * <li>{@code 0} or other non-{@code 1} integer &rarr; {@code Value.just(Boolean.FALSE)} (item may already exist)</li>
 * <li>simple string / inline error / nil &rarr; {@code Value.empty()} (filter full or error)</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class ErrorTolerantBooleanValueListOutputUnitTests {

    private final ErrorTolerantBooleanValueListOutput<String, String> sut = new ErrorTolerantBooleanValueListOutput<>(
            StringCodec.UTF8);

    @Test
    void defaultSubscriberIsSet() {
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    void set1MappedToValueTrue() {
        sut.multi(1);
        sut.set(1L);

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.just(Boolean.TRUE));
    }

    @Test
    void set0MappedToValueFalse() {
        sut.multi(1);
        sut.set(0L);

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.just(Boolean.FALSE));
    }

    @Test
    void setNegativeOneMappedToValueFalse() {
        sut.multi(1);
        sut.set(-1L);

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.just(Boolean.FALSE));
    }

    @Test
    void setBooleanPassthrough() {
        sut.multi(2);
        sut.set(true);
        sut.set(false);

        List<Value<Boolean>> result = sut.get();
        assertThat(result).containsExactly(Value.just(Boolean.TRUE), Value.just(Boolean.FALSE));
    }

    @Test
    void setErrorPushesValueEmptyWhenInitialized() {
        sut.multi(1);
        sut.setError(ByteBuffer.wrap("ERR filter full".getBytes()));

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.empty());
    }

    @Test
    void setNullByteBufferPushesValueEmptyWhenInitialized() {
        sut.multi(1);
        sut.set((ByteBuffer) null);

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.empty());
    }

    @Test
    void setSimpleStringPushesValueEmptyWhenInitialized() {
        sut.multi(1);
        sut.set(ByteBuffer.wrap("Filter is full".getBytes()));

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.empty());
    }

}
