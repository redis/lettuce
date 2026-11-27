/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.RedisStateMachine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link NullableScoredValueListOutput}.
 */
@Tag(UNIT_TEST)
class NullableScoredValueListOutputUnitTests {

    private final NullableScoredValueListOutput<String, String> sut = new NullableScoredValueListOutput<>(StringCodec.UTF8);

    private static ByteBuffer bytes(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void resp2PairsAreDecodedInOrder() {

        sut.multi(2);
        sut.multi(2);
        sut.set(bytes("a"));
        sut.set(bytes("1"));
        sut.multi(2);
        sut.set(bytes("b"));
        sut.set(bytes("2.5"));

        assertThat(sut.get()).containsExactly(ScoredValue.just(1, "a"), ScoredValue.just(2.5, "b"));
    }

    @Test
    void resp3PairsAreDecodedInOrder() {

        sut.multi(1);
        sut.multi(2);
        sut.set(bytes("a"));
        sut.set(Double.POSITIVE_INFINITY);

        assertThat(sut.get()).containsExactly(ScoredValue.just(Double.POSITIVE_INFINITY, "a"));
    }

    @Test
    void resp2NullArrayStaysNull() {

        sut.multi(-1);

        assertThat(sut.get()).isNull();
    }

    @Test
    void resp3NullStaysNull() {

        sut.set((ByteBuffer) null);

        assertThat(sut.get()).isNull();
    }

    @Test
    void emptyArrayIsEmptyList() {

        sut.multi(0);

        assertThat(sut.get()).isEmpty();
    }

    private static final String RESP2_TWO_PAIRS = "*2\r\n*2\r\n$1\r\na\r\n$1\r\n1\r\n*2\r\n$1\r\nb\r\n$1\r\n2\r\n";

    private static List<ScoredValue<String>> decode(ProtocolVersion protocolVersion, String reply) {
        return decode(protocolVersion, reply, new NullableScoredValueListOutput<>(StringCodec.UTF8));
    }

    private static List<ScoredValue<String>> decode(ProtocolVersion protocolVersion, String reply,
            NullableScoredValueListOutput<String, String> output) {

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
     * A RESP2 reply of two nested pairs with bulk-string scores decodes to the pairs in reply order.
     */
    @Test
    void resp2TwoPairsDecodeInOrder() {

        assertThat(decode(ProtocolVersion.RESP2, RESP2_TWO_PAIRS)).containsExactly(ScoredValue.just(1.0, "a"),
                ScoredValue.just(2.0, "b"));
    }

    /**
     * A RESP3 reply of two nested pairs with double scores decodes to the same list as RESP2.
     */
    @Test
    void resp3TwoPairsDecodeLikeResp2() {

        String reply = "*2\r\n*2\r\n$1\r\na\r\n,1\r\n*2\r\n$1\r\nb\r\n,2\r\n";

        assertThat(decode(ProtocolVersion.RESP3, reply)).containsExactly(ScoredValue.just(1.0, "a"), ScoredValue.just(2.0, "b"))
                .isEqualTo(decode(ProtocolVersion.RESP2, RESP2_TWO_PAIRS));
    }

    /**
     * One nested pair decodes to a list of one, in both protocols.
     */
    @Test
    void onePairIsAListOfOne() {

        assertThat(decode(ProtocolVersion.RESP2, "*1\r\n*2\r\n$1\r\na\r\n$1\r\n1\r\n"))
                .containsExactly(ScoredValue.just(1.0, "a"));
        assertThat(decode(ProtocolVersion.RESP3, "*1\r\n*2\r\n$1\r\na\r\n,1\r\n")).containsExactly(ScoredValue.just(1.0, "a"));
    }

    /**
     * The RESP2 null array decodes to {@code null}, not to an empty list.
     */
    @Test
    void resp2NullArrayDecodesToNull() {

        assertThat(decode(ProtocolVersion.RESP2, "*-1\r\n")).isNull();
    }

    /**
     * The RESP3 null decodes to {@code null}.
     */
    @Test
    void resp3NullDecodesToNull() {

        assertThat(decode(ProtocolVersion.RESP3, "_\r\n")).isNull();
    }

    /**
     * Infinite scores decode to positive and negative infinity, in both protocols.
     */
    @Test
    void infiniteScoresDecode() {

        List<ScoredValue<String>> resp2 = decode(ProtocolVersion.RESP2,
                "*2\r\n*2\r\n$1\r\na\r\n$3\r\ninf\r\n*2\r\n$1\r\nb\r\n$4\r\n-inf\r\n");
        List<ScoredValue<String>> resp3 = decode(ProtocolVersion.RESP3,
                "*2\r\n*2\r\n$1\r\na\r\n,inf\r\n*2\r\n$1\r\nb\r\n,-inf\r\n");

        for (List<ScoredValue<String>> decoded : Arrays.asList(resp2, resp3)) {
            assertThat(decoded).hasSize(2);
            assertThat(decoded.get(0).getValue()).isEqualTo("a");
            assertThat(decoded.get(0).getScore()).isEqualTo(Double.POSITIVE_INFINITY);
            assertThat(decoded.get(1).getValue()).isEqualTo("b");
            assertThat(decoded.get(1).getScore()).isEqualTo(Double.NEGATIVE_INFINITY);
        }
    }

    /**
     * A small score written in plain decimal decodes to the number, in both protocols.
     */
    @Test
    void smallScoreDecodesAsNumber() {

        List<ScoredValue<String>> resp2 = decode(ProtocolVersion.RESP2, "*1\r\n*2\r\n$1\r\na\r\n$7\r\n0.00001\r\n");
        List<ScoredValue<String>> resp3 = decode(ProtocolVersion.RESP3, "*1\r\n*2\r\n$1\r\na\r\n,0.00001\r\n");

        assertThat(resp2.get(0).getScore()).isEqualTo(1.0E-5);
        assertThat(resp3.get(0).getScore()).isEqualTo(1.0E-5);
    }

    /**
     * A streaming subscriber receives each decoded pair in reply order.
     */
    @Test
    void subscriberReceivesPairsInOrder() {

        List<ScoredValue<String>> received = new ArrayList<>();
        NullableScoredValueListOutput<String, String> output = new NullableScoredValueListOutput<>(StringCodec.UTF8);
        output.setSubscriber(new StreamingOutput.Subscriber<ScoredValue<String>>() {

            @Override
            public void onNext(ScoredValue<String> item) {
                received.add(item);
            }

        });

        decode(ProtocolVersion.RESP2, RESP2_TWO_PAIRS, output);

        assertThat(received).containsExactly(ScoredValue.just(1.0, "a"), ScoredValue.just(2.0, "b"));
    }

}
