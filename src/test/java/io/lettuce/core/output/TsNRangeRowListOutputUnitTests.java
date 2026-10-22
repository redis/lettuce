/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.RedisStateMachine;
import io.lettuce.core.timeseries.TsNRangeRow;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link TsNRangeRowListOutput}, the TS.NRANGE / TS.NREVRANGE output (design §4, shape P6): rows
 * {@code [ts, [v…]]} with one more nesting level than TS.RANGE. The replies are the wire bytes observed on 8.10.0 for fixture
 * F6 ({@code {ts}:n1} (0, 1), (5, 2), (10, 100); {@code {ts}:n2} (0, 3), (5, 27), (12, 50)), fed through
 * {@link RedisStateMachine}.
 */
@Tag(UNIT_TEST)
class TsNRangeRowListOutputUnitTests {

    private static final List<Integer> RAW_TWO_KEYS = Arrays.asList(1, 1);

    /**
     * {@code TS.NRANGE 2 {ts}:n1 {ts}:n2 - +}, RESP2 (8.10.0).
     */
    private static final String RAW_RESP2 = "*4\r\n" + "*2\r\n:0\r\n*2\r\n+1\r\n+3\r\n" + "*2\r\n:5\r\n*2\r\n+2\r\n+27\r\n"
            + "*2\r\n:10\r\n*2\r\n+100\r\n+NaN\r\n" + "*2\r\n:12\r\n*2\r\n+NaN\r\n+50\r\n";

    /**
     * The same reply on RESP3 (8.10.0).
     */
    private static final String RAW_RESP3 = "*4\r\n" + "*2\r\n:0\r\n*2\r\n,1\r\n,3\r\n" + "*2\r\n:5\r\n*2\r\n,2\r\n,27\r\n"
            + "*2\r\n:10\r\n*2\r\n,100\r\n,nan\r\n" + "*2\r\n:12\r\n*2\r\n,nan\r\n,50\r\n";

    private static final List<TsNRangeRow> RAW_EXPECTED = Arrays.asList(row(0, RAW_TWO_KEYS, 1.0, 3.0),
            row(5, RAW_TWO_KEYS, 2.0, 27.0), row(10, RAW_TWO_KEYS, 100.0, Double.NaN), row(12, RAW_TWO_KEYS, Double.NaN, 50.0));

    private static final List<Integer> FLOW_COUNTS = Arrays.asList(1, 2);

    /**
     * {@code TS.NRANGE 2 {ts}:n1 {ts}:n2 - + AGGREGATION sum sum,count 10}, RESP2 (8.10.0):
     * {@code [[0,[3,30,2]],[10,[100,50,1]]]}.
     */
    private static final String FLOW_RESP2 = "*2\r\n" + "*2\r\n:0\r\n*3\r\n+3\r\n+30\r\n+2\r\n"
            + "*2\r\n:10\r\n*3\r\n+100\r\n+50\r\n+1\r\n";

    /**
     * The same reply on RESP3 (8.10.0).
     */
    private static final String FLOW_RESP3 = "*2\r\n" + "*2\r\n:0\r\n*3\r\n,3\r\n,30\r\n,2\r\n"
            + "*2\r\n:10\r\n*3\r\n,100\r\n,50\r\n,1\r\n";

    private static final List<TsNRangeRow> FLOW_EXPECTED = Arrays.asList(row(0, FLOW_COUNTS, 3.0, 30.0, 2.0),
            row(10, FLOW_COUNTS, 100.0, 50.0, 1.0));

    /**
     * {@code TS.NRANGE 2 {ts}:n1 {ts}:n2 - + AGGREGATION avg,max avg,max 3 EMPTY}, RESP2 (8.10.0): bucket 6 is empty for both
     * keys, so every slot is NaN.
     */
    private static final String EMPTY_RESP2 = "*5\r\n" + "*2\r\n:0\r\n*4\r\n+1\r\n+1\r\n+3\r\n+3\r\n"
            + "*2\r\n:3\r\n*4\r\n+2\r\n+2\r\n+27\r\n+27\r\n" + "*2\r\n:6\r\n*4\r\n+NaN\r\n+NaN\r\n+NaN\r\n+NaN\r\n"
            + "*2\r\n:9\r\n*4\r\n+100\r\n+100\r\n+NaN\r\n+NaN\r\n" + "*2\r\n:12\r\n*4\r\n+NaN\r\n+NaN\r\n+50\r\n+50\r\n";

