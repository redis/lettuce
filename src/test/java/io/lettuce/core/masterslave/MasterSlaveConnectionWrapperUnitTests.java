/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.masterslave;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;

@Tag(UNIT_TEST)
class MasterSlaveConnectionWrapperUnitTests {

    @SuppressWarnings("unchecked")
    private final StatefulRedisMasterReplicaConnection<String, String> delegate = mock(
            StatefulRedisMasterReplicaConnection.class);

    private MasterSlaveConnectionWrapper<String, String> wrapper;

    @BeforeEach
    void setup() {
        wrapper = new MasterSlaveConnectionWrapper<>(delegate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void commandsDelegateToTheUnderlyingConnection() {
        RedisReactiveCommands<String, String> expected = mock(RedisReactiveCommands.class);
        when(delegate.commands(any())).thenReturn(expected);

        RedisReactiveCommands<String, String> actual = wrapper.commands(RedisReactiveCommands.factory());

        assertThat(actual).isSameAs(expected);
    }

}
