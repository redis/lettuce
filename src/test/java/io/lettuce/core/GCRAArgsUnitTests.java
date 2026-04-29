/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link GCRAArgs}.
 *
 * @author Aleksandar Todorov
 * @since 7.6
 */
@Tag(UNIT_TEST)
class GCRAArgsUnitTests {

    @Test
    void shouldBuildWithRequiredParameters() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        GCRAArgs.Builder.rate(5, 10, 60).build(args);

        assertThat(args.toCommandString()).isEqualTo("5 10 60.0");
    }

    @Test
    void shouldBuildWithTokens() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        GCRAArgs.Builder.rate(5, 10, 60).tokens(3).build(args);

        assertThat(args.toCommandString()).isEqualTo("5 10 60.0 TOKENS 3");
    }

    @Test
    void shouldBuildWithMinimalValues() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        GCRAArgs.Builder.rate(0, 1, 1.0).build(args);

        assertThat(args.toCommandString()).isEqualTo("0 1 1.0");
    }

    @Test
    void shouldBuildWithMaxPeriod() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        GCRAArgs.Builder.rate(0, 1, 1e12).build(args);

        assertThat(args.toCommandString()).contains("1.0E12");
    }

    @Test
    void shouldBuildWithTokensOne() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        GCRAArgs.Builder.rate(5, 10, 60).tokens(1).build(args);

        assertThat(args.toCommandString()).isEqualTo("5 10 60.0 TOKENS 1");
    }

    @Test
    void shouldRejectNegativeMaxBurst() {
        assertThatThrownBy(() -> GCRAArgs.Builder.rate(-1, 10, 60)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxBurst must be >= 0");
    }

    @Test
    void shouldRejectZeroTokensPerPeriod() {
        assertThatThrownBy(() -> GCRAArgs.Builder.rate(5, 0, 60)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokensPerPeriod must be >= 1");
    }

    @Test
    void shouldRejectNegativeTokensPerPeriod() {
        assertThatThrownBy(() -> GCRAArgs.Builder.rate(5, -1, 60)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokensPerPeriod must be >= 1");
    }

    @Test
    void shouldRejectPeriodBelowOne() {
        assertThatThrownBy(() -> GCRAArgs.Builder.rate(5, 10, 0.5)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("period must be >= 1.0");
    }

    @Test
    void shouldRejectPeriodAboveMax() {
        assertThatThrownBy(() -> GCRAArgs.Builder.rate(5, 10, 1e12 + 1)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("period must be <= 1e12");
    }

    @Test
    void shouldRejectZeroTokens() {
        assertThatThrownBy(() -> GCRAArgs.Builder.rate(5, 10, 60).tokens(0)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokens must be >= 1");
    }

    @Test
    void shouldRejectNegativeTokens() {
        assertThatThrownBy(() -> GCRAArgs.Builder.rate(5, 10, 60).tokens(-1)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokens must be >= 1");
    }

    @Test
    void shouldNotIncludeTokensWhenNotSet() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        GCRAArgs.Builder.rate(5, 10, 60).build(args);

        assertThat(args.toCommandString()).doesNotContain("TOKENS");
    }

}
