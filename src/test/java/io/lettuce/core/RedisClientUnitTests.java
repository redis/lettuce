package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.AsyncCloseable;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.ReflectionTestUtils;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Unit tests for {@link RedisClient}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@Tag(UNIT_TEST)
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

    @Test
    void connectAsyncShouldNotWrapSocketAddressResolutionFailureInCompletionException() throws Exception {

        RedisConnectionException lookupFailure = new RedisConnectionException("Cannot resolve master address");

        RedisClient redisClient = new RedisClient(TestClientResources.get(), RedisURI.create("redis://foo")) {

            @Override
            protected Supplier<CompletionStage<SocketAddress>> getSocketAddress(RedisURI redisURI) {
                return () -> Futures.failed(lookupFailure);
            }

        };

        try {
            ConnectionFuture<?> future = redisClient.connectAsync(StringCodec.UTF8, RedisURI.create("redis://foo"));

            Throwable error = catchThrowable(() -> future.get(5, TimeUnit.SECONDS));

            assertThat(error).isInstanceOf(ExecutionException.class);
            assertThat(error.getCause()).isInstanceOf(RedisConnectionException.class)
                    .hasMessage("Cannot resolve master address");
            assertThat(causeChain(error.getCause())).doesNotHaveAnyElementsOfTypes(CompletionException.class);
        } finally {
            FastShutdown.shutdown(redisClient);
        }
    }

    @Test
    void connectAsyncShouldFailFutureWhenSocketAddressSupplierThrows() throws Exception {

        RedisConnectionException lookupFailure = new RedisConnectionException("Cannot resolve master address");

        RedisClient redisClient = new RedisClient(TestClientResources.get(), RedisURI.create("redis://foo")) {

            @Override
            protected Supplier<CompletionStage<SocketAddress>> getSocketAddress(RedisURI redisURI) {
                return () -> {
                    throw lookupFailure;
                };
            }

        };

        try {
            // must not throw at the call site; the failure is reported through the returned future
            ConnectionFuture<?> future = redisClient.connectAsync(StringCodec.UTF8, RedisURI.create("redis://foo"));

            Throwable error = catchThrowable(() -> future.get(5, TimeUnit.SECONDS));

            assertThat(error).isInstanceOf(ExecutionException.class);
            assertThat(error.getCause()).isInstanceOf(RedisConnectionException.class)
                    .hasMessage("Cannot resolve master address");
            assertThat(causeChain(error.getCause())).doesNotHaveAnyElementsOfTypes(CompletionException.class);
        } finally {
            FastShutdown.shutdown(redisClient);
        }
    }

    private static List<Throwable> causeChain(Throwable throwable) {

        List<Throwable> chain = new ArrayList<>();
        for (Throwable t = throwable; t != null && !chain.contains(t); t = t.getCause()) {
            chain.add(t);
        }
        return chain;
    }

}
