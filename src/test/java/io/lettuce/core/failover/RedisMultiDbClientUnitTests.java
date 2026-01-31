package io.lettuce.core.failover;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.internal.AsyncCloseable;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.ReflectionTestUtils;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Unit tests for {@link RedisClient}.
 *
 * @author Ali Takavci
 * @since 7.1
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@Tag(UNIT_TEST)
class RedisMultiDbClientUnitTests {

    @Mock
    ClientResources clientResources;

    @Mock(extraInterfaces = Closeable.class)
    AsyncCloseable asyncCloseable;

    @Test
    void shutdownShouldDeferResourcesShutdown() throws Exception {

        when(clientResources.eventExecutorGroup()).thenReturn(ImmediateEventExecutor.INSTANCE);

        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        when(asyncCloseable.closeAsync()).thenReturn(completableFuture);

        MultiDbClient redisClient = MultiDbClient.create(clientResources,
                MultiDbTestSupport.getDatabaseConfigs(RedisURI.create("redis://foo"), RedisURI.create("redis://bar")));

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

        MultiDbClient redisClient = MultiDbClient.create(clientResources,
                MultiDbTestSupport.getDatabaseConfigs(RedisURI.create("redis://foo"), RedisURI.create("redis://bar")));

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

    @Test
    void connectAsyncShouldRejectNullCodec() {
        MultiDbClient client = MultiDbClient.create(MultiDbTestSupport.DBs);

        try {
            assertThatThrownBy(() -> client.connectAsync(null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("codec must not be null");
        } finally {
            client.shutdown();
        }
    }

}
