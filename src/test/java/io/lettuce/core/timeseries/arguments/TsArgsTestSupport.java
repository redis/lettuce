/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Helpers for the TimeSeries argument unit tests.
 */
final class TsArgsTestSupport {

    private TsArgsTestSupport() {
    }

    /**
     * Serialize {@code argument} and return the wire tokens, in order.
     */
    static List<String> tokens(CompositeArgument argument) {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        argument.build(args);

        ByteBuf buf = Unpooled.buffer();
        args.encode(buf);
        String[] lines = buf.toString(StandardCharsets.UTF_8).split("\r\n", -1);

        List<String> tokens = new ArrayList<>();
        for (int line = 0; line + 1 < lines.length; line += 2) {
            assertThat(lines[line]).startsWith("$");
            int length = Integer.parseInt(lines[line].substring(1));
            assertThat(lines[line + 1].getBytes(StandardCharsets.UTF_8)).hasSize(length);
            tokens.add(lines[line + 1]);
        }
        assertThat(tokens).hasSize(args.count());
        return tokens;
    }

}
