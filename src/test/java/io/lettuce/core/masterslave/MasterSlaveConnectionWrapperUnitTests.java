/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
