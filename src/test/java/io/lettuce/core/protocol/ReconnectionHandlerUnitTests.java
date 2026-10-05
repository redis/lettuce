package io.lettuce.core.protocol;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.Pair;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.internal.Futures;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

/**
 * Unit tests for {@link ReconnectionHandler}.
 */
@Tag(UNIT_TEST)
class ReconnectionHandlerUnitTests {

    @Test
    void reconnectShouldNotWrapSocketAddressFailureInCompletionException() {

        RedisConnectionException lookupFailure = new RedisConnectionException("Cannot resolve master address");

        ReconnectionHandler handler = new ReconnectionHandler(ClientOptions.create(), new Bootstrap(),
                () -> Futures.failed(lookupFailure));

        Pair<CompletableFuture<Channel>, CompletableFuture<SocketAddress>> result = handler.reconnect();

        assertThat(failureOf(result.getT1())).isSameAs(lookupFailure);
        assertThat(failureOf(result.getT2())).isSameAs(lookupFailure);
    }

    @Test
    void reconnectShouldNotWrapSynchronousConnectFailureInCompletionException() {

        // A Bootstrap without an EventLoopGroup makes bootstrap.connect(...) throw IllegalStateException synchronously.
        ReconnectionHandler handler = new ReconnectionHandler(ClientOptions.create(), new Bootstrap(),
                () -> CompletableFuture.completedFuture(InetSocketAddress.createUnresolved("localhost", 6379)));

        Pair<CompletableFuture<Channel>, CompletableFuture<SocketAddress>> result = handler.reconnect();

        assertThat(failureOf(result.getT1())).isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(CompletionException.class);
    }

    @Test
    void reconnectShouldCompleteWhenFailureIsCompletionExceptionWithoutCause() {

        CompletionException causeless = new CompletionException("no cause", null);

        ReconnectionHandler handler = new ReconnectionHandler(ClientOptions.create(), new Bootstrap(),
                () -> Futures.failed(causeless));

        Pair<CompletableFuture<Channel>, CompletableFuture<SocketAddress>> result = handler.reconnect();

        assertThat(failureOf(result.getT1())).isSameAs(causeless);
        assertThat(failureOf(result.getT2())).isSameAs(causeless);
    }

    private static Throwable failureOf(CompletableFuture<?> future) {

        AtomicReference<Throwable> failure = new AtomicReference<>();
        future.whenComplete((value, error) -> failure.set(error));

        assertThat(future).isDone();
        return failure.get();
    }

}
