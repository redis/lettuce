/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.sentinel;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.lettuce.core.tracing.Tracing;

@Tag(UNIT_TEST)
class StatefulRedisSentinelConnectionImplUnitTests {

    private final RedisCodec<String, String> codec = StringCodec.UTF8;

    private StatefulRedisSentinelConnection<String, String> connection;

    @BeforeEach
    void setup() {
        RedisChannelWriter writer = mock(RedisChannelWriter.class);
        ClientResources resources = mock(ClientResources.class);
        Tracing tracing = mock(Tracing.class);
        when(resources.tracing()).thenReturn(tracing);
        when(tracing.isEnabled()).thenReturn(Boolean.FALSE);
        when(writer.getClientResources()).thenReturn(resources);

        connection = new StatefulRedisSentinelConnectionImpl<>(writer, codec, Duration.ofSeconds(5));
    }

    @Test
    void reactiveCommandsAreCached() {
        RedisSentinelReactiveCommands<String, String> first = connection.commands(RedisSentinelReactiveCommands.factory());
        RedisSentinelReactiveCommands<String, String> second = connection.commands(RedisSentinelReactiveCommands.factory());

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
    }

    @Test
    void factoryIsSingleton() {
        assertThat(RedisSentinelReactiveCommands.factory()).isSameAs(RedisSentinelReactiveCommands.factory());
    }

    @Test
    void factoryProducesReactiveCommands() {
        assertThat(connection.commands(RedisSentinelReactiveCommands.factory()))
                .isInstanceOf(RedisSentinelReactiveCommandsImpl.class);
    }

}