    /**
     * The same reply on RESP3 (8.10.0).
     */
    private static final String EMPTY_RESP3 = "*5\r\n" + "*2\r\n:0\r\n*4\r\n,1\r\n,1\r\n,3\r\n,3\r\n"
            + "*2\r\n:3\r\n*4\r\n,2\r\n,2\r\n,27\r\n,27\r\n" + "*2\r\n:6\r\n*4\r\n,nan\r\n,nan\r\n,nan\r\n,nan\r\n"
            + "*2\r\n:9\r\n*4\r\n,100\r\n,100\r\n,nan\r\n,nan\r\n" + "*2\r\n:12\r\n*4\r\n,nan\r\n,nan\r\n,50\r\n,50\r\n";

    private final RedisStateMachine rsm = new RedisStateMachine();

    @AfterEach
    void tearDown() {
        rsm.close();
    }

    private static TsNRangeRow row(long timestamp, List<Integer> columnCounts, Double... values) {
        return TsNRangeRow.of(timestamp, Arrays.asList(values), columnCounts);
    }

    private List<TsNRangeRow> decode(String reply, List<Integer> columnCounts) {
        return decode(reply, new TsNRangeRowListOutput<>(StringCodec.UTF8, columnCounts));
    }

    private List<TsNRangeRow> decode(String reply, TsNRangeRowListOutput<String, String> output) {

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

    /**
     * Scenario U05: raw pivot on RESP2: one row per timestamp in reply order, values in key order, NaN where a key has no
     * sample.
     */
    @Test
    void rawRowsResp2() {

        List<TsNRangeRow> rows = decode(RAW_RESP2, RAW_TWO_KEYS);

        assertThat(rows).isEqualTo(RAW_EXPECTED);
        assertThat(rows.get(2).getValues(0)).containsExactly(100.0);
        assertThat(rows.get(2).getValues(1).get(0)).isNaN();
    }

    /**
     * Scenario U05: the same rows from the RESP3 reply ({@code ,nan} doubles), equal to the RESP2 rows.
     */
    @Test
    void rawRowsResp3() {

        assertThat(decode(RAW_RESP3, RAW_TWO_KEYS)).isEqualTo(RAW_EXPECTED).isEqualTo(decode(RAW_RESP2, RAW_TWO_KEYS));
    }

    /**
     * Scenario U05 / E04: the flow case with column counts {@code [1, 2]} (raw key, then {@code sum,count}):
     * {@code getValues(1)} of row 2 = [50, 1], on both protocols.
     */
    @Test
    void flowCaseSlicesByColumnCounts() {

        List<TsNRangeRow> fromResp2 = decode(FLOW_RESP2, FLOW_COUNTS);
        List<TsNRangeRow> fromResp3 = decode(FLOW_RESP3, FLOW_COUNTS);

        assertThat(fromResp2).isEqualTo(FLOW_EXPECTED);
        assertThat(fromResp3).isEqualTo(FLOW_EXPECTED);
        assertThat(fromResp2.get(1).getValues()).containsExactly(100.0, 50.0, 1.0);
        assertThat(fromResp2.get(1).getValues(0)).containsExactly(100.0);
        assertThat(fromResp2.get(1).getValues(1)).containsExactly(50.0, 1.0);
        assertThat(fromResp3.get(0).getValues(1)).containsExactly(30.0, 2.0);
    }

    /**
     * Scenario U05 / E07: EMPTY with two aggregations per key: the empty bucket has one NaN per requested slot.
     */
    @Test
    void emptyBucketsWithSeveralAggregations() {

        List<Integer> counts = Arrays.asList(2, 2);
        List<TsNRangeRow> expected = Arrays.asList(row(0, counts, 1.0, 1.0, 3.0, 3.0), row(3, counts, 2.0, 2.0, 27.0, 27.0),
                row(6, counts, Double.NaN, Double.NaN, Double.NaN, Double.NaN),
                row(9, counts, 100.0, 100.0, Double.NaN, Double.NaN), row(12, counts, Double.NaN, Double.NaN, 50.0, 50.0));

        assertThat(decode(EMPTY_RESP2, counts)).isEqualTo(expected);
        assertThat(decode(EMPTY_RESP3, counts)).isEqualTo(expected);
        assertThat(decode(EMPTY_RESP3, counts).get(2).getValues(1)).hasSize(2).allMatch(v -> v.isNaN());
    }

    /**
     * Scenario U05: RESP2 spells infinities {@code Infinity} / {@code -Infinity}, RESP3 sends {@code inf} / {@code -inf}
     * (TIMESERIES-CANONICAL.md §8); both decode to the Java infinities.
     */
    @Test
    void infinities() {

        List<TsNRangeRow> expected = Collections
                .singletonList(row(1, RAW_TWO_KEYS, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY));

        assertThat(decode("*1\r\n*2\r\n:1\r\n*2\r\n+Infinity\r\n+-Infinity\r\n", RAW_TWO_KEYS)).isEqualTo(expected);
        assertThat(decode("*1\r\n*2\r\n:1\r\n*2\r\n,inf\r\n,-inf\r\n", RAW_TWO_KEYS)).isEqualTo(expected);
    }

    /**
     * Scenario U05: one key, one column.
     */
    @Test
    void singleKey() {

        List<Integer> one = Collections.singletonList(1);
        assertThat(decode("*2\r\n*2\r\n:0\r\n*1\r\n+1\r\n*2\r\n:5\r\n*1\r\n,2.5\r\n", one)).containsExactly(row(0, one, 1.0),
                row(5, one, 2.5));
    }

    /**
     * Scenario U05: an empty reply ({@code *0}, observed for a range without samples on 8.10.0) → an empty list, not
     * {@code null}.
     */
    @Test
    void emptyReply() {

        assertThat(decode("*0\r\n", RAW_TWO_KEYS)).isNotNull().isEmpty();
    }

    /**
     * Scenario U05: rows are streamed to a subscriber one by one, each as soon as it is complete (design §4: "Each row is
     * emitted to the subscriber as soon as it is decoded"). The reply is fed in two parts; after the first part, the subscriber
     * has seen exactly the first row.
     */
    @Test
    void rowsStreamedOneByOne() {

        List<TsNRangeRow> seen = new ArrayList<>();
        TsNRangeRowListOutput<String, String> output = new TsNRangeRowListOutput<>(StringCodec.UTF8, FLOW_COUNTS);
        output.setSubscriber(new StreamingOutput.Subscriber<TsNRangeRow>() {

            @Override
            public void onNext(TsNRangeRow row) {
                seen.add(row);
            }

        });

        String firstRow = "*2\r\n*2\r\n:0\r\n*3\r\n+3\r\n+30\r\n+2\r\n";
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeBytes(firstRow.getBytes(StandardCharsets.UTF_8));
            assertThat(rsm.decode(buffer, output)).isFalse();
            assertThat(seen).containsExactly(FLOW_EXPECTED.get(0));

            buffer.writeBytes(FLOW_RESP2.substring(firstRow.length()).getBytes(StandardCharsets.UTF_8));
            assertThat(rsm.decode(buffer, output)).isTrue();
            assertThat(seen).isEqualTo(FLOW_EXPECTED);
        } finally {
            buffer.release();
        }
    }

