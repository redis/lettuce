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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.RedisStateMachine;
import io.lettuce.core.timeseries.TsSample;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link TsSampleListOutput}, the TS.RANGE / TS.REVRANGE / TS.READ output (design §4, shape P5). The replies are
 * the wire bytes observed on 8.10.0, fed through {@link RedisStateMachine}; some tests drive the output methods directly, in
 * the order the state machine calls them.
 */
@Tag(UNIT_TEST)
class TsSampleListOutputUnitTests {

    /**
     * F2 ({@code 2, 4, 4, 4, 5, 5, 7, 9} at 1000 … 8000), {@code AGGREGATION min,avg,max 3000}, RESP2 (verified on 8.10.0).
     */
    private static final String MULTI_AGG_RESP2 = "*3\r\n" + "*4\r\n:0\r\n+2\r\n+3\r\n+4\r\n"
            + "*4\r\n:3000\r\n+4\r\n+4.333333333333333\r\n+5\r\n" + "*4\r\n:6000\r\n+5\r\n+7\r\n+9\r\n";

    /**
     * The same reply on RESP3.
     */
    private static final String MULTI_AGG_RESP3 = "*3\r\n" + "*4\r\n:0\r\n,2\r\n,3\r\n,4\r\n"
            + "*4\r\n:3000\r\n,4\r\n,4.333333333333333\r\n,5\r\n" + "*4\r\n:6000\r\n,5\r\n,7\r\n,9\r\n";

    private static final List<TsSample> MULTI_AGG_EXPECTED = Arrays.asList(TsSample.of(0, Arrays.asList(2.0, 3.0, 4.0)),
            TsSample.of(3000, Arrays.asList(4.0, 4.333333333333333, 5.0)), TsSample.of(6000, Arrays.asList(5.0, 7.0, 9.0)));

    private final RedisStateMachine rsm = new RedisStateMachine();

    @AfterEach
    void tearDown() {
        rsm.close();
    }

    private List<TsSample> decode(String reply) {
        return decode(reply, new TsSampleListOutput<>(StringCodec.UTF8));
    }

