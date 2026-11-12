/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.cluster.ClusterReadOnlyCommands;

/**
 * Unit tests for {@link ReadOnlyCommands}.
 */
@Tag(UNIT_TEST)
class ReadOnlyCommandsUnitTests {

    /**
     * Commands that Redis flags as {@code readonly} so that {@code ReadFrom} can route them to a replica.
     */
    @Test
    void readOnlyKeyCommandsAreReadOnly() {

        for (CommandType type : new CommandType[] { CommandType.EXPIRETIME, CommandType.PEXPIRETIME, CommandType.HTTL,
                CommandType.HPTTL, CommandType.HEXPIRETIME, CommandType.HPEXPIRETIME, CommandType.LCS, CommandType.SINTERCARD,
                CommandType.ZDIFF, CommandType.ZINTER, CommandType.ZINTERCARD, CommandType.ZMSCORE, CommandType.ZUNION }) {
            assertThat(ReadOnlyCommands.isReadOnlyCommand(type)).as(type.toString()).isTrue();
            assertThat(ClusterReadOnlyCommands.isReadOnlyCommand(type)).as(type.toString()).isTrue();
        }
    }

    @Test
    void storeVariantsAreNotReadOnly() {

        for (CommandType type : new CommandType[] { CommandType.ZDIFFSTORE, CommandType.ZINTERSTORE, CommandType.ZUNIONSTORE,
                CommandType.SINTERSTORE, CommandType.HEXPIRE, CommandType.EXPIRE }) {
            assertThat(ReadOnlyCommands.isReadOnlyCommand(type)).as(type.toString()).isFalse();
            assertThat(ClusterReadOnlyCommands.isReadOnlyCommand(type)).as(type.toString()).isFalse();
        }
    }

}
