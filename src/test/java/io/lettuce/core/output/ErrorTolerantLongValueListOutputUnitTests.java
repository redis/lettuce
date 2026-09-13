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
 * Unit tests for {@link ErrorTolerantLongValueListOutput}.
 *
 * Verifies the {@link Value}-wrapped mapping used by reactive TS.MADD, whose reply array mixes successful (integer timestamp)
 * and failed (error) elements:
 * <ul>
 * <li>an integer element &rarr; {@code Value.just(timestamp)}</li>
 * <li>an error element, once the array has started &rarr; {@code Value.empty()}, without failing the whole command</li>
 * <li>a top-level error (array never started) &rarr; the command fails, as usual</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class ErrorTolerantLongValueListOutputUnitTests {

    private final ErrorTolerantLongValueListOutput<String, String> sut = new ErrorTolerantLongValueListOutput<>(
            StringCodec.UTF8);

    @Test
    void defaultSubscriberIsSet() {
        assertThat(sut.getSubscriber()).isNotNull().isInstanceOf(ListSubscriber.class);
    }

    @Test
    void setLongMappedToValueJust() {
        sut.multi(1);
        sut.set(1000L);

        List<Value<Long>> result = sut.get();
        assertThat(result).containsExactly(Value.just(1000L));
    }

    @Test
    void setErrorPushesValueEmptyWhenInitialized() {
        sut.multi(1);
        sut.setError(ByteBuffer.wrap("ERR TSDB: some error".getBytes()));

        List<Value<Long>> result = sut.get();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(Value.empty());
        assertThat(sut.hasError()).isFalse();
    }

    @Test
    void setErrorFailsCommandWhenNotInitialized() {
        sut.setError(ByteBuffer.wrap("ERR top-level failure".getBytes()));

        assertThat(sut.hasError()).isTrue();
        assertThat(sut.getError()).isEqualTo("ERR top-level failure");
    }

    @Test
    void partialFailureKeepsSuccessfulTimestampsAlongsideValueEmpty() {
        sut.multi(3);
        sut.set(1000L);
        sut.setError(ByteBuffer.wrap("ERR TSDB: duplicate policy BLOCK".getBytes()));
        sut.set(2000L);

        List<Value<Long>> result = sut.get();
        assertThat(result).containsExactly(Value.just(1000L), Value.empty(), Value.just(2000L));
        assertThat(sut.hasError()).isFalse();
    }

}
