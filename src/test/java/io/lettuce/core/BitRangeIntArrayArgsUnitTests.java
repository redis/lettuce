/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Unit tests for {@link BitRangeIntArrayArgs}.
 */
@Tag(UNIT_TEST)
class BitRangeIntArrayArgsUnitTests {

    private static String render(BitRangeIntArrayArgs bitRangeIntArrayArgs) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        bitRangeIntArrayArgs.build(args);
        return args.toCommandString();
    }

    /**
     * Scenario A01: empty args emit no tokens.
     */
    @Test
    void emptyEmitsNoTokens() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        BitRangeIntArrayArgs.Builder.empty().build(args);

        assertThat(args.count()).isZero();
        assertThat(args.toCommandString()).isEmpty();
    }

    /**
     * Scenario A02: {@code bit()} emits {@code BIT}.
     */
    @Test
    void bitEmitsBit() {
        assertThat(render(BitRangeIntArrayArgs.Builder.bit())).isEqualTo("BIT");
        assertThat(render(BitRangeIntArrayArgs.Builder.empty().bit())).isEqualTo("BIT");
    }

    /**
     * Scenario A03: {@code byteUnit()} emits {@code BYTE}.
     */
    @Test
    void byteUnitEmitsByte() {
        assertThat(render(BitRangeIntArrayArgs.Builder.byteUnit())).isEqualTo("BYTE");
        assertThat(render(BitRangeIntArrayArgs.Builder.empty().byteUnit())).isEqualTo("BYTE");
    }

    /**
     * Scenario A04: {@code limit(5)} emits {@code LIMIT 5}.
     */
    @Test
    void limitEmitsLimitAndCount() {
        assertThat(render(BitRangeIntArrayArgs.Builder.limit(5))).isEqualTo("LIMIT 5");
        assertThat(render(BitRangeIntArrayArgs.Builder.empty().limit(5))).isEqualTo("LIMIT 5");
    }

    /**
     * Scenario A05: {@code limit(0)} emits {@code LIMIT 0}, not "no limit".
     */
    @Test
    void limitZeroEmitsLimitZero() {
        assertThat(render(BitRangeIntArrayArgs.Builder.limit(0))).isEqualTo("LIMIT 0");
        assertThat(render(BitRangeIntArrayArgs.Builder.empty().limit(0))).isEqualTo("LIMIT 0");
    }

    /**
     * Scenario A06: a negative limit is rejected with {@link IllegalArgumentException} and nothing is built.
     */
    @Test
    void negativeLimitIsRejected() {
        assertThatThrownBy(() -> BitRangeIntArrayArgs.Builder.limit(-1)).isInstanceOf(IllegalArgumentException.class);

        BitRangeIntArrayArgs args = BitRangeIntArrayArgs.Builder.empty();
        assertThatThrownBy(() -> args.limit(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(render(args)).isEmpty();
    }

    /**
     * Scenario A07: {@code limit(Long.MAX_VALUE)} emits the value unchanged.
     */
    @Test
    void limitMaxValueEmitsValueUnchanged() {
        assertThat(render(BitRangeIntArrayArgs.Builder.limit(Long.MAX_VALUE))).isEqualTo("LIMIT 9223372036854775807");
    }

    /**
     * Scenario A08: {@code zeros()} called twice emits one {@code ZEROS}.
     */
    @Test
    void zerosTwiceEmitsOneZeros() {
        assertThat(render(BitRangeIntArrayArgs.Builder.zeros().zeros())).isEqualTo("ZEROS");
    }

    /**
     * Scenario A09: the options are emitted in the order unit, {@code LIMIT}, {@code ZEROS}.
     */
    @Test
    void optionsAreEmittedInSyntaxOrder() {
        assertThat(render(BitRangeIntArrayArgs.Builder.bit().limit(100).zeros())).isEqualTo("BIT LIMIT 100 ZEROS");
        assertThat(render(BitRangeIntArrayArgs.Builder.zeros().limit(100).bit())).isEqualTo("BIT LIMIT 100 ZEROS");
    }

    /**
     * Scenario A10: {@code bit().byteUnit()} emits one unit token, {@code BYTE}.
     */
    @Test
    void lastUnitWinsAndIsEmittedOnce() {
        assertThat(render(BitRangeIntArrayArgs.Builder.bit().byteUnit())).isEqualTo("BYTE");
    }

    /**
     * Scenario A11: every emitted token is uppercase ASCII on the wire.
     */
    @Test
    void tokensAreUppercaseAscii() {
        assertThat(encode(BitRangeIntArrayArgs.Builder.bit().limit(1).zeros()))
                .isEqualTo("$3\r\nBIT\r\n$5\r\nLIMIT\r\n$1\r\n1\r\n$5\r\nZEROS\r\n");
        assertThat(encode(BitRangeIntArrayArgs.Builder.byteUnit())).isEqualTo("$4\r\nBYTE\r\n");
    }

    private static String encode(BitRangeIntArrayArgs bitRangeIntArrayArgs) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        bitRangeIntArrayArgs.build(args);
        ByteBuf buf = Unpooled.buffer();
        args.encode(buf);
        return buf.toString(StandardCharsets.US_ASCII);
    }

}
