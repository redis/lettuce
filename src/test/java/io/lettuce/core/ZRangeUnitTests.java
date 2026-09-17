/*
 * Copyright 2026, Redis Ltd. and Contributors
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

/**
 * Unit tests for {@link ZRange}.
 */
@Tag(UNIT_TEST)
class ZRangeUnitTests {

    @Test
    void byIndexShouldCaptureBoundariesAndRev() {

        ZRange.ByIndex range = ZRange.byIndex(0, -1);

        assertThat(range.getStart()).isEqualTo(0);
        assertThat(range.getStop()).isEqualTo(-1);
        assertThat(range.isRev()).isFalse();

        assertThat(range.rev()).isSameAs(range);
        assertThat(range.isRev()).isTrue();
    }

    @Test
    void byScoreShouldCaptureRangeRevAndLimit() {

        ZRange.ByScore range = ZRange.byScore(Range.create(1, 10));

        assertThat(range.getRange()).isEqualTo(Range.create(1, 10));
        assertThat(range.isRev()).isFalse();
        assertThat(range.getLimit().isLimited()).isFalse();

        range.rev().limit(2, 5);

        assertThat(range.isRev()).isTrue();
        assertThat(range.getLimit().getOffset()).isEqualTo(2);
        assertThat(range.getLimit().getCount()).isEqualTo(5);
    }

    @Test
    void byScoreDoublesShouldBeInclusive() {

        ZRange.ByScore range = ZRange.byScore(1.5, 10);

        assertThat(range.getRange().getLower().getValue()).isEqualTo(1.5);
        assertThat(range.getRange().getLower().isIncluding()).isTrue();
        assertThat(range.getRange().getUpper().getValue()).isEqualTo(10.0);
        assertThat(range.getRange().getUpper().isIncluding()).isTrue();
    }

    @Test
    void byScoreShouldRejectNaN() {

        assertThatThrownBy(() -> ZRange.byScore(Range.create(1.0, Double.NaN))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZRange.byScore(Double.NaN, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void byScoreShouldAcceptInfinityAndUnbounded() {

        assertThat(ZRange.byScore(Range.create(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)).getRange())
                .isEqualTo(Range.create(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertThat(ZRange.byScore(Range.unbounded()).getRange().isUnbounded()).isTrue();
    }

    @Test
    void byLexShouldCaptureRangeRevAndLimit() {

        ZRange.ByLex<String> range = ZRange.byLex(Range.create("a", "z"));

        assertThat(range.getRange()).isEqualTo(Range.create("a", "z"));
        assertThat(range.isRev()).isFalse();
        assertThat(range.getLimit().isLimited()).isFalse();

        range.rev().limit(Limit.create(0, 3));

        assertThat(range.isRev()).isTrue();
        assertThat(range.getLimit().getCount()).isEqualTo(3);
    }

    @Test
    void selectorsShouldCopyTheRange() {

        Range<Integer> scores = Range.create(1, 10);
        ZRange.ByScore byScore = ZRange.byScore(scores);
        scores.gt(5);

        assertThat(byScore.getRange()).isEqualTo(Range.create(1, 10));

        Range<String> lex = Range.create("a", "z");
        ZRange.ByLex<String> byLex = ZRange.byLex(lex);
        lex.lt("m");

        assertThat(byLex.getRange()).isEqualTo(Range.create("a", "z"));
    }

    @Test
    void shouldRejectNullArguments() {

        assertThatThrownBy(() -> ZRange.byScore(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZRange.byLex((Range<String>) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZRange.byScore(Range.unbounded()).limit(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZRange.byLex(Range.<String> unbounded()).limit(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRenderToString() {

        assertThat(ZRange.byIndex(0, 9).rev().toString()).isEqualTo("ByIndex [0 to 9, REV]");
        assertThat(ZRange.byScore(Range.create(1, 2)).toString()).isEqualTo("ByScore [" + Range.create(1, 2) + "]");
        assertThat(ZRange.byLex(Range.create("a", "b")).rev().limit(0, 1).toString())
                .isEqualTo("ByLex [" + Range.create("a", "b") + ", REV, " + Limit.create(0, 1) + "]");
    }

}
