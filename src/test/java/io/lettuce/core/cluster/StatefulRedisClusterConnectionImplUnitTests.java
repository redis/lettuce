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
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;

@Tag(UNIT_TEST)
class StatefulRedisClusterConnectionImplUnitTests {

    private final RedisCodec<String, String> codec = StringCodec.UTF8;

    private StatefulRedisClusterConnection<String, String> connection;

    @BeforeEach
    void setup() {
        RedisChannelWriter writer = mock(RedisChannelWriter.class);
        ClientResources resources = mock(ClientResources.class);
        Tracing tracing = mock(Tracing.class);
        when(resources.tracing()).thenReturn(tracing);
        when(tracing.isEnabled()).thenReturn(Boolean.FALSE);
        when(writer.getClientResources()).thenReturn(resources);

        connection = new StatefulRedisClusterConnectionImpl<>(writer, mock(ClusterPushHandler.class), codec,
                Duration.ofSeconds(5));
    }

    @Test
    void reactiveCommandsAreCached() {
        RedisAdvancedClusterReactiveCommands<String, String> first = connection
                .commands(RedisAdvancedClusterReactiveCommands.factory());
        RedisAdvancedClusterReactiveCommands<String, String> second = connection
                .commands(RedisAdvancedClusterReactiveCommands.factory());

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
    }

    @Test
    void factoryIsSingleton() {
        assertThat(RedisAdvancedClusterReactiveCommands.factory()).isSameAs(RedisAdvancedClusterReactiveCommands.factory());
    }

    @Test
    @SuppressWarnings("deprecation")
    void factoryProducesSameTypeAsReactive() {
        assertThat(connection.commands(RedisAdvancedClusterReactiveCommands.factory()).getClass())
                .isSameAs(connection.reactive().getClass());
    }

}
