/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.cluster.ClusterReadOnlyCommands;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ReadOnlyCommands;

/**
 * Read-only registration of the TimeSeries commands (design §5.4, NF.4).
 */
@Tag(UNIT_TEST)
class TsReadOnlyCommandsUnitTests {

    /**
     * Scenario K10 (TS.INFO): TS.INFO is read-only, so {@code ReadFrom.REPLICA} may route it to a replica.
     */
    @Test
    void infoIsReadOnly() {

        assertThat(ReadOnlyCommands.isReadOnlyCommand(CommandType.TS_INFO)).isTrue();
        assertThat(ClusterReadOnlyCommands.isReadOnlyCommand(CommandType.TS_INFO)).isTrue();
    }

    /**
     * Scenario K10: the writing lifecycle commands are not read-only.
     */
    @Test
    void writesAreNotReadOnly() {

        for (CommandType type : new CommandType[] { CommandType.TS_CREATE, CommandType.TS_ALTER, CommandType.TS_DEL }) {
            assertThat(ReadOnlyCommands.isReadOnlyCommand(type)).as(type.toString()).isFalse();
            assertThat(ClusterReadOnlyCommands.isReadOnlyCommand(type)).as(type.toString()).isFalse();
        }
    }

    /**
     * Scenario K10: the wire names of the lifecycle commands.
     */
    @Test
    void commandNames() {

        assertThat(CommandType.TS_CREATE.toString()).isEqualTo("TS.CREATE");
        assertThat(CommandType.TS_ALTER.toString()).isEqualTo("TS.ALTER");
        assertThat(CommandType.TS_DEL.toString()).isEqualTo("TS.DEL");
        assertThat(CommandType.TS_INFO.toString()).isEqualTo("TS.INFO");
    }

}
