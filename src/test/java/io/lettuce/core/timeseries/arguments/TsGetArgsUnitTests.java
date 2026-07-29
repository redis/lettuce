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

/**
 * Unit tests for {@link TsGetArgs}.
 * <p>
 * PLAN (Given/When/Then):
 * <ul>
 * <li>Given no options set, when {@code build()}, then no tokens are emitted.</li>
 * <li>Given {@code latest()}, when {@code build()}, then exactly {@code LATEST} is emitted.</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class TsGetArgsUnitTests {

    private static String bulk(String value) {
        return "$" + value.getBytes(StandardCharsets.UTF_8).length + "\r\n" + value + "\r\n";
    }

    private static String encode(TsGetArgs getArgs) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        getArgs.build(args);
        ByteBuf buf = Unpooled.buffer();
        args.encode(buf);
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldRenderNoArgs() {
        assertThat(encode(new TsGetArgs())).isEmpty();
    }

    @Test
    void shouldRenderLatest() {
        assertThat(encode(TsGetArgs.Builder.latest())).isEqualTo(bulk("LATEST"));
    }

}
