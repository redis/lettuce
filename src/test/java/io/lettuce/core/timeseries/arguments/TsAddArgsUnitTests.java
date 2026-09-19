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
 * Unit tests for {@link TsAddArgs}.
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
 * <li>Given {@code onDuplicate(policy)}, when {@code build()}, then the token emitted is {@code ON_DUPLICATE}, never
 * {@code DUPLICATE_POLICY}.</li>
 * <li>Given {@code duplicatePolicy(policy)}, when {@code build()}, then the token emitted is {@code DUPLICATE_POLICY}.</li>
 * <li>Given {@code ignore(a, b)}, when {@code build()}, then both values are emitted after {@code IGNORE}.</li>
 * <li>Given {@code label(k, v)} called repeatedly, when {@code build()}, then all pairs are emitted in call order under a
 * single {@code LABELS} keyword.</li>
 * <li>Given {@code labels(Map)}, when {@code build()}, then the map's iteration order is preserved.</li>
 * <li>Given every option combined, when {@code build()}, then tokens appear in the wire-contract order: RETENTION, ENCODING,
 * CHUNK_SIZE, DUPLICATE_POLICY, ON_DUPLICATE, IGNORE, LABELS (LABELS last).</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class TsAddArgsUnitTests {

    private static String bulk(String value) {
        return "$" + value.getBytes(StandardCharsets.UTF_8).length + "\r\n" + value + "\r\n";
    }

    private static String encode(TsAddArgs addArgs) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        addArgs.build(args);
        ByteBuf buf = Unpooled.buffer();
        args.encode(buf);
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldRenderNoArgs() {
        assertThat(encode(new TsAddArgs())).isEmpty();
    }

    @Test
    void shouldRenderRetention() {
        assertThat(encode(TsAddArgs.Builder.retention(1000))).isEqualTo(bulk("RETENTION") + bulk("1000"));
    }

    @Test
    void shouldRenderEncoding() {
        assertThat(encode(TsAddArgs.Builder.encoding(TsEncodingFormat.COMPRESSED)))
                .isEqualTo(bulk("ENCODING") + bulk("COMPRESSED"));
    }

    @Test
    void shouldRenderChunkSize() {
        assertThat(encode(TsAddArgs.Builder.chunkSize(4096))).isEqualTo(bulk("CHUNK_SIZE") + bulk("4096"));
    }

    @Test
    void shouldRenderOnDuplicateNotDuplicatePolicy() {
        assertThat(encode(TsAddArgs.Builder.onDuplicate(TsDuplicatePolicy.LAST)))
                .isEqualTo(bulk("ON_DUPLICATE") + bulk("LAST"));
    }

    @Test
    void shouldRenderDuplicatePolicy() {
        assertThat(encode(TsAddArgs.Builder.duplicatePolicy(TsDuplicatePolicy.MAX)))
                .isEqualTo(bulk("DUPLICATE_POLICY") + bulk("MAX"));
    }

    @Test
    void shouldRenderIgnore() {
        assertThat(encode(TsAddArgs.Builder.ignore(100, 5.5))).isEqualTo(bulk("IGNORE") + bulk("100") + bulk("5.5"));
    }

    @Test
    void shouldRenderSingleLabel() {
        assertThat(encode(TsAddArgs.Builder.label("region", "us"))).isEqualTo(bulk("LABELS") + bulk("region") + bulk("us"));
    }

    @Test
    void shouldRenderChainedLabels() {
        assertThat(encode(TsAddArgs.Builder.label("region", "us").label("env", "prod")))
                .isEqualTo(bulk("LABELS") + bulk("region") + bulk("us") + bulk("env") + bulk("prod"));
    }

    @Test
    void shouldRenderLabelsFromMapPreservingOrder() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("region", "us");
        labels.put("env", "prod");

        assertThat(encode(TsAddArgs.Builder.labels(labels)))
                .isEqualTo(bulk("LABELS") + bulk("region") + bulk("us") + bulk("env") + bulk("prod"));
    }

    @Test
    void shouldAppendChainedLabelAfterLabelsMap() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("region", "us");

        assertThat(encode(TsAddArgs.Builder.labels(labels).label("env", "prod")))
                .isEqualTo(bulk("LABELS") + bulk("region") + bulk("us") + bulk("env") + bulk("prod"));
    }

    @Test
    void shouldRenderFullCombinationInWireOrder() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("region", "us");

        TsAddArgs addArgs = TsAddArgs.Builder.retention(1000).encoding(TsEncodingFormat.UNCOMPRESSED).chunkSize(4096)
                .duplicatePolicy(TsDuplicatePolicy.LAST).onDuplicate(TsDuplicatePolicy.MAX).ignore(100, 5.5).labels(labels);

        assertThat(encode(addArgs)).isEqualTo(bulk("RETENTION") + bulk("1000") + bulk("ENCODING") + bulk("UNCOMPRESSED")
                + bulk("CHUNK_SIZE") + bulk("4096") + bulk("DUPLICATE_POLICY") + bulk("LAST") + bulk("ON_DUPLICATE")
                + bulk("MAX") + bulk("IGNORE") + bulk("100") + bulk("5.5") + bulk("LABELS") + bulk("region") + bulk("us"));
    }

}