    private List<TsSample> decode(String reply, TsSampleListOutput<String, String> output) {

        ByteBuf buffer = Unpooled.copiedBuffer(reply, StandardCharsets.UTF_8);
        try {
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
     * Scenario U04: raw rows on RESP2 (F1) and RESP3, including NaN, in reply order.
     */
    @Test
    void rawRows() {

        assertThat(decode("*3\r\n*2\r\n:1000\r\n+1\r\n*2\r\n:2000\r\n+2\r\n*2\r\n:3000\r\n+3\r\n"))
                .containsExactly(TsSample.of(1000, 1.0), TsSample.of(2000, 2.0), TsSample.of(3000, 3.0));
        assertThat(decode("*4\r\n*2\r\n:1000\r\n,1\r\n*2\r\n:2000\r\n,nan\r\n*2\r\n:3000\r\n,3\r\n*2\r\n:4000\r\n,nan\r\n"))
                .containsExactly(TsSample.of(1000, 1.0), TsSample.of(2000, Double.NaN), TsSample.of(3000, 3.0),
                        TsSample.of(4000, Double.NaN));
        assertThat(decode("*2\r\n*2\r\n:4000\r\n+NaN\r\n*2\r\n:1000\r\n+-Infinity\r\n"))
                .containsExactly(TsSample.of(4000, Double.NaN), TsSample.of(1000, Double.NEGATIVE_INFINITY));
    }

    /**
     * Scenario U04: multi-value rows (several aggregations) on RESP2 and RESP3; each row keeps its values in order, and both
     * protocols give equal lists.
     */
    @Test
    void multiValueRows() {

        List<TsSample> fromResp2 = decode(MULTI_AGG_RESP2);
        List<TsSample> fromResp3 = decode(MULTI_AGG_RESP3);

        assertThat(fromResp2).isEqualTo(MULTI_AGG_EXPECTED);
        assertThat(fromResp3).isEqualTo(MULTI_AGG_EXPECTED);
        assertThat(fromResp2.get(1).getValue()).isEqualTo(4.0);
        assertThat(fromResp2.get(1).getValues()).containsExactly(4.0, 4.333333333333333, 5.0);
    }

    /**
     * Scenario U04: rows of different widths in one reply are kept as sent.
     */
    @Test
    void mixedWidths() {
        assertThat(decode("*2\r\n*2\r\n:1\r\n+1\r\n*3\r\n:2\r\n+2\r\n+3\r\n")).containsExactly(TsSample.of(1, 1.0),
                TsSample.of(2, Arrays.asList(2.0, 3.0)));
    }

    /**
     * Scenario U04: an empty reply → an empty list (not {@code null}).
     */
    @Test
    void emptyReply() {
        assertThat(decode("*0\r\n")).isNotNull().isEmpty();
    }

    /**
     * Scenario U04: rows are streamed to a subscriber one by one, each as soon as its inner array is complete. The reply is fed
     * in two parts; after the first part, the subscriber has seen exactly the first row.
     */
    @Test
    void rowsStreamedOneByOne() {

        List<TsSample> seen = new ArrayList<>();
        TsSampleListOutput<String, String> output = new TsSampleListOutput<>(StringCodec.UTF8);
        output.setSubscriber(new StreamingOutput.Subscriber<TsSample>() {

            @Override
            public void onNext(TsSample sample) {
                seen.add(sample);
            }

        });

        String firstRow = "*3\r\n*4\r\n:0\r\n+2\r\n+3\r\n+4\r\n";
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeBytes(firstRow.getBytes(StandardCharsets.UTF_8));
            assertThat(rsm.decode(buffer, output)).isFalse();
            assertThat(seen).containsExactly(MULTI_AGG_EXPECTED.get(0));

            buffer.writeBytes(MULTI_AGG_RESP2.substring(firstRow.length()).getBytes(StandardCharsets.UTF_8));
            assertThat(rsm.decode(buffer, output)).isTrue();
            assertThat(seen).isEqualTo(MULTI_AGG_EXPECTED);
        } finally {
            buffer.release();
        }
    }

    /**
     * Scenario U04: the default subscriber collects the rows into {@code get()}.
     */
    @Test
    void defaultSubscriberIsSet() {
        assertThat(new TsSampleListOutput<>(StringCodec.UTF8).getSubscriber()).isNotNull();
    }

    /**
     * Scenario U04: a range reply followed by the reply of the next command: each output gets its own reply.
     */
    @Test
    void nextReplyNotAffected() {

        ByteBuf buffer = Unpooled.copiedBuffer(MULTI_AGG_RESP3 + ":5\r\n", StandardCharsets.UTF_8);
        try {
            TsSampleListOutput<String, String> range = new TsSampleListOutput<>(StringCodec.UTF8);
            IntegerOutput<String, String> next = new IntegerOutput<>(StringCodec.UTF8);

            assertThat(rsm.decode(buffer, range)).isTrue();
            assertThat(rsm.decode(buffer, next)).isTrue();

            assertThat(range.get()).isEqualTo(MULTI_AGG_EXPECTED);
            assertThat(next.get()).isEqualTo(5L);
        } finally {
            buffer.release();
        }
    }

    /**
     * Scenario U04, driven directly: two RESP2 rows, then one RESP3 row with two values, in the call order of the state
     * machine.
     */
    @Test
    void directDrive() {

        TsSampleListOutput<String, String> output = new TsSampleListOutput<>(StringCodec.UTF8);
        output.multi(2);
        output.multi(2);
        output.set(1000L);
        output.complete(2);
        output.set(bytes("Infinity"));
        output.complete(2);
        output.complete(1);
        output.multi(3);
        output.set(2000L);
        output.complete(2);
        output.set(1.5);
        output.complete(2);
        output.set(Double.NaN);
        output.complete(2);
        output.complete(1);
        output.complete(0);

        assertThat(output.get()).containsExactly(TsSample.of(1000, Double.POSITIVE_INFINITY),
                TsSample.of(2000, Arrays.asList(1.5, Double.NaN)));
    }

    /**
     * Scenario U04, driven directly: an empty array → an empty list.
     */
    @Test
    void directEmpty() {

        TsSampleListOutput<String, String> output = new TsSampleListOutput<>(StringCodec.UTF8);
        output.multi(0);
        output.complete(0);

        assertThat(output.get()).isEmpty();
    }

}
