/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.timeseries.arguments.TsArgsTestSupport.tokens;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsReadArgs}: {@code BLOCK ms min} and {@code MAX_COUNT n}, and their validation (design §3.3).
 */
@Tag(UNIT_TEST)
class TsReadArgsUnitTests {

    /**
     * Scenario A11: no option set sends nothing.
     */
    @Test
    void emptyArgsSendNothing() {
        assertThat(tokens(new TsReadArgs())).isEmpty();
    }

    /**
     * Scenario A11: {@code BLOCK ms min} is sent before {@code MAX_COUNT n}, in either setter order.
     */
    @Test
    void blockBeforeMaxCount() {

        assertThat(tokens(new TsReadArgs().block(300, 2).maxCount(5))).containsExactly("BLOCK", "300", "2", "MAX_COUNT", "5");
        assertThat(tokens(new TsReadArgs().maxCount(5).block(300, 2))).containsExactly("BLOCK", "300", "2", "MAX_COUNT", "5");
    }

    /**
     * Scenario A11: each option on its own; the builder factories give the same tokens.
     */
    @Test
    void singleOptionsAndBuilder() {

        assertThat(tokens(new TsReadArgs().block(100, 1))).containsExactly("BLOCK", "100", "1");
        assertThat(tokens(new TsReadArgs().maxCount(3))).containsExactly("MAX_COUNT", "3");
        assertThat(tokens(TsReadArgs.Builder.block(100, 1))).containsExactly("BLOCK", "100", "1");
        assertThat(tokens(TsReadArgs.Builder.block(Duration.ofMillis(250), 4))).containsExactly("BLOCK", "250", "4");
        assertThat(tokens(TsReadArgs.Builder.maxCount(3))).containsExactly("MAX_COUNT", "3");
    }

    /**
     * Scenario A17 (READ): {@code BLOCK 0} (wait without a time limit) is allowed; a negative block time throws.
     */
    @Test
    void blockTime() {

        assertThat(tokens(new TsReadArgs().block(0, 1))).containsExactly("BLOCK", "0", "1");
        assertThat(tokens(new TsReadArgs().block(Duration.ZERO, 1))).containsExactly("BLOCK", "0", "1");

        assertThatThrownBy(() -> new TsReadArgs().block(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsReadArgs.Builder.block(-1, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A17 (READ): the {@code Duration} form sends whole milliseconds; a non-zero duration under one millisecond, a
     * negative duration and {@code null} throw.
     */
    @Test
    void blockDuration() {

        assertThat(tokens(new TsReadArgs().block(Duration.ofSeconds(1), 2))).containsExactly("BLOCK", "1000", "2");

        assertThatThrownBy(() -> new TsReadArgs().block(Duration.ofNanos(500_000), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsReadArgs().block(Duration.ofMillis(-5), 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsReadArgs().block((Duration) null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsReadArgs.Builder.block(Duration.ofNanos(1), 1)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A17 (READ): {@code minCount} ≤ 0 throws.
     */
    @Test
    void minCountMustBePositive() {

        assertThatThrownBy(() -> new TsReadArgs().block(100, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsReadArgs().block(100, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsReadArgs().block(Duration.ofMillis(100), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsReadArgs.Builder.block(100, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A17 (READ): {@code maxCount} ≤ 0 throws.
     */
    @Test
    void maxCountMustBePositive() {

        assertThatThrownBy(() -> new TsReadArgs().maxCount(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsReadArgs().maxCount(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsReadArgs.Builder.maxCount(0)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A17 (READ): {@code maxCount < minCount} throws in both setter orders; equal counts are allowed.
     */
    @Test
    void maxCountBelowMinCount() {

        assertThatThrownBy(() -> new TsReadArgs().block(100, 5).maxCount(3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsReadArgs().maxCount(3).block(100, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsReadArgs().maxCount(3).block(Duration.ofMillis(100), 5))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(tokens(new TsReadArgs().block(100, 3).maxCount(3))).containsExactly("BLOCK", "100", "3", "MAX_COUNT", "3");
        assertThat(tokens(new TsReadArgs().maxCount(3).block(100, 3))).containsExactly("BLOCK", "100", "3", "MAX_COUNT", "3");
    }

}
