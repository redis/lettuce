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
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.CommandsFactoryProvider;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.cluster.ClusterPushHandler;
import io.lettuce.core.cluster.StatefulRedisClusterConnectionImpl;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.PushHandler;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;

/**
 * Demonstrates the {@code connection.commands(CommandsFactoryProvider.from(connection))} accessor variant: the family-precise
 * factory is resolved on the connection's static type, the command API is cached on the connection, and the return type is
 * precise per family.
 */
@Tag(UNIT_TEST)
class CommandsAccessorUnitTests {

    private final RedisCodec<String, String> codec = StringCodec.UTF8;

    private RedisChannelWriter writer;

    @BeforeEach
    void setup() {
        writer = mock(RedisChannelWriter.class);
        ClientResources resources = mock(ClientResources.class);
        Tracing tracing = mock(Tracing.class);
        when(resources.tracing()).thenReturn(tracing);
        when(tracing.isEnabled()).thenReturn(Boolean.FALSE);
        when(writer.getClientResources()).thenReturn(resources);
    }

    @Test
    void standaloneCommandsArePreciseAndCached() {
        StatefulRedisConnection<String, String> connection = new StatefulRedisConnectionImpl<>(writer, mock(PushHandler.class),
                codec, Duration.ofSeconds(5));

        RedisReactiveCommands<String, String> first = connection.commands(CommandsFactoryProvider.from(connection));
        RedisReactiveCommands<String, String> second = connection.commands(CommandsFactoryProvider.from(connection));

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
    }

    @Test
    void clusterCommandsArePreciseAndCached() {
        StatefulRedisClusterConnection<String, String> connection = new StatefulRedisClusterConnectionImpl<>(writer,
                mock(ClusterPushHandler.class), codec, Duration.ofSeconds(5));

        RedisAdvancedClusterReactiveCommands<String, String> first = connection
                .commands(CommandsFactoryProvider.from(connection));
        RedisAdvancedClusterReactiveCommands<String, String> second = connection
                .commands(CommandsFactoryProvider.from(connection));

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
    }

}