    /**
     * Scenario U05: the default subscriber collects the rows into {@code get()}.
     */
    @Test
    void defaultSubscriberIsSet() {
        assertThat(new TsNRangeRowListOutput<>(StringCodec.UTF8, RAW_TWO_KEYS).getSubscriber()).isNotNull();
    }

    /**
     * Scenario U05: an NRANGE reply followed by the reply of the next command: each output gets its own reply, so the
     * connection stays in sync.
     */
    @Test
    void nextReplyNotAffected() {

        ByteBuf buffer = Unpooled.copiedBuffer(EMPTY_RESP3 + ":5\r\n", StandardCharsets.UTF_8);
        try {
            TsNRangeRowListOutput<String, String> nrange = new TsNRangeRowListOutput<>(StringCodec.UTF8, Arrays.asList(2, 2));
            IntegerOutput<String, String> next = new IntegerOutput<>(StringCodec.UTF8);

            assertThat(rsm.decode(buffer, nrange)).isTrue();
            assertThat(rsm.decode(buffer, next)).isTrue();

            assertThat(nrange.get()).hasSize(5);
            assertThat(nrange.get().get(4).getValues()).containsExactly(Double.NaN, Double.NaN, 50.0, 50.0);
            assertThat(next.get()).isEqualTo(5L);
        } finally {
            buffer.release();
        }
    }

    /**
     * Scenario U05: the column counts must not be {@code null} or empty (constructor Javadoc).
     */
    @Test
    void constructorValidation() {

        assertThatThrownBy(() -> new TsNRangeRowListOutput<>(StringCodec.UTF8, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeRowListOutput<>(StringCodec.UTF8, Collections.<Integer> emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
