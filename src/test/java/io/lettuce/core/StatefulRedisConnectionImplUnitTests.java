/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.PushHandler;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@Tag(UNIT_TEST)
class StatefulRedisConnectionImplUnitTests {

    private final RedisCodec<String, String> codec = StringCodec.UTF8;

    private RedisChannelWriter writer;

    private StatefulRedisConnection<String, String> connection;

    @BeforeEach
    void setup() {
        writer = mock(RedisChannelWriter.class);
        ClientResources resources = mock(ClientResources.class);
        Tracing tracing = mock(Tracing.class);
        when(resources.tracing()).thenReturn(tracing);
        when(tracing.isEnabled()).thenReturn(Boolean.FALSE);
        when(writer.getClientResources()).thenReturn(resources);

        connection = new StatefulRedisConnectionImpl<>(writer, mock(PushHandler.class), codec, Duration.ofSeconds(5));
    }

    @Test
    void reactiveCommandsAreCached() {
        RedisReactiveCommands<String, String> first = connection.commands(RedisReactiveCommands.factory());
        RedisReactiveCommands<String, String> second = connection.commands(RedisReactiveCommands.factory());

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
    }

    @Test
    void factoryIsSingleton() {
        assertThat(RedisReactiveCommands.factory()).isSameAs(RedisReactiveCommands.factory());
    }

    @Test
    @SuppressWarnings("deprecation")
    void factoryProducesSameTypeAsReactive() {
        assertThat(connection.commands(RedisReactiveCommands.factory()).getClass()).isSameAs(connection.reactive().getClass());
    }

    @Test
    @SuppressWarnings("unchecked")
    void activatedReplaysNonDiscardedFieldsets() {

        StatefulRedisConnectionImpl<String, String> impl = (StatefulRedisConnectionImpl<String, String>) connection;
        // Replace the auth handler so activation does not attempt a real credentials subscription.
        impl.setAuthenticationHandler(mock(RedisAuthenticationHandler.class));

        HashImport<String> active = HashImport.of(seq -> "active", "name", "email");
        HashImport<String> discarded = HashImport.of(seq -> "discarded", "sku");
        discarded.close();
        impl.getConnectionState().getHashImportRegistry().add(active);
        impl.getConnectionState().getHashImportRegistry().add(discarded);

        impl.activated();

        ArgumentCaptor<RedisCommand<String, String, ?>> captor = ArgumentCaptor.forClass(RedisCommand.class);
        verify(writer, atLeastOnce()).write(captor.capture());

        List<String> himportCommands = captor.getAllValues().stream().filter(c -> c.getType() == CommandType.HIMPORT)
                .map(StatefulRedisConnectionImplUnitTests::encode).collect(Collectors.toList());

        // Exactly one HIMPORT PREPARE, for the non-discarded fieldset; the discarded one is not replayed.
        assertThat(himportCommands).hasSize(1);
        assertThat(himportCommands.get(0)).contains("PREPARE").contains("active").contains("name").contains("email")
                .doesNotContain("discarded").doesNotContain("sku");
    }

    private static String encode(RedisCommand<String, String, ?> command) {
        ByteBuf buf = Unpooled.buffer();
        command.encode(buf);
        return buf.toString(StandardCharsets.UTF_8);
    }

}
