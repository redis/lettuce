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
 * Unit tests for {@link ZMoveMemberArgs}.
 */
@Tag(UNIT_TEST)
class ZMoveMemberArgsUnitTests {

    private static String render(ZMoveMemberArgs zMoveMemberArgs) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        zMoveMemberArgs.build(args);
        return args.toCommandString();
    }

    @Test
    void nx() {
        assertThat(render(ZMoveMemberArgs.Builder.nx())).isEqualTo("NX");
    }

    @Test
    void xx() {
        assertThat(render(ZMoveMemberArgs.Builder.xx())).isEqualTo("XX");
    }

    @Test
    void lastConditionWins() {
        assertThat(render(ZMoveMemberArgs.Builder.nx().xx())).isEqualTo("XX");
        assertThat(render(ZMoveMemberArgs.Builder.xx().nx())).isEqualTo("NX");
    }

}
