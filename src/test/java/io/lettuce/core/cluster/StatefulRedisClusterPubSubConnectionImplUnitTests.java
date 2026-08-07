/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
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
import io.lettuce.core.cluster.pubsub.api.reactive.RedisClusterPubSubReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;

@Tag(UNIT_TEST)
class StatefulRedisClusterPubSubConnectionImplUnitTests {

    private final RedisCodec<String, String> codec = StringCodec.UTF8;

    private StatefulRedisClusterPubSubConnectionImpl<String, String> connection;

    @BeforeEach
    void setup() {
        RedisChannelWriter writer = mock(RedisChannelWriter.class);
        ClientResources resources = mock(ClientResources.class);
        Tracing tracing = mock(Tracing.class);
        when(resources.tracing()).thenReturn(tracing);
        when(tracing.isEnabled()).thenReturn(Boolean.FALSE);
        when(writer.getClientResources()).thenReturn(resources);

        connection = new StatefulRedisClusterPubSubConnectionImpl<>(mock(PubSubClusterEndpoint.class),
                mock(ClusterPushHandler.class), writer, codec, Duration.ofSeconds(5));
    }

    @Test
    void reactiveCommandsAreCached() {
        RedisClusterPubSubReactiveCommands<String, String> first = connection
                .commands(RedisClusterPubSubReactiveCommands.factory());
        RedisClusterPubSubReactiveCommands<String, String> second = connection
                .commands(RedisClusterPubSubReactiveCommands.factory());

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
    }

    @Test
    void factoryIsSingleton() {
        assertThat(RedisClusterPubSubReactiveCommands.factory()).isSameAs(RedisClusterPubSubReactiveCommands.factory());
    }

    @Test
    void factoryProducesReactiveCommands() {
        assertThat(connection.commands(RedisClusterPubSubReactiveCommands.factory()))
                .isInstanceOf(RedisClusterPubSubReactiveCommandsImpl.class);
    }

}
