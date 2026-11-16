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

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link SAddExArgs}.
 */
@Tag(UNIT_TEST)
class SAddExArgsUnitTests {

    @Test
    void shouldRenderNothingByDefault() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        new SAddExArgs().build(args);
        assertThat(args.toCommandString()).isEmpty();
    }

    @Test
    void shouldRenderEx() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SAddExArgs.Builder.ex(Duration.ofSeconds(10)).build(args);
        assertThat(args.toCommandString()).isEqualTo("EX 10");
    }

    @Test
    void shouldTruncateExToWholeSeconds() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SAddExArgs.Builder.ex(Duration.ofMillis(1500)).build(args);
        assertThat(args.toCommandString()).isEqualTo("EX 1");
    }

    @Test
    void shouldRenderPx() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SAddExArgs.Builder.px(Duration.ofMillis(1500)).build(args);
        assertThat(args.toCommandString()).isEqualTo("PX 1500");
    }

    @Test
    void shouldRenderExAt() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SAddExArgs.Builder.exAt(Instant.ofEpochMilli(1_700_000_000_999L)).build(args);
        assertThat(args.toCommandString()).isEqualTo("EXAT 1700000000");
    }

    @Test
    void shouldRenderPxAt() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SAddExArgs.Builder.pxAt(Instant.ofEpochMilli(1_700_000_000_999L)).build(args);
        assertThat(args.toCommandString()).isEqualTo("PXAT 1700000000999");
    }

    @Test
    void shouldRenderKeepttl() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SAddExArgs.Builder.keepttl().build(args);
        assertThat(args.toCommandString()).isEqualTo("KEEPTTL");
    }

    @Test
    void shouldRenderMnx() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SAddExArgs.Builder.mnx().build(args);
        assertThat(args.toCommandString()).isEqualTo("MNX");
    }

    @Test
    void shouldRenderMxx() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SAddExArgs.Builder.mxx().build(args);
        assertThat(args.toCommandString()).isEqualTo("MXX");
    }

    @Test
    void shouldRenderConditionBeforeExpiration() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SAddExArgs.Builder.ex(Duration.ofSeconds(10)).mnx().build(args);
        assertThat(args.toCommandString()).isEqualTo("MNX EX 10");
    }

    @Test
    void shouldPassConflictingTokensThrough() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        SAddExArgs.Builder.mnx().mxx().ex(Duration.ofSeconds(1)).px(Duration.ofMillis(2)).keepttl().build(args);
        assertThat(args.toCommandString()).isEqualTo("MNX MXX EX 1 PX 2 KEEPTTL");
    }

    @Test
    void shouldRejectNullDuration() {
        assertThatThrownBy(() -> SAddExArgs.Builder.ex(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SAddExArgs.Builder.px(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullInstant() {
        assertThatThrownBy(() -> SAddExArgs.Builder.exAt(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SAddExArgs.Builder.pxAt(null)).isInstanceOf(IllegalArgumentException.class);
    }

}
