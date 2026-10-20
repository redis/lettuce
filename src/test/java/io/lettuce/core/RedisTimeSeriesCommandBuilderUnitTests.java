/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.Range.Boundary;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.timeseries.TsAggregation;
import io.lettuce.core.timeseries.TsAggregationType;
import io.lettuce.core.timeseries.TsDuplicatePolicy;
import io.lettuce.core.timeseries.TsDuplicatePolicyType;
import io.lettuce.core.timeseries.TsEncoding;
import io.lettuce.core.timeseries.TsEncodingType;
import io.lettuce.core.timeseries.TsMAddValue;
import io.lettuce.core.timeseries.arguments.TsAddArgs;
import io.lettuce.core.timeseries.arguments.TsAlterArgs;
import io.lettuce.core.timeseries.arguments.TsCreateArgs;
import io.lettuce.core.timeseries.arguments.TsIncrByArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link RedisTimeSeriesCommandBuilder}: the exact RESP encoding of the series lifecycle, write and compaction
 * rule commands.
 */
@Tag(UNIT_TEST)
class RedisTimeSeriesCommandBuilderUnitTests {

    private static final String KEY = "{ts}:k";

    private final RedisTimeSeriesCommandBuilder<String, String> builder = new RedisTimeSeriesCommandBuilder<>(StringCodec.UTF8);

    private static String encode(Command<?, ?, ?> command) {
        ByteBuf buf = Unpooled.buffer();
        command.encode(buf);
        return buf.toString(StandardCharsets.UTF_8);
    }

    /**
     * Decode the RESP array of bulk strings produced by {@link Command#encode(ByteBuf)} into its tokens.
     */
    private static List<String> tokens(Command<?, ?, ?> command) {

        String resp = encode(command);
        String[] lines = resp.split("\r\n", -1);
        assertThat(lines[0]).startsWith("*");
        int count = Integer.parseInt(lines[0].substring(1));
        List<String> tokens = new ArrayList<>();
        int line = 1;
        for (int i = 0; i < count; i++) {
            assertThat(lines[line]).startsWith("$");
            int length = Integer.parseInt(lines[line].substring(1));
            String token = lines[line + 1];
            assertThat(token.getBytes(StandardCharsets.UTF_8)).hasSize(length);
            tokens.add(token);
            line += 2;
        }
        return tokens;
    }

