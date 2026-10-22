/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.Value;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ErrorTolerantIntegerListOutput;
import io.lettuce.core.output.ErrorTolerantIntegerValueListOutput;
import io.lettuce.core.output.IntegerOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Scenario U07: TS.MADD reply framing. {@link RedisStateMachine} is fed the bytes of two pipelined replies, a TS.MADD reply
 * with an error element followed by the reply of the next command. The error element must not end the TS.MADD reply early, so
 * the next command gets its own reply.
 */
@Tag(UNIT_TEST)
class TsMAddReplyFramingUnitTests {

    /**
     * The TS.MADD partial-failure reply verified on 8.10.0 (TIMESERIES-CANONICAL.md §8).
     */
    private static final String MADD_REPLY = "*2\r\n"
            + "-ERR TSDB: Error at upsert, update is not supported when DUPLICATE_POLICY is set to BLOCK mode, or either current or new value is NaN and DUPLICATE_POLICY is MAX/MIN/SUM\r\n"
            + ":9\r\n";

    private static final String NEXT_REPLY = ":5\r\n";

    private final RedisStateMachine rsm = new RedisStateMachine();

    @AfterEach
    void tearDown() {
        rsm.close();
    }

    private static ByteBuf buffer(String content) {
        return Unpooled.copiedBuffer(content, StandardCharsets.UTF_8);
    }

    /**
     * Scenario U07: {@code *2 -ERR… :9} then {@code :5}: the first output gets {@code [null, 9]}, the second gets {@code 5}.
     */
    @Test
    void errorElementDoesNotDesyncTheNextReply() {

        ByteBuf buffer = buffer(MADD_REPLY + NEXT_REPLY);
        ErrorTolerantIntegerListOutput<String, String> madd = new ErrorTolerantIntegerListOutput<>(StringCodec.UTF8);
        IntegerOutput<String, String> next = new IntegerOutput<>(StringCodec.UTF8);

        assertThat(rsm.decode(buffer, madd)).isTrue();
        assertThat(rsm.decode(buffer, next)).isTrue();

        assertThat(madd.get()).containsExactly(null, 9L);
        assertThat(madd.hasError()).isFalse();
        assertThat(next.get()).isEqualTo(5L);
        assertThat(next.hasError()).isFalse();
        assertThat(buffer.readableBytes()).isZero();
        buffer.release();
    }

    /**
     * Scenario U07 ({@code Value} variant used by the reactive API): the same bytes give {@code [Value.empty(), 9]} and
     * {@code 5}.
     */
    @Test
    void errorElementDoesNotDesyncTheNextReplyValueVariant() {

        ByteBuf buffer = buffer(MADD_REPLY + NEXT_REPLY);
        ErrorTolerantIntegerValueListOutput<String, String> madd = new ErrorTolerantIntegerValueListOutput<>(StringCodec.UTF8);
        IntegerOutput<String, String> next = new IntegerOutput<>(StringCodec.UTF8);

        assertThat(rsm.decode(buffer, madd)).isTrue();
        assertThat(rsm.decode(buffer, next)).isTrue();

        assertThat(madd.get()).containsExactly(Value.empty(), Value.just(9L));
        assertThat(next.get()).isEqualTo(5L);
        buffer.release();
    }

    /**
     * Scenario U07: the TS.MADD reply arrives in two network reads, split inside the error element; the reply is still decoded
     * whole, and the next reply stays separate.
     */
    @Test
    void errorElementSplitAcrossReads() {

        String all = MADD_REPLY + NEXT_REPLY;
        int split = all.indexOf("BLOCK mode");
        ByteBuf buffer = buffer(all.substring(0, split));
        ErrorTolerantIntegerListOutput<String, String> madd = new ErrorTolerantIntegerListOutput<>(StringCodec.UTF8);
        IntegerOutput<String, String> next = new IntegerOutput<>(StringCodec.UTF8);

        assertThat(rsm.decode(buffer, madd)).isFalse();
        buffer.writeBytes(all.substring(split).getBytes(StandardCharsets.UTF_8));
        assertThat(rsm.decode(buffer, madd)).isTrue();
        assertThat(rsm.decode(buffer, next)).isTrue();

        assertThat(madd.get()).containsExactly(null, 9L);
        assertThat(next.get()).isEqualTo(5L);
        buffer.release();
    }

    /**
     * Scenario U07: a top-level error (before the array) fails only the TS.MADD command; the next reply is unaffected.
     */
    @Test
    void topLevelErrorThenNextReply() {

        ByteBuf buffer = buffer("-ERR wrong number of arguments for 'ts.madd' command\r\n" + NEXT_REPLY);
        ErrorTolerantIntegerListOutput<String, String> madd = new ErrorTolerantIntegerListOutput<>(StringCodec.UTF8);
        IntegerOutput<String, String> next = new IntegerOutput<>(StringCodec.UTF8);

        assertThat(rsm.decode(buffer, madd)).isTrue();
        assertThat(rsm.decode(buffer, next)).isTrue();

        assertThat(madd.hasError()).isTrue();
        assertThat(madd.getError()).contains("wrong number of arguments");
        assertThat(next.get()).isEqualTo(5L);
        buffer.release();
    }

}
