/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.timeseries.TsDuplicatePolicy;
import io.lettuce.core.timeseries.TsEncodingFormat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TsIncrByArgs}, shared by {@code TS.INCRBY} and {@code TS.DECRBY}.
 * <p>
 * Assertions are made against the raw RESP bulk-string encoding produced by {@link CommandArgs#encode(ByteBuf)} rather than
 * {@link CommandArgs#toCommandString()}: the latter renders custom {@code ProtocolKeyword} enum values (that are not
 * {@code CommandType}/{@code CommandKeyword}) as Base64 instead of their wire value, a pre-existing quirk in
 * {@code CommandArgs.ProtocolKeywordArgument} unrelated to this change. The actual wire bytes written by {@code encode()} are
 * unaffected and are what this test verifies.
 * <p>
 * PLAN (Given/When/Then):
 * <ul>
 * <li>Given no options set, when {@code build()}, then no tokens are emitted.</li>
 * <li>Given a single option set, when {@code build()}, then exactly that option's tokens are emitted.</li>
 * <li>Given {@code timestamp(ts)}, when {@code build()}, then {@code TIMESTAMP ts} is emitted.</li>
 * <li>Given {@code ignore(a, b)}, when {@code build()}, then both values are emitted after {@code IGNORE}.</li>
 * <li>Given {@code label(k, v)} called repeatedly, when {@code build()}, then all pairs are emitted in call order under a
 * single {@code LABELS} keyword.</li>
 * <li>Given {@code labels(Map)}, when {@code build()}, then the map's iteration order is preserved.</li>
 * <li>Given every option combined, when {@code build()}, then tokens appear in the wire-contract order: TIMESTAMP, RETENTION,
 * ENCODING, CHUNK_SIZE, DUPLICATE_POLICY, IGNORE, LABELS (LABELS last).</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class TsIncrByArgsUnitTests {

    private static String bulk(String value) {
        return "$" + value.getBytes(StandardCharsets.UTF_8).length + "\r\n" + value + "\r\n";
    }

    private static String encode(TsIncrByArgs incrByArgs) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        incrByArgs.build(args);
        ByteBuf buf = Unpooled.buffer();
        args.encode(buf);
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldRenderNoArgs() {
        assertThat(encode(new TsIncrByArgs())).isEmpty();
    }

    @Test
    void shouldRenderTimestamp() {
        assertThat(encode(TsIncrByArgs.Builder.timestamp(1000))).isEqualTo(bulk("TIMESTAMP") + bulk("1000"));
    }

    @Test
    void shouldRenderRetention() {
        assertThat(encode(TsIncrByArgs.Builder.retention(1000))).isEqualTo(bulk("RETENTION") + bulk("1000"));
    }

    @Test
    void shouldRenderEncoding() {
        assertThat(encode(TsIncrByArgs.Builder.encoding(TsEncodingFormat.COMPRESSED)))
                .isEqualTo(bulk("ENCODING") + bulk("COMPRESSED"));
    }

    @Test
    void shouldRenderChunkSize() {
        assertThat(encode(TsIncrByArgs.Builder.chunkSize(4096))).isEqualTo(bulk("CHUNK_SIZE") + bulk("4096"));
    }

    @Test
    void shouldRenderDuplicatePolicy() {
        assertThat(encode(TsIncrByArgs.Builder.duplicatePolicy(TsDuplicatePolicy.LAST)))
                .isEqualTo(bulk("DUPLICATE_POLICY") + bulk("LAST"));
    }

    @Test
    void shouldRenderIgnore() {
        assertThat(encode(TsIncrByArgs.Builder.ignore(100, 5.5))).isEqualTo(bulk("IGNORE") + bulk("100") + bulk("5.5"));
    }

    @Test
    void shouldRenderSingleLabel() {
        assertThat(encode(TsIncrByArgs.Builder.label("region", "us"))).isEqualTo(bulk("LABELS") + bulk("region") + bulk("us"));
    }

    @Test
    void shouldRenderChainedLabels() {
        assertThat(encode(TsIncrByArgs.Builder.label("region", "us").label("env", "prod")))
                .isEqualTo(bulk("LABELS") + bulk("region") + bulk("us") + bulk("env") + bulk("prod"));
    }

    @Test
    void shouldRenderLabelsFromMapPreservingOrder() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("region", "us");
        labels.put("env", "prod");

        assertThat(encode(TsIncrByArgs.Builder.labels(labels)))
                .isEqualTo(bulk("LABELS") + bulk("region") + bulk("us") + bulk("env") + bulk("prod"));
    }

    @Test
    void shouldAppendChainedLabelAfterLabelsMap() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("region", "us");

        assertThat(encode(TsIncrByArgs.Builder.labels(labels).label("env", "prod")))
                .isEqualTo(bulk("LABELS") + bulk("region") + bulk("us") + bulk("env") + bulk("prod"));
    }

    @Test
    void shouldRenderFullCombinationInWireOrder() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("region", "us");

        TsIncrByArgs incrByArgs = TsIncrByArgs.Builder.timestamp(2000).retention(1000).encoding(TsEncodingFormat.UNCOMPRESSED)
                .chunkSize(4096).duplicatePolicy(TsDuplicatePolicy.LAST).ignore(100, 5.5).labels(labels);

        assertThat(encode(incrByArgs)).isEqualTo(bulk("TIMESTAMP") + bulk("2000") + bulk("RETENTION") + bulk("1000")
                + bulk("ENCODING") + bulk("UNCOMPRESSED") + bulk("CHUNK_SIZE") + bulk("4096") + bulk("DUPLICATE_POLICY")
                + bulk("LAST") + bulk("IGNORE") + bulk("100") + bulk("5.5") + bulk("LABELS") + bulk("region") + bulk("us"));
    }

}