    private static Map<String, String> labels(String... pairs) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            labels.put(pairs[i], pairs[i + 1]);
        }
        return labels;
    }

    // --- TS.CREATE ---------------------------------------------------------------------------------------------------

    @Test
    void createWithoutOptions() {
        assertThat(encode(builder.tsCreate(KEY))).isEqualTo("*2\r\n$9\r\nTS.CREATE\r\n$6\r\n{ts}:k\r\n");
    }

    /**
     * Scenario A01: CREATE with every option emits the documented order with LABELS last, regardless of the setter order.
     */
    @Test
    void createWithEveryOptionEmitsFixedOrderLabelsLast() {

        TsCreateArgs args = new TsCreateArgs().labels(labels("sensor", "temp", "area", "north")).ignore(10, 0.5)
                .duplicatePolicy(TsDuplicatePolicyType.LAST).chunkSize(128).encoding(TsEncodingType.UNCOMPRESSED)
                .retention(5000);

        assertThat(tokens(builder.tsCreate(KEY, args))).containsExactly("TS.CREATE", KEY, "RETENTION", "5000", "ENCODING",
                "UNCOMPRESSED", "CHUNK_SIZE", "128", "DUPLICATE_POLICY", "LAST", "IGNORE", "10", "0.5", "LABELS", "sensor",
                "temp", "area", "north");
    }

    /**
     * Scenario A01: the exact wire bytes of a CREATE with every option.
     */
    @Test
    void createWithEveryOptionWireBytes() {

        TsCreateArgs args = new TsCreateArgs().retention(5000).encoding(TsEncodingType.COMPRESSED).chunkSize(128)
                .duplicatePolicy(TsDuplicatePolicyType.SUM).ignore(10, 0.5).label("a", "1");

        assertThat(encode(builder.tsCreate(KEY, args))).isEqualTo("*16\r\n" + "$9\r\nTS.CREATE\r\n" + "$6\r\n{ts}:k\r\n"
                + "$9\r\nRETENTION\r\n" + "$4\r\n5000\r\n" + "$8\r\nENCODING\r\n" + "$10\r\nCOMPRESSED\r\n"
                + "$10\r\nCHUNK_SIZE\r\n" + "$3\r\n128\r\n" + "$16\r\nDUPLICATE_POLICY\r\n" + "$3\r\nSUM\r\n"
                + "$6\r\nIGNORE\r\n" + "$2\r\n10\r\n" + "$3\r\n0.5\r\n" + "$6\r\nLABELS\r\n" + "$1\r\na\r\n" + "$1\r\n1\r\n");
    }

    /**
     * Scenario A01: options that are not set are not sent.
     */
    @Test
    void createWithSomeOptionsOmitsUnset() {

        assertThat(tokens(builder.tsCreate(KEY, new TsCreateArgs().chunkSize(256)))).containsExactly("TS.CREATE", KEY,
                "CHUNK_SIZE", "256");
        assertThat(tokens(builder.tsCreate(KEY, new TsCreateArgs()))).containsExactly("TS.CREATE", KEY);
    }

    /**
     * Scenario A03: each option is emitted exactly once; a setter called twice keeps the last value.
     */
    @Test
    void createEmitsEachOptionOnceLastCallWins() {

        TsCreateArgs args = new TsCreateArgs().duplicatePolicy(TsDuplicatePolicyType.FIRST)
                .duplicatePolicy(TsDuplicatePolicyType.MAX).retention(1).retention(2).encoding(TsEncodingType.COMPRESSED)
                .encoding(TsEncodingType.UNCOMPRESSED).chunkSize(64).chunkSize(128).ignore(1, 1.5).ignore(2, 2.5);

        assertThat(tokens(builder.tsCreate(KEY, args))).containsExactly("TS.CREATE", KEY, "RETENTION", "2", "ENCODING",
                "UNCOMPRESSED", "CHUNK_SIZE", "128", "DUPLICATE_POLICY", "MAX", "IGNORE", "2", "2.5");
    }

    /**
     * Scenario A03: {@code labels(Map)} replaces earlier labels, {@code label(...)} appends; LABELS appears once.
     */
    @Test
    void createLabelsReplaceAndAppend() {

        TsCreateArgs args = new TsCreateArgs().label("x", "1").labels(labels("a", "1")).label("b", "2");

        assertThat(tokens(builder.tsCreate(KEY, args))).containsExactly("TS.CREATE", KEY, "LABELS", "a", "1", "b", "2");
    }

    /**
     * Scenario A04: an empty label map on CREATE sends a bare LABELS.
     */
    @Test
    void createWithEmptyLabelsSendsBareLabels() {

        assertThat(tokens(builder.tsCreate(KEY, new TsCreateArgs().retention(0).labels(Collections.emptyMap()))))
                .containsExactly("TS.CREATE", KEY, "RETENTION", "0", "LABELS");
    }

    /**
     * Scenario A21: an unknown duplicate policy and encoding created with {@code of(String)} are sent exactly as given.
     */
    @Test
    void createWithRawValueTypes() {

        TsCreateArgs args = new TsCreateArgs().encoding(TsEncoding.of("zstd")).duplicatePolicy(TsDuplicatePolicy.of("keep"));

        assertThat(tokens(builder.tsCreate(KEY, args))).containsExactly("TS.CREATE", KEY, "ENCODING", "zstd",
                "DUPLICATE_POLICY", "keep");
    }

    /**
     * Scenario A21: {@code of(String)} of a known name encodes like the enum constant.
     */
    @Test
    void createWithKnownValueTypesFromString() {

        TsCreateArgs args = new TsCreateArgs().encoding(TsEncoding.of("uncompressed"))
                .duplicatePolicy(TsDuplicatePolicy.of("last"));

        assertThat(tokens(builder.tsCreate(KEY, args))).containsExactly("TS.CREATE", KEY, "ENCODING", "UNCOMPRESSED",
                "DUPLICATE_POLICY", "LAST");
    }

    @Test
    void createRejectsNullArguments() {

        assertThatThrownBy(() -> builder.tsCreate(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsCreate(null, new TsCreateArgs())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsCreate(KEY, null)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- TS.ALTER ----------------------------------------------------------------------------------------------------

    /**
     * Scenario A02a: ALTER with every option emits RETENTION, CHUNK_SIZE, DUPLICATE_POLICY, IGNORE, LABELS, in that order.
     */
    @Test
    void alterWithEveryOptionEmitsFixedOrderLabelsLast() {

        TsAlterArgs args = new TsAlterArgs().label("sensor", "hum").ignore(3, 4.5).duplicatePolicy(TsDuplicatePolicyType.MIN)
                .chunkSize(256).retention(60000);

        assertThat(tokens(builder.tsAlter(KEY, args))).containsExactly("TS.ALTER", KEY, "RETENTION", "60000", "CHUNK_SIZE",
                "256", "DUPLICATE_POLICY", "MIN", "IGNORE", "3", "4.5", "LABELS", "sensor", "hum");
    }

    /**
     * Scenario A02a: ALTER arguments offer no ENCODING option.
     */
    @Test
    void alterArgsHaveNoEncoding() {

        assertThat(TsAlterArgs.class.getMethods()).noneMatch(m -> m.getName().equalsIgnoreCase("encoding"));
        assertThat(TsAlterArgs.Builder.class.getMethods()).noneMatch(m -> m.getName().equalsIgnoreCase("encoding"));
    }

    /**
     * Scenario A03: ALTER emits DUPLICATE_POLICY once; the last call wins.
     */
    @Test
    void alterEmitsDuplicatePolicyOnce() {

        TsAlterArgs args = new TsAlterArgs().duplicatePolicy(TsDuplicatePolicyType.BLOCK)
                .duplicatePolicy(TsDuplicatePolicyType.LAST);

        assertThat(tokens(builder.tsAlter(KEY, args))).containsExactly("TS.ALTER", KEY, "DUPLICATE_POLICY", "LAST");
    }

    /**
     * Scenario A04: an empty label map on ALTER sends a bare LABELS.
     */
    @Test
    void alterWithEmptyLabelsSendsBareLabels() {

        assertThat(encode(builder.tsAlter(KEY, new TsAlterArgs().labels(Collections.emptyMap()))))
                .isEqualTo("*3\r\n$8\r\nTS.ALTER\r\n$6\r\n{ts}:k\r\n$6\r\nLABELS\r\n");
    }

    /**
     * Scenario A21: an unknown duplicate policy is sent as given on ALTER.
     */
    @Test
    void alterWithRawDuplicatePolicy() {

        assertThat(tokens(builder.tsAlter(KEY, new TsAlterArgs().duplicatePolicy(TsDuplicatePolicy.of("keep")))))
                .containsExactly("TS.ALTER", KEY, "DUPLICATE_POLICY", "keep");
    }

    @Test
    void alterRejectsNullArguments() {

        assertThatThrownBy(() -> builder.tsAlter(null, new TsAlterArgs())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsAlter(KEY, null)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- TS.DEL ------------------------------------------------------------------------------------------------------

    /**
     * Scenario A06a: inclusive bounds send the timestamps.
     */
    @Test
    void delInclusiveRange() {

        assertThat(encode(builder.tsDel(KEY, Range.create(1000L, 2000L))))
                .isEqualTo("*4\r\n$6\r\nTS.DEL\r\n$6\r\n{ts}:k\r\n$4\r\n1000\r\n$4\r\n2000\r\n");
    }

    /**
     * Scenario A06a: unbounded sides send {@code -} and {@code +}.
     */
    @Test
    void delUnboundedRange() {

        assertThat(tokens(builder.tsDel(KEY, Range.unbounded()))).containsExactly("TS.DEL", KEY, "-", "+");
        assertThat(tokens(builder.tsDel(KEY, Range.from(Boundary.unbounded(), Boundary.including(2000L)))))
                .containsExactly("TS.DEL", KEY, "-", "2000");
        assertThat(tokens(builder.tsDel(KEY, Range.from(Boundary.including(1000L), Boundary.unbounded()))))
                .containsExactly("TS.DEL", KEY, "1000", "+");
    }

    /**
     * Scenario A06b: exclusive bounds are sent as the inclusive neighbor.
     */
    @Test
    void delExclusiveBoundsAreConvertedToInclusiveNeighbor() {

        assertThat(tokens(builder.tsDel(KEY, Range.from(Boundary.excluding(1000L), Boundary.including(2000L)))))
                .containsExactly("TS.DEL", KEY, "1001", "2000");
        assertThat(tokens(builder.tsDel(KEY, Range.from(Boundary.including(1000L), Boundary.excluding(2000L)))))
                .containsExactly("TS.DEL", KEY, "1000", "1999");
        assertThat(tokens(builder.tsDel(KEY, Range.from(Boundary.excluding(1000L), Boundary.excluding(2000L)))))
                .containsExactly("TS.DEL", KEY, "1001", "1999");
        assertThat(tokens(builder.tsDel(KEY, Range.from(Boundary.excluding(1000L), Boundary.unbounded()))))
                .containsExactly("TS.DEL", KEY, "1001", "+");
    }

    /**
     * Scenario A06c: an exclusive bound at the {@code Long} edge throws before any command is created.
     */
    @Test
    void delExclusiveBoundAtLongEdgeThrows() {

        assertThatThrownBy(() -> builder.tsDel(KEY, Range.from(Boundary.excluding(Long.MAX_VALUE), Boundary.unbounded())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsDel(KEY, Range.from(Boundary.unbounded(), Boundary.excluding(Long.MIN_VALUE))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A06c: the bounds next to the edges are still accepted.
     */
    @Test
    void delExclusiveBoundNextToLongEdge() {

        assertThat(tokens(builder.tsDel(KEY, Range.from(Boundary.excluding(Long.MAX_VALUE - 1), Boundary.unbounded()))))
                .containsExactly("TS.DEL", KEY, Long.toString(Long.MAX_VALUE), "+");
        assertThat(tokens(builder.tsDel(KEY, Range.from(Boundary.unbounded(), Boundary.excluding(Long.MIN_VALUE + 1)))))
                .containsExactly("TS.DEL", KEY, "-", Long.toString(Long.MIN_VALUE));
    }

    @Test
    void delRejectsNullArguments() {

        assertThatThrownBy(() -> builder.tsDel(null, Range.unbounded())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsDel(KEY, null)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- TS.INFO -----------------------------------------------------------------------------------------------------

    /**
     * Scenario A19 (INFO): {@code tsInfo(k)} and {@code tsInfo(k, false)} send no DEBUG; {@code tsInfo(k, true)} sends DEBUG.
     */
    @Test
    void infoDebugFlag() {

        assertThat(encode(builder.tsInfo(KEY))).isEqualTo("*2\r\n$7\r\nTS.INFO\r\n$6\r\n{ts}:k\r\n");
        assertThat(encode(builder.tsInfo(KEY, false))).isEqualTo("*2\r\n$7\r\nTS.INFO\r\n$6\r\n{ts}:k\r\n");
        assertThat(encode(builder.tsInfo(KEY, true))).isEqualTo("*3\r\n$7\r\nTS.INFO\r\n$6\r\n{ts}:k\r\n$5\r\nDEBUG\r\n");
    }

    @Test
    void infoRejectsNullKey() {

        assertThatThrownBy(() -> builder.tsInfo(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsInfo(null, true)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- TS.ADD ------------------------------------------------------------------------------------------------------

    /**
     * Scenario A05: {@code tsAdd(k, v)} sends {@code TS.ADD k * v}.
     */
    @Test
    void addWithoutTimestampSendsStar() {

        assertThat(encode(builder.tsAdd(KEY, 2.5))).isEqualTo("*4\r\n$6\r\nTS.ADD\r\n$6\r\n{ts}:k\r\n$1\r\n*\r\n$3\r\n2.5\r\n");
        assertThat(tokens(builder.tsAdd(KEY, 2.5, new TsAddArgs().retention(10)))).containsExactly("TS.ADD", KEY, "*", "2.5",
                "RETENTION", "10");
    }

    /**
     * Scenario A05: {@code tsAdd(k, ts, v)} sends the timestamp; a {@code long} value is not confused with the timestamp.
     */
    @Test
    void addWithTimestamp() {

        assertThat(encode(builder.tsAdd(KEY, 1000L, 2.5)))
                .isEqualTo("*4\r\n$6\r\nTS.ADD\r\n$6\r\n{ts}:k\r\n$4\r\n1000\r\n$3\r\n2.5\r\n");
        assertThat(tokens(builder.tsAdd(KEY, 0L, 2.5))).containsExactly("TS.ADD", KEY, "0", "2.5");
    }

    /**
     * Scenario A02b: ADD with every option emits RETENTION, ENCODING, CHUNK_SIZE, DUPLICATE_POLICY, ON_DUPLICATE, IGNORE,
     * LABELS after the value, regardless of the setter order.
     */
    @Test
    void addWithEveryOptionEmitsFixedOrderLabelsLast() {

        TsAddArgs args = new TsAddArgs().labels(labels("sensor", "temp", "area", "north")).ignore(10, 0.5)
                .onDuplicate(TsDuplicatePolicyType.MIN).duplicatePolicy(TsDuplicatePolicyType.LAST).chunkSize(128)
                .encoding(TsEncodingType.UNCOMPRESSED).retention(5000);

        assertThat(tokens(builder.tsAdd(KEY, 1000L, 2.5, args))).containsExactly("TS.ADD", KEY, "1000", "2.5", "RETENTION",
                "5000", "ENCODING", "UNCOMPRESSED", "CHUNK_SIZE", "128", "DUPLICATE_POLICY", "LAST", "ON_DUPLICATE", "MIN",
                "IGNORE", "10", "0.5", "LABELS", "sensor", "temp", "area", "north");
        assertThat(tokens(builder.tsAdd(KEY, 2.5, args))).containsExactly("TS.ADD", KEY, "*", "2.5", "RETENTION", "5000",
                "ENCODING", "UNCOMPRESSED", "CHUNK_SIZE", "128", "DUPLICATE_POLICY", "LAST", "ON_DUPLICATE", "MIN", "IGNORE",
                "10", "0.5", "LABELS", "sensor", "temp", "area", "north");
    }

    /**
     * Scenario A02b: an unknown duplicate policy is sent as given in ON_DUPLICATE; an empty label map sends a bare LABELS.
     */
    @Test
    void addWithRawOnDuplicateAndBareLabels() {

        assertThat(tokens(builder.tsAdd(KEY, 1L, 2.5,
                new TsAddArgs().onDuplicate(TsDuplicatePolicy.of("keep")).labels(Collections.emptyMap()))))
                        .containsExactly("TS.ADD", KEY, "1", "2.5", "ON_DUPLICATE", "keep", "LABELS");
    }

    /**
     * Scenario A02b: NaN and negative values are sent as numbers the server parses.
     */
    @Test
    void addSpecialValues() {

        List<String> nan = tokens(builder.tsAdd(KEY, 1000L, Double.NaN));
        assertThat(nan.subList(0, 3)).containsExactly("TS.ADD", KEY, "1000");
        assertThat(nan.get(3)).isEqualToIgnoringCase("nan");

        List<String> negative = tokens(builder.tsAdd(KEY, 1000L, -1.7e308));
        assertThat(Double.parseDouble(negative.get(3))).isEqualTo(-1.7e308);
    }

    /**
     * Scenario A13: any {@code null} argument of TS.ADD throws {@link IllegalArgumentException}.
     */
    @Test
    void addRejectsNullArguments() {

        assertThatThrownBy(() -> builder.tsAdd(null, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsAdd(null, 1.0, new TsAddArgs())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsAdd(KEY, 1.0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsAdd(null, 1L, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsAdd(null, 1L, 1.0, new TsAddArgs())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsAdd(KEY, 1L, 1.0, null)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- TS.MADD -----------------------------------------------------------------------------------------------------

    /**
     * Scenario A05: {@code TsMAddValue.of(k, v)} is sent as {@code k * v}; samples keep the list order, including a repeated
     * key.
     */
    @Test
    void maddSendsTriplesInOrder() {

        List<TsMAddValue<String>> samples = Arrays.asList(TsMAddValue.of("{ts}:a", 1000L, 1.5), TsMAddValue.of("{ts}:b", 2.5),
                TsMAddValue.of("{ts}:a", 2000L, 3.5));

        assertThat(tokens(builder.tsMAdd(samples))).containsExactly("TS.MADD", "{ts}:a", "1000", "1.5", "{ts}:b", "*", "2.5",
                "{ts}:a", "2000", "3.5");
        assertThat(tokens(builder.tsMAddValues(samples))).containsExactly("TS.MADD", "{ts}:a", "1000", "1.5", "{ts}:b", "*",
                "2.5", "{ts}:a", "2000", "3.5");
    }

    /**
     * Scenario A05: the exact wire bytes of a one-sample TS.MADD with {@code *}.
     */
    @Test
    void maddWireBytes() {

        assertThat(encode(builder.tsMAdd(Collections.singletonList(TsMAddValue.of(KEY, 2.5)))))
                .isEqualTo("*4\r\n$7\r\nTS.MADD\r\n$6\r\n{ts}:k\r\n$1\r\n*\r\n$3\r\n2.5\r\n");
    }

    /**
     * Scenario A13 (MADD): a {@code null} or empty list, or a {@code null} element, throws {@link IllegalArgumentException}.
     */
    @Test
    void maddRejectsNullAndEmpty() {

        assertThatThrownBy(() -> builder.tsMAdd(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsMAdd(Collections.emptyList())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsMAdd(Arrays.asList(TsMAddValue.of(KEY, 1.0), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsMAddValues(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsMAddValues(Collections.emptyList())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsMAddValues(Collections.singletonList(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- TS.INCRBY / TS.DECRBY ---------------------------------------------------------------------------------------

    /**
     * Scenario A02b: INCRBY and DECRBY without options send the key and the operand only (server clock).
     */
    @Test
    void incrByDecrByWithoutOptions() {

        assertThat(encode(builder.tsIncrBy(KEY, 2.5))).isEqualTo("*3\r\n$9\r\nTS.INCRBY\r\n$6\r\n{ts}:k\r\n$3\r\n2.5\r\n");
        assertThat(encode(builder.tsDecrBy(KEY, 2.5))).isEqualTo("*3\r\n$9\r\nTS.DECRBY\r\n$6\r\n{ts}:k\r\n$3\r\n2.5\r\n");
        assertThat(tokens(builder.tsIncrBy(KEY, 2.5, new TsIncrByArgs()))).containsExactly("TS.INCRBY", KEY, "2.5");
    }

    /**
     * Scenario A02b: INCRBY and DECRBY with every option emit TIMESTAMP first and LABELS last, regardless of the setter order.
     */
    @Test
    void incrByDecrByWithEveryOptionTimestampFirstLabelsLast() {

        TsIncrByArgs args = new TsIncrByArgs().label("sensor", "temp").ignore(10, 0.5)
                .duplicatePolicy(TsDuplicatePolicyType.LAST).chunkSize(128).encoding(TsEncodingType.COMPRESSED).retention(5000)
                .timestamp(1000);

        assertThat(tokens(builder.tsIncrBy(KEY, 2.5, args))).containsExactly("TS.INCRBY", KEY, "2.5", "TIMESTAMP", "1000",
                "RETENTION", "5000", "ENCODING", "COMPRESSED", "CHUNK_SIZE", "128", "DUPLICATE_POLICY", "LAST", "IGNORE", "10",
                "0.5", "LABELS", "sensor", "temp");
        assertThat(tokens(builder.tsDecrBy(KEY, 2.5, args))).containsExactly("TS.DECRBY", KEY, "2.5", "TIMESTAMP", "1000",
                "RETENTION", "5000", "ENCODING", "COMPRESSED", "CHUNK_SIZE", "128", "DUPLICATE_POLICY", "LAST", "IGNORE", "10",
                "0.5", "LABELS", "sensor", "temp");
    }

    /**
     * Scenario A02b: a negative operand is sent as given.
     */
    @Test
    void incrByNegativeOperand() {

        assertThat(tokens(builder.tsIncrBy(KEY, -2.5))).containsExactly("TS.INCRBY", KEY, "-2.5");
        assertThat(tokens(builder.tsDecrBy(KEY, -2.5, TsIncrByArgs.Builder.timestamp(7)))).containsExactly("TS.DECRBY", KEY,
                "-2.5", "TIMESTAMP", "7");
    }

    /**
     * Scenario A13: any {@code null} argument of TS.INCRBY / TS.DECRBY throws {@link IllegalArgumentException}.
     */
    @Test
    void incrByDecrByRejectNullArguments() {

        assertThatThrownBy(() -> builder.tsIncrBy(null, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsIncrBy(null, 1.0, new TsIncrByArgs())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsIncrBy(KEY, 1.0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsDecrBy(null, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsDecrBy(null, 1.0, new TsIncrByArgs())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsDecrBy(KEY, 1.0, null)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- TS.CREATERULE / TS.DELETERULE -------------------------------------------------------------------------------

    /**
     * Scenario A18: CREATERULE without alignTimestamp sends {@code AGGREGATION aggregator bucketDuration} after both keys.
     */
    @Test
    void createRuleWithoutAlignment() {

        assertThat(encode(builder.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregationType.AVG, 1000)))
                .isEqualTo("*6\r\n" + "$13\r\nTS.CREATERULE\r\n" + "$8\r\n{ts}:src\r\n" + "$8\r\n{ts}:dst\r\n"
                        + "$11\r\nAGGREGATION\r\n" + "$3\r\nAVG\r\n" + "$4\r\n1000\r\n");
    }

    /**
     * Scenario A18: CREATERULE with alignTimestamp sends the alignment after the bucket duration.
     */
    @Test
    void createRuleWithAlignmentSendsItAfterBucketDuration() {

        assertThat(tokens(builder.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregationType.MIN, 1000, 500)))
                .containsExactly("TS.CREATERULE", "{ts}:src", "{ts}:dst", "AGGREGATION", "MIN", "1000", "500");
    }

    /**
     * Scenario A18: an alignment of 0 is sent as given, so the alignment overload always sends the value.
     */
    @Test
    void createRuleWithZeroAlignmentSendsIt() {

        assertThat(tokens(builder.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregationType.SUM, 60000, 0)))
                .containsExactly("TS.CREATERULE", "{ts}:src", "{ts}:dst", "AGGREGATION", "SUM", "60000", "0");
    }

    /**
     * Scenario A18 / A21: the aggregation tokens of design §3.1 ({@code STD.P}, {@code VAR.S}, {@code COUNTNAN}), and an
     * unknown aggregation from {@code TsAggregation.of(String)} sent exactly as given.
     */
    @Test
    void createRuleAggregationTokens() {

        assertThat(tokens(builder.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregationType.STD_P, 1000)).get(4))
                .isEqualTo("STD.P");
        assertThat(tokens(builder.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregationType.VAR_S, 1000)).get(4))
                .isEqualTo("VAR.S");
        assertThat(tokens(builder.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregationType.COUNTNAN, 1000)).get(4))
                .isEqualTo("COUNTNAN");
        assertThat(tokens(builder.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregation.of("median"), 1000, 7)))
                .containsExactly("TS.CREATERULE", "{ts}:src", "{ts}:dst", "AGGREGATION", "median", "1000", "7");
    }

    /**
     * Scenario A18: DELETERULE sends the source key, then the destination key.
     */
    @Test
    void deleteRule() {

        assertThat(encode(builder.tsDeleteRule("{ts}:src", "{ts}:dst")))
                .isEqualTo("*3\r\n" + "$13\r\nTS.DELETERULE\r\n" + "$8\r\n{ts}:src\r\n" + "$8\r\n{ts}:dst\r\n");
    }

    /**
     * Scenario A18: a {@code null} key or aggregation throws {@link IllegalArgumentException} (Javadoc of {@code tsCreateRule}
     * / {@code tsDeleteRule}).
     */
    @Test
    void createRuleAndDeleteRuleRejectNullArguments() {

        assertThatThrownBy(() -> builder.tsCreateRule(null, "{ts}:dst", TsAggregationType.AVG, 1000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsCreateRule("{ts}:src", null, TsAggregationType.AVG, 1000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsCreateRule("{ts}:src", "{ts}:dst", null, 1000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsCreateRule(null, "{ts}:dst", TsAggregationType.AVG, 1000, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsCreateRule("{ts}:src", null, TsAggregationType.AVG, 1000, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsCreateRule("{ts}:src", "{ts}:dst", null, 1000, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsDeleteRule(null, "{ts}:dst")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsDeleteRule("{ts}:src", null)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A18b: a negative alignTimestamp throws {@link IllegalArgumentException} before a command is built, and an
     * alignment of 0 is accepted and sent (design §2.2 and §5; Javadoc of {@code tsCreateRule}).
     */
    @Test
    void createRuleRejectsNegativeAlignment() {

        assertThatThrownBy(() -> builder.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregationType.AVG, 1000, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregationType.AVG, 1000, Long.MIN_VALUE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(tokens(builder.tsCreateRule("{ts}:src", "{ts}:dst", TsAggregationType.AVG, 1000, 0)))
                .containsExactly("TS.CREATERULE", "{ts}:src", "{ts}:dst", "AGGREGATION", "AVG", "1000", "0");
    }

}
