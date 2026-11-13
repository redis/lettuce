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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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

}
