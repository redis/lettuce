/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.output.ComplexDataParser;
import io.lettuce.core.output.EncodedComplexOutput;
import io.lettuce.core.protocol.RedisStateMachine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

/**
 * Helpers for the TimeSeries parser unit tests: feed the bytes of one reply through {@link RedisStateMachine}, as on a real
 * connection.
 */
final class TsReplyTestSupport {

    private TsReplyTestSupport() {
    }

    /**
     * Decode {@code resp} into an {@link EncodedComplexOutput} with {@code parser} and return the parsed value.
     */
    static <K, V, T> T parse(String resp, RedisCodec<K, V> codec, ComplexDataParser<T> parser) {
        return decode(resp, new EncodedComplexOutput<>(codec, parser));
    }

    /**
     * Decode {@code resp} into {@code output} and return {@code output.get()}.
     */
    static <K, V, T> T decode(String resp, CommandOutput<K, V, T> output) {

        RedisStateMachine rsm = new RedisStateMachine(ByteBufAllocator.DEFAULT);
        ByteBuf buffer = Unpooled.copiedBuffer(resp, StandardCharsets.UTF_8);
        try {
            assertThat(rsm.decode(buffer, output)).isTrue();
            assertThat(buffer.readableBytes()).isZero();
            assertThat(output.getError()).isNull();
            return output.get();
        } finally {
            buffer.release();
            rsm.close();
        }
    }

}
