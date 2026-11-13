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

}
