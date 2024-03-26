package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.internal.AsyncCloseable;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.ReflectionTestUtils;
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
    void shutdownShouldDeferResourcesShutdown() throws Exception {

        when(clientResources.eventExecutorGroup()).thenReturn(ImmediateEventExecutor.INSTANCE);

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        when(asyncCloseable.closeAsync()).thenReturn(completableFuture);

        RedisClient redisClient = RedisClient.create(clientResources, "redis://foo");

        Field field = AbstractRedisClient.class.getDeclaredField("sharedResources");
        field.setAccessible(true);
        field.set(redisClient, false);

        Set<AsyncCloseable> closeableResources = (Set) ReflectionTestUtils.getField(redisClient, "closeableResources");
        closeableResources.add(asyncCloseable);

        CompletableFuture<Void> future = redisClient.shutdownAsync();

        verify(asyncCloseable).closeAsync();
        verify(clientResources, never()).shutdown(anyLong(), anyLong(), any());
        assertThat(future).isNotDone();
    }

    @Test
    void shutdownShutsDownResourcesAfterChannels() throws Exception {

        when(clientResources.eventExecutorGroup()).thenReturn(ImmediateEventExecutor.INSTANCE);

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        when(asyncCloseable.closeAsync()).thenReturn(completableFuture);

        RedisClient redisClient = RedisClient.create(clientResources, "redis://foo");

        Field field = AbstractRedisClient.class.getDeclaredField("sharedResources");
        field.setAccessible(true);
        field.set(redisClient, false);

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
