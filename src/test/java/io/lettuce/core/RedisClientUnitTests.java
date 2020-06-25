/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import io.lettuce.core.internal.AsyncCloseable;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Unit tests for {@link RedisClient}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class RedisClientUnitTests {

    @Mock
    ClientResources clientResources;

    @Mock(extraInterfaces = Closeable.class)
    AsyncCloseable asyncCloseable;

    @Test
    void shutdownShouldDeferResourcesShutdown() {

        when(clientResources.eventExecutorGroup()).thenReturn(ImmediateEventExecutor.INSTANCE);

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        when(asyncCloseable.closeAsync()).thenReturn(completableFuture);

        RedisClient redisClient = RedisClient.create(clientResources, "redis://foo");
        ReflectionTestUtils.setField(redisClient, "sharedResources", false);

        Set<AsyncCloseable> closeableResources = (Set) ReflectionTestUtils.getField(redisClient, "closeableResources");
        closeableResources.add(asyncCloseable);

        CompletableFuture<Void> future = redisClient.shutdownAsync();

        verify(asyncCloseable).closeAsync();
        verify(clientResources, never()).shutdown(anyLong(), anyLong(), any());
        assertThat(future).isNotDone();
    }

    @Test
    void shutdownShutsDownResourcesAfterChannels() {

        when(clientResources.eventExecutorGroup()).thenReturn(ImmediateEventExecutor.INSTANCE);

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        when(asyncCloseable.closeAsync()).thenReturn(completableFuture);

        RedisClient redisClient = RedisClient.create(clientResources, "redis://foo");
        ReflectionTestUtils.setField(redisClient, "sharedResources", false);

        Set<AsyncCloseable> closeableResources = (Set) ReflectionTestUtils.getField(redisClient, "closeableResources");
        closeableResources.add(asyncCloseable);

        CompletableFuture<Void> future = redisClient.shutdownAsync();

        verify(asyncCloseable).closeAsync();
        verify(clientResources, never()).shutdown(anyLong(), anyLong(), any());

        completableFuture.complete(null);

        verify(clientResources).shutdown(anyLong(), anyLong(), any());
        assertThat(future).isDone();
    }

}
