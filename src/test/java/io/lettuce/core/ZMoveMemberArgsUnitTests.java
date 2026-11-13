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

    /**
     * Scenario ZMOV-A07c: the type holds one condition, so {@code NX} with {@code XX} can't be written: the last call wins.
     */
    @Test
    void nxWithXxNeverReachesTheWire() {
        assertThat(render(new ZMoveMemberArgs().nx().xx())).isEqualTo("XX");
        assertThat(render(new ZMoveMemberArgs().xx().nx())).isEqualTo("NX");
    }

    /**
     * Scenario ZMOV-SV04: NX and XX together are unrepresentable in the type: however the setters are chained, one condition is
     * written.
     */
    @Test
    void conditionHasOneSlot() {
        assertThat(render(ZMoveMemberArgs.Builder.nx().xx().nx())).isEqualTo("NX");
        assertThat(render(ZMoveMemberArgs.Builder.xx().nx().xx())).isEqualTo("XX");
    }

}
