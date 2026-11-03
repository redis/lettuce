/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.Value;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link CuckooInsertBooleanValueListOutput}.
 *
 * Verifies the 3-state mapping required by reactive CF.INSERTNX:
 * <ul>
 * <li>1 → {@code Value.just(Boolean.TRUE)} (item added)</li>
 * <li>0 → {@code Value.just(Boolean.FALSE)} (item already exists, INSERTNX only)</li>
 * <li>-1 → {@code Value.empty()} (filter is full)</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class CuckooInsertBooleanValueListOutputUnitTests {

    private final CuckooInsertBooleanValueListOutput<String, String> sut = new CuckooInsertBooleanValueListOutput<>(
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
    void setNegativeOneMappedToValueEmpty() {
        sut.multi(1);
        sut.set(-1L);

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.empty());
    }

    @Test
    void otherNegativeValueMappedToValueEmpty() {
        sut.multi(1);
        sut.set(-2L);

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.empty());
    }

    @Test
    void mixedResponsesAllDistinct() {
        sut.multi(3);
        sut.set(1L);
        sut.set(0L);
        sut.set(-1L);

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(Value.just(Boolean.TRUE));
        assertThat(result.get(1)).isEqualTo(Value.just(Boolean.FALSE));
        assertThat(result.get(2)).isEqualTo(Value.empty());
    }

    @Test
    void setErrorPushesValueEmptyWhenInitialized() {
        sut.multi(1);
        sut.setError(java.nio.ByteBuffer.wrap("ERR filter full".getBytes()));

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.empty());
    }

    @Test
    void setNullByteBufferPushesValueEmptyWhenInitialized() {
        sut.multi(1);
        sut.set((java.nio.ByteBuffer) null);

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.empty());
    }

    @Test
    void setSimpleStringPushesValueEmptyWhenInitialized() {
        sut.multi(1);
        sut.set(java.nio.ByteBuffer.wrap("Filter is full".getBytes()));

        List<Value<Boolean>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.empty());
    }

}
