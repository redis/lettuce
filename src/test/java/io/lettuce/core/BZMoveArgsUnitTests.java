/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link BZMoveArgs}.
 */
@Tag(UNIT_TEST)
class BZMoveArgsUnitTests {

    private static String render(BZMoveArgs bzMoveArgs) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        bzMoveArgs.build(args);
        return args.toCommandString();
    }

    @Test
    void defaultTimeoutIsZero() {
        assertThat(render(BZMoveArgs.Builder.min())).isEqualTo("MIN 0");
    }

    @Test
    void longTimeoutAndCount() {
        assertThat(render(BZMoveArgs.Builder.max().timeout(2).count(5))).isEqualTo("MAX 2 COUNT 5");
    }

    @Test
    void doubleTimeoutKeepsFraction() {
        assertThat(render(BZMoveArgs.Builder.min().timeout(0.5))).isEqualTo("MIN 0.5");
    }

    @Test
    void lastTimeoutWins() {
        assertThat(render(BZMoveArgs.Builder.min().timeout(0.5).timeout(3))).isEqualTo("MIN 3");
        assertThat(render(BZMoveArgs.Builder.min().timeout(3).timeout(0.25))).isEqualTo("MIN 0.25");
    }

    /**
     * Scenario ZMOV-A05: the timeout follows the end, with its fraction, and comes before {@code COUNT}; without a timeout
     * {@code 0} is written.
     */
    @Test
    void timeoutEncoding() {
        assertThat(render(BZMoveArgs.Builder.min().timeout(0.1))).isEqualTo("MIN 0.1");
        assertThat(render(BZMoveArgs.Builder.max().timeout(1L))).isEqualTo("MAX 1");
        assertThat(render(BZMoveArgs.Builder.min())).isEqualTo("MIN 0");
        assertThat(render(BZMoveArgs.Builder.min().timeout(0.0))).isEqualTo("MIN 0.0");
        assertThat(render(BZMoveArgs.Builder.min().timeout(5.0E-4))).isEqualTo("MIN 5.0E-4");
        assertThat(render(BZMoveArgs.Builder.min().timeout(0.1).count(5))).isEqualTo("MIN 0.1 COUNT 5");
    }

    /**
     * Scenario ZMOV-A06: negative timeouts and a count of 0 are written as given, without an exception.
     */
    @Test
    void negativeTimeoutAndZeroCountAreNotChecked() {
        assertThat(render(BZMoveArgs.Builder.min().timeout(-1L))).isEqualTo("MIN -1");
        assertThat(render(BZMoveArgs.Builder.min().timeout(-0.5))).isEqualTo("MIN -0.5");
        assertThat(render(BZMoveArgs.Builder.min().timeout(1L).count(0))).isEqualTo("MIN 1 COUNT 0");
    }

    /**
     * Scenario ZMOV-A06b: infinite and NaN timeouts are written with the {@link Double#toString(double)} spelling, without an
     * exception.
     */
    @Test
    void infiniteAndNanTimeoutsAreWrittenAsGiven() {
        assertThat(render(BZMoveArgs.Builder.min().timeout(Double.POSITIVE_INFINITY))).isEqualTo("MIN Infinity");
        assertThat(render(BZMoveArgs.Builder.min().timeout(Double.NaN))).isEqualTo("MIN NaN");
    }

    /**
     * Scenario ZMOV-A06c: the last timeout setter wins, whichever of the two forms it is.
     */
    @Test
    void lastTimeoutSetterWins() {
        assertThat(render(BZMoveArgs.Builder.min().timeout(2L).timeout(0.5))).isEqualTo("MIN 0.5");
        assertThat(render(BZMoveArgs.Builder.min().timeout(0.5).timeout(2L))).isEqualTo("MIN 2");
    }

    /**
     * Scenario ZMOV-SV01: a count of 0 or below is written as given for the blocking move, without an exception.
     */
    @Test
    void countBelowOneIsWritten() {
        assertThat(render(BZMoveArgs.Builder.max().timeout(1L).count(-3))).isEqualTo("MAX 1 COUNT -3");
    }

    /**
     * Scenario ZMOV-SV02: a negative timeout is written as given, without an exception.
     */
    @Test
    void negativeTimeoutIsWritten() {
        assertThat(render(BZMoveArgs.Builder.max().timeout(-2.25))).isEqualTo("MAX -2.25");
    }

}
