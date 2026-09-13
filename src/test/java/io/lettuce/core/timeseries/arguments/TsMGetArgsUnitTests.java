/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import java.nio.charset.StandardCharsets;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TsMGetArgs}.
 * <p>
 * PLAN (Given/When/Then):
 * <ul>
 * <li>Given no options set, when {@code build()}, then no tokens are emitted (the caller appends {@code FILTER}
 * separately).</li>
 * <li>Given {@code latest()}, when {@code build()}, then exactly {@code LATEST} is emitted.</li>
 * <li>Given {@code withLabels()}, when {@code build()}, then exactly {@code WITHLABELS} is emitted.</li>
 * <li>Given {@code selectedLabels(a, b)}, when {@code build()}, then {@code SELECTED_LABELS a b} is emitted.</li>
 * <li>Given both {@code withLabels()} and {@code selectedLabels(...)}, when {@code build()}, then an
 * {@link IllegalArgumentException} is thrown (server rejects the combination, {@code query_language.c:851-853}).</li>
 * <li>Given {@code latest()} combined with {@code selectedLabels(...)}, when {@code build()}, then {@code LATEST} precedes
 * {@code SELECTED_LABELS} so a subsequent {@code FILTER} keyword still terminates the label scan on the wire.</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class TsMGetArgsUnitTests {

    private static String bulk(String value) {
        return "$" + value.getBytes(StandardCharsets.UTF_8).length + "\r\n" + value + "\r\n";
    }

    private static String encode(TsMGetArgs mGetArgs) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        mGetArgs.build(args);
        ByteBuf buf = Unpooled.buffer();
        args.encode(buf);
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldRenderNoArgs() {
        assertThat(encode(new TsMGetArgs())).isEmpty();
    }

    @Test
    void shouldRenderLatest() {
        assertThat(encode(TsMGetArgs.Builder.latest())).isEqualTo(bulk("LATEST"));
    }

    @Test
    void shouldRenderWithLabels() {
        assertThat(encode(TsMGetArgs.Builder.withLabels())).isEqualTo(bulk("WITHLABELS"));
    }

    @Test
    void shouldRenderSelectedLabels() {
        assertThat(encode(TsMGetArgs.Builder.selectedLabels("region", "env")))
                .isEqualTo(bulk("SELECTED_LABELS") + bulk("region") + bulk("env"));
    }

    @Test
    void shouldRenderLatestBeforeSelectedLabels() {
        TsMGetArgs args = TsMGetArgs.Builder.latest().selectedLabels("region");

        assertThat(encode(args)).isEqualTo(bulk("LATEST") + bulk("SELECTED_LABELS") + bulk("region"));
    }

    @Test
    void shouldRejectWithLabelsAndSelectedLabelsTogether() {
        TsMGetArgs args = TsMGetArgs.Builder.withLabels().selectedLabels("region");

        assertThatThrownBy(() -> encode(args)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("WITHLABELS")
                .hasMessageContaining("SELECTED_LABELS");
    }

}
