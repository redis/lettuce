/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.RedisStateMachine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link NullableScoredValueOutput}.
 */
@Tag(UNIT_TEST)
class NullableScoredValueOutputUnitTests {

    private static ScoredValue<String> decode(ProtocolVersion protocolVersion, String reply) {

        NullableScoredValueOutput<String, String> output = new NullableScoredValueOutput<>(StringCodec.UTF8);
        RedisStateMachine rsm = new RedisStateMachine();
        rsm.setProtocolVersion(protocolVersion);
        ByteBuf buffer = Unpooled.copiedBuffer(reply, StandardCharsets.UTF_8);
        try {
            assertThat(rsm.decode(buffer, output)).isTrue();
            assertThat(output.getError()).isNull();
            return output.get();
        } finally {
            buffer.release();
            rsm.close();
        }
    }

    /**
     * An array of one pair decodes to the pair itself, with a bulk-string score in RESP2 and a double in RESP3.
     */
    @Test
    void onePairDecodesToTheScoredValue() {

        assertThat(decode(ProtocolVersion.RESP2, "*1\r\n*2\r\n$1\r\na\r\n$1\r\n1\r\n")).isEqualTo(ScoredValue.just(1.0, "a"));
        assertThat(decode(ProtocolVersion.RESP3, "*1\r\n*2\r\n$1\r\na\r\n,1\r\n")).isEqualTo(ScoredValue.just(1.0, "a"));
    }

    /**
     * The RESP2 null array and the RESP3 null decode to {@code null}, not to an empty scored value.
     */
    @Test
    void nullReplyDecodesToNull() {

        assertThat(decode(ProtocolVersion.RESP2, "*-1\r\n")).isNull();
        assertThat(decode(ProtocolVersion.RESP3, "_\r\n")).isNull();
    }

}
