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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TsAlterArgs}.
 * <p>
 * See {@link TsCreateArgsUnitTests} for why assertions target {@link CommandArgs#encode(ByteBuf)} rather than
 * {@link CommandArgs#toCommandString()}.
 * <p>
 * PLAN (Given/When/Then): mirrors {@link TsCreateArgsUnitTests}, minus {@code ENCODING} (TS.ALTER cannot change the encoding of
 * an existing series).
 * <ul>
 * <li>Given no options set, when {@code build()}, then no tokens are emitted.</li>
 * <li>Given each option individually, when {@code build()}, then exactly that option's tokens are emitted.</li>
 * <li>Given every option combined, when {@code build()}, then tokens appear in wire-contract order: RETENTION, CHUNK_SIZE,
 * DUPLICATE_POLICY, IGNORE, LABELS.</li>
 * <li>Given {@code labelsReset()}, when {@code build()}, then an empty {@code LABELS} keyword is emitted (clears existing
 * labels server-side).</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class TsAlterArgsUnitTests {

    private static String bulk(String value) {
        return "$" + value.getBytes(StandardCharsets.UTF_8).length + "\r\n" + value + "\r\n";
    }

    private static String encode(TsAlterArgs alterArgs) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        alterArgs.build(args);
        ByteBuf buf = Unpooled.buffer();
        args.encode(buf);
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldRenderNoArgs() {
        assertThat(encode(new TsAlterArgs())).isEmpty();
    }

    @Test
    void shouldRenderRetention() {
        assertThat(encode(TsAlterArgs.Builder.retention(1000))).isEqualTo(bulk("RETENTION") + bulk("1000"));
    }

    @Test
    void shouldRenderChunkSize() {
        assertThat(encode(TsAlterArgs.Builder.chunkSize(4096))).isEqualTo(bulk("CHUNK_SIZE") + bulk("4096"));
    }

    @Test
    void shouldRenderDuplicatePolicy() {
        assertThat(encode(TsAlterArgs.Builder.duplicatePolicy(TsDuplicatePolicy.MIN)))
                .isEqualTo(bulk("DUPLICATE_POLICY") + bulk("MIN"));
    }

    @Test
    void shouldRenderIgnore() {
        assertThat(encode(TsAlterArgs.Builder.ignore(50, 1.25))).isEqualTo(bulk("IGNORE") + bulk("50") + bulk("1.25"));
    }

    @Test
    void shouldRenderSingleLabel() {
        assertThat(encode(TsAlterArgs.Builder.label("region", "us"))).isEqualTo(bulk("LABELS") + bulk("region") + bulk("us"));
    }

    @Test
    void shouldRenderLabelsFromMapPreservingOrder() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("region", "us");
        labels.put("env", "prod");

        assertThat(encode(TsAlterArgs.Builder.labels(labels)))
                .isEqualTo(bulk("LABELS") + bulk("region") + bulk("us") + bulk("env") + bulk("prod"));
    }

    @Test
    void shouldRenderLabelsResetAsEmptyLabelsKeyword() {
        assertThat(encode(TsAlterArgs.Builder.labelsReset())).isEqualTo(bulk("LABELS"));
    }

    @Test
    void shouldRenderFullCombinationInWireOrderWithoutEncoding() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("region", "us");

        TsAlterArgs alterArgs = TsAlterArgs.Builder.retention(1000).chunkSize(4096).duplicatePolicy(TsDuplicatePolicy.LAST)
                .ignore(100, 5.5).labels(labels);

        assertThat(encode(alterArgs)).isEqualTo(
                bulk("RETENTION") + bulk("1000") + bulk("CHUNK_SIZE") + bulk("4096") + bulk("DUPLICATE_POLICY") + bulk("LAST")
                        + bulk("IGNORE") + bulk("100") + bulk("5.5") + bulk("LABELS") + bulk("region") + bulk("us"));
    }

}
