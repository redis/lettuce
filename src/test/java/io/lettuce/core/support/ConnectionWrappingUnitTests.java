/*
 * Copyright (c) 2026-Present, Redis Ltd. All rights reserved.
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.support;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.PushHandler;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;

/**
 * Unit tests for {@link ConnectionWrapping}: every command API handed out by a pooled connection must be pool-aware, i.e.
 * resolve back to the pooled proxy and return the connection to the pool on close.
 */
@Tag(UNIT_TEST)
@SuppressWarnings("deprecation")
class ConnectionWrappingUnitTests {

    private RedisChannelWriter writer;

    private StatefulRedisConnection<String, String> target;

    private StatefulRedisConnection<String, String> pooled;

    private final AtomicInteger returned = new AtomicInteger();

    private final AtomicInteger returnedAsync = new AtomicInteger();

    @BeforeEach
    void setup() {
        writer = mock(RedisChannelWriter.class);
        ClientResources resources = mock(ClientResources.class);
        Tracing tracing = mock(Tracing.class);
        when(resources.tracing()).thenReturn(tracing);
        when(tracing.isEnabled()).thenReturn(Boolean.FALSE);
        when(writer.getClientResources()).thenReturn(resources);

        target = new StatefulRedisConnectionImpl<>(writer, mock(PushHandler.class), StringCodec.UTF8, Duration.ofSeconds(5));

        pooled = ConnectionWrapping.wrapConnection(target,
                new ConnectionWrapping.Origin<StatefulRedisConnection<String, String>>() {

                    @Override
                    public void returnObject(StatefulRedisConnection<String, String> o) {
                        returned.incrementAndGet();
                    }

                    @Override
                    public CompletableFuture<Void> returnObjectAsync(StatefulRedisConnection<String, String> o) {
                        returnedAsync.incrementAndGet();
                        return CompletableFuture.completedFuture(null);
                    }

                });
    }

    @Test
    void reactiveIsWrapped() {
        RedisReactiveCommands<String, String> reactive = pooled.reactive();

        assertThat(reactive).isNotInstanceOf(RedisReactiveCommandsImpl.class);
        assertThat(reactive.getStatefulConnection()).isSameAs(pooled);
    }

    @Test
    void reactiveIsCachedPerPooledConnection() {
        assertThat(pooled.reactive()).isSameAs(pooled.reactive());
    }

    @Test
    void commandsIsWrapped() {
        RedisReactiveCommands<String, String> reactive = pooled.commands(RedisReactiveCommands.factory());

        assertThat(reactive).isNotInstanceOf(RedisReactiveCommandsImpl.class);
        assertThat(reactive.getStatefulConnection()).isSameAs(pooled);
    }

    @Test
    void commandsIsCachedPerPooledConnection() {
        assertThat(pooled.commands(RedisReactiveCommands.factory())).isSameAs(pooled.commands(RedisReactiveCommands.factory()));
    }

    @Test
    void closingViaReactiveReturnsToPool() {
        pooled.reactive().getStatefulConnection().close();

        assertThat(returned).hasValue(1);
        assertThat(returnedAsync).hasValue(0);
        verify(writer, never()).close();
        verify(writer, never()).closeAsync();
    }

    @Test
    void closeAsyncViaReactiveReturnsToPool() {
        pooled.reactive().getStatefulConnection().closeAsync().join();

        assertThat(returnedAsync).hasValue(1);
        assertThat(returned).hasValue(0);
        verify(writer, never()).close();
        verify(writer, never()).closeAsync();
    }

    @Test
    void reactiveIsUnusableAfterClose() {
        pooled.close();

        assertThatThrownBy(() -> pooled.reactive()).isInstanceOf(RedisException.class)
                .hasMessageContaining("Connection is deallocated");
    }

}
