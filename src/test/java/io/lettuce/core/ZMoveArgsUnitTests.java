/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.Range.Boundary;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Unit tests for {@link ZMoveArgs}.
 */
@Tag(UNIT_TEST)
class ZMoveArgsUnitTests {

    private static String render(ZMoveArgs zMoveArgs) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        zMoveArgs.build(args);
        return args.toCommandString();
    }

    @Test
    void minWithoutCount() {
        assertThat(render(ZMoveArgs.Builder.min())).isEqualTo("MIN");
    }

    @Test
    void maxWithCount() {
        assertThat(render(ZMoveArgs.Builder.max().count(3))).isEqualTo("MAX COUNT 3");
    }

    @Test
    void byScoreEncodesBoundsAsRangeQueries() {
        assertThat(render(ZMoveArgs.Builder.byScore(Range.create(1, 5)))).isEqualTo("BYSCORE 1 5");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.from(Range.Boundary.excluding(1), Range.Boundary.unbounded()))))
                .isEqualTo("BYSCORE (1 +inf");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.unbounded()).count(2))).isEqualTo("BYSCORE -inf +inf COUNT 2");
    }

    @Test
    void countIsSentAsGiven() {
        assertThat(render(ZMoveArgs.Builder.min().count(0))).isEqualTo("MIN COUNT 0");
        assertThat(render(ZMoveArgs.Builder.min().count(-1))).isEqualTo("MIN COUNT -1");
    }

    @Test
    void byScoreRejectsNullRange() {
        assertThatThrownBy(() -> ZMoveArgs.Builder.byScore(null)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario ZMOV-A01: {@code MIN} and {@code MAX} without a count append the selection only, no {@code COUNT} token.
     */
    @Test
    void minAndMaxWithoutCountAppendNoCount() {
        assertThat(render(ZMoveArgs.Builder.min())).isEqualTo("MIN");
        assertThat(render(ZMoveArgs.Builder.max())).isEqualTo("MAX");
    }

    /**
     * Scenario ZMOV-A02: a count is appended after the selection, a count of 1 included.
     */
    @Test
    void countIsAppendedAfterSelectionIncludingOne() {
        assertThat(render(ZMoveArgs.Builder.min().count(2))).isEqualTo("MIN COUNT 2");
        assertThat(render(ZMoveArgs.Builder.max().count(1))).isEqualTo("MAX COUNT 1");
    }

    /**
     * Scenario ZMOV-A03: score ranges are written as {@code BYSCORE min max}, exclusive bounds with {@code (}, unbounded and
     * infinite bounds as {@code -inf} / {@code +inf}; no {@code COUNT} unless set.
     */
    @Test
    void byScoreRangeSpellings() {
        assertThat(render(ZMoveArgs.Builder.byScore(Range.create(1, 3)))).isEqualTo("BYSCORE 1 3");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.from(Boundary.excluding(1), Boundary.including(3)))))
                .isEqualTo("BYSCORE (1 3");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.unbounded()))).isEqualTo("BYSCORE -inf +inf");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.create(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY))))
                .isEqualTo("BYSCORE -inf +inf");
        assertThat(render(ZMoveArgs.Builder.byScore(
                Range.from(Boundary.excluding(Double.NEGATIVE_INFINITY), Boundary.excluding(Double.POSITIVE_INFINITY)))))
                        .isEqualTo("BYSCORE (-inf (+inf");
    }

    /**
     * Scenario ZMOV-A03a: bounds of other {@link Number} types are written with their {@code toString()} spelling.
     */
    @Test
    void byScoreBoundsOfOtherNumberTypes() {
        assertThat(render(ZMoveArgs.Builder
                .byScore(Range.<Number> from(Boundary.including(1), Boundary.including(Float.POSITIVE_INFINITY)))))
                        .isEqualTo("BYSCORE 1 Infinity");
        assertThat(render(ZMoveArgs.Builder
                .byScore(Range.<Number> from(Boundary.including(1), Boundary.excluding(new BigDecimal("1E+3"))))))
                        .isEqualTo("BYSCORE 1 (1E+3");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.create(new BigDecimal("0.0000001"), new BigDecimal("0.0000001")))))
                .isEqualTo("BYSCORE 1E-7 1E-7");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.create(-0.0, 1.0)))).isEqualTo("BYSCORE -0.0 1.0");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.create(Long.MIN_VALUE, Long.MAX_VALUE))))
                .isEqualTo("BYSCORE -9223372036854775808 9223372036854775807");
    }

    /**
     * Scenario ZMOV-A04: counts of 0, -1 and {@link Long#MAX_VALUE} are written as given on {@code MIN} and on {@code BYSCORE},
     * without an exception.
     */
    @Test
    void countIsNeverCheckedOnMinAndByScore() {
        assertThat(render(ZMoveArgs.Builder.min().count(0))).isEqualTo("MIN COUNT 0");
        assertThat(render(ZMoveArgs.Builder.min().count(-1))).isEqualTo("MIN COUNT -1");
        assertThat(render(ZMoveArgs.Builder.min().count(Long.MAX_VALUE))).isEqualTo("MIN COUNT 9223372036854775807");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.create(1, 3)).count(0))).isEqualTo("BYSCORE 1 3 COUNT 0");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.create(1, 3)).count(-1))).isEqualTo("BYSCORE 1 3 COUNT -1");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.create(1, 3)).count(Long.MAX_VALUE)))
                .isEqualTo("BYSCORE 1 3 COUNT 9223372036854775807");
    }

    /**
     * Scenario ZMOV-SV05: without a count no {@code COUNT} token is written, for {@code MIN} and for {@code BYSCORE}.
     */
    @Test
    void noCountMeansNoCountToken() {
        assertThat(render(ZMoveArgs.Builder.min())).doesNotContain("COUNT");
        assertThat(render(ZMoveArgs.Builder.byScore(Range.create(1, 4)))).isEqualTo("BYSCORE 1 4");
    }

}
