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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.Value;
import io.lettuce.core.codec.StringCodec;

/**
 * Unit tests for {@link ErrorTolerantIntegerValueListOutput}, the TS.MADD output of the reactive API: an error element of the
 * reply array becomes {@link Value#empty()}, an error before the array fails the command.
 */
@Tag(UNIT_TEST)
class ErrorTolerantIntegerValueListOutputUnitTests {

    private final ErrorTolerantIntegerValueListOutput<String, String> sut = new ErrorTolerantIntegerValueListOutput<>(
            StringCodec.UTF8);

    private static ByteBuffer bytes(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Scenario U06 ({@code Value} variant): {@code *2 -ERR… :9} gives {@code [Value.empty(), Value.just(9)]}.
     */
    @Test
    void errorElementBecomesValueEmpty() {

        sut.multi(2);
        sut.setError(bytes("ERR TSDB: Error at upsert, update is not supported when DUPLICATE_POLICY is set to BLOCK mode"));
        sut.set(9L);

        assertThat(sut.get()).containsExactly(Value.empty(), Value.just(9L));
        assertThat(sut.hasError()).isFalse();
    }

    /**
     * Scenario U06 ({@code Value} variant): the missing-key reply gives {@code [Value.just(1), Value.empty()]}.
     */
    @Test
    void errorElementAtTheEnd() {

        sut.multi(2);
        sut.set(1L);
        sut.setError(bytes("ERR TSDB: the key is not a TSDB key"));

        assertThat(sut.get()).containsExactly(Value.just(1L), Value.empty());
        assertThat(sut.hasError()).isFalse();
    }

    /**
     * Scenario U06 ({@code Value} variant): each element is streamed to the subscriber in reply order.
     */
    @Test
    void streamsElementsInOrder() {

        List<Value<Long>> received = new ArrayList<>();
        sut.setSubscriber(new StreamingOutput.Subscriber<Value<Long>>() {

            @Override
            public void onNext(Value<Long> item) {
                received.add(item);
            }

        });

        sut.multi(3);
        sut.set(1000L);
        sut.setError(bytes("ERR x"));
        sut.set(3000L);

        assertThat(received).containsExactly(Value.just(1000L), Value.empty(), Value.just(3000L));
    }

    /**
     * Scenario U06 ({@code Value} variant): an error before the array fails the command.
     */
    @Test
    void topLevelErrorFailsTheCommand() {

        sut.setError(bytes("ERR wrong number of arguments for 'ts.madd' command"));

        assertThat(sut.hasError()).isTrue();
        assertThat(sut.getError()).isEqualTo("ERR wrong number of arguments for 'ts.madd' command");
    }

    @Test
    void defaultSubscriberIsSet() {
        assertThat(sut.getSubscriber()).isNotNull();
    }

}
