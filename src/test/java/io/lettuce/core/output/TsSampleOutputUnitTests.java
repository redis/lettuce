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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.RedisStateMachine;
import io.lettuce.core.timeseries.TsSample;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link TsSampleOutput}, the TS.GET output (design §4, shape P4). The replies are the wire bytes observed on
 * 8.10.0: RESP2 sends values as simple strings ({@code +1}, {@code +NaN}, {@code +Infinity}), RESP3 as doubles ({@code ,1},
 * {@code ,nan}, {@code ,inf}). They are fed through {@link RedisStateMachine}, which drives the output with
 * {@code multi}/{@code set}/{@code complete} as on a real connection; a second set of tests calls the output methods directly,
 * in the order the state machine calls them (each element is followed by {@code complete(depth)}).
 */
@Tag(UNIT_TEST)
class TsSampleOutputUnitTests {

    private final RedisStateMachine rsm = new RedisStateMachine();

    @AfterEach
    void tearDown() {
        rsm.close();
    }

    private TsSample decode(String reply) {

        ByteBuf buffer = Unpooled.copiedBuffer(reply, StandardCharsets.UTF_8);
        try {
            TsSampleOutput<String, String> output = new TsSampleOutput<>(StringCodec.UTF8);
            assertThat(rsm.decode(buffer, output)).isTrue();
            assertThat(buffer.readableBytes()).isZero();
            assertThat(output.hasError()).isFalse();
            return output.get();
        } finally {
            buffer.release();
        }
    }

    private static ByteBuffer bytes(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Scenario U03: RESP2 {@code [:1000, +1]} → (1000, 1.0).
     */
    @Test
    void resp2Value() {
        assertThat(decode("*2\r\n:1000\r\n+1\r\n")).isEqualTo(TsSample.of(1000, 1.0));
        assertThat(decode("*2\r\n:3000\r\n+4.333333333333333\r\n")).isEqualTo(TsSample.of(3000, 4.333333333333333));
        assertThat(decode("*2\r\n:5\r\n+-2.5\r\n")).isEqualTo(TsSample.of(5, -2.5));
    }

    /**
     * Scenario U03: RESP2 {@code NaN}, {@code Infinity} and {@code -Infinity} (spellings observed on the server,
     * TIMESERIES-CANONICAL.md §8).
     */
    @Test
    void resp2SpecialValues() {

        TsSample nan = decode("*2\r\n:2000\r\n+NaN\r\n");
        assertThat(nan.getTimestamp()).isEqualTo(2000);
        assertThat(nan.getValue()).isNaN();
        assertThat(decode("*2\r\n:1000\r\n+Infinity\r\n").getValue()).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(decode("*2\r\n:1000\r\n+-Infinity\r\n").getValue()).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    /**
     * Scenario U03: RESP3 doubles, including {@code nan}, {@code inf} and {@code -inf}.
     */
    @Test
    void resp3Values() {

        assertThat(decode("*2\r\n:1000\r\n,1\r\n")).isEqualTo(TsSample.of(1000, 1.0));
        assertThat(decode("*2\r\n:1000\r\n,1.5\r\n")).isEqualTo(TsSample.of(1000, 1.5));
        assertThat(decode("*2\r\n:2000\r\n,nan\r\n").getValue()).isNaN();
        assertThat(decode("*2\r\n:1000\r\n,inf\r\n").getValue()).isEqualTo(Double.POSITIVE_INFINITY);
        assertThat(decode("*2\r\n:1000\r\n,-inf\r\n").getValue()).isEqualTo(Double.NEGATIVE_INFINITY);
    }

    /**
     * Scenario U03: the RESP2 and RESP3 forms of the same sample are equal (NF.1).
     */
    @Test
    void resp2AndResp3Equal() {

        assertThat(decode("*2\r\n:1000\r\n+2.5\r\n")).isEqualTo(decode("*2\r\n:1000\r\n,2.5\r\n"));
        assertThat(decode("*2\r\n:1000\r\n+NaN\r\n")).isEqualTo(decode("*2\r\n:1000\r\n,nan\r\n"));
        assertThat(decode("*2\r\n:1000\r\n+Infinity\r\n")).isEqualTo(decode("*2\r\n:1000\r\n,inf\r\n"));
    }

    /**
     * Scenario U03: an empty array (a series without samples) → {@code null}, on both protocols.
     */
    @Test
    void emptyArrayIsNull() {
        assertThat(decode("*0\r\n")).isNull();
    }

    /**
     * Scenario U03: a large timestamp keeps its full {@code long} value.
     */
    @Test
    void largeTimestamp() {
        assertThat(decode("*2\r\n:1700000000123\r\n+7\r\n")).isEqualTo(TsSample.of(1700000000123L, 7.0));
    }

    /**
     * Scenario U03, driven directly: {@code set(long)} gives the timestamp, {@code set(ByteBuffer)} a RESP2 value.
     */
    @Test
    void directResp2() {

        TsSampleOutput<String, String> output = new TsSampleOutput<>(StringCodec.UTF8);
        output.multi(2);
        output.set(1000L);
        output.complete(1);
        output.set(bytes("NaN"));
        output.complete(1);
        output.complete(0);

        assertThat(output.get()).isEqualTo(TsSample.of(1000, Double.NaN));
    }

    /**
     * Scenario U03, driven directly: {@code set(double)} gives a RESP3 value.
     */
    @Test
    void directResp3() {

        TsSampleOutput<String, String> output = new TsSampleOutput<>(StringCodec.UTF8);
        output.multi(2);
        output.set(1000L);
        output.complete(1);
        output.set(Double.NEGATIVE_INFINITY);
        output.complete(1);
        output.complete(0);

        assertThat(output.get()).isEqualTo(TsSample.of(1000, Double.NEGATIVE_INFINITY));
    }

    /**
     * Scenario U03, driven directly: an empty array → {@code null}.
     */
    @Test
    void directEmpty() {

        TsSampleOutput<String, String> output = new TsSampleOutput<>(StringCodec.UTF8);
        output.multi(0);
        output.complete(0);

        assertThat(output.get()).isNull();
    }

}
