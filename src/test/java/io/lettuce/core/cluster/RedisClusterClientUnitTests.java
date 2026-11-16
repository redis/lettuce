package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * Unit tests for {@link RedisClusterClient}.
 */
@Tag(UNIT_TEST)
class RedisClusterClientUnitTests {

    @Test
    void connectAsyncShouldNotWrapConnectFailureInCompletionException() {

        RedisConnectionException lookupFailure = new RedisConnectionException("Cannot resolve node address");
        RedisClusterClient clusterClient = clientFailingSocketAddressLookup(lookupFailure);

        try {
            CompletableFuture<?> future = clusterClient.connectAsync(StringCodec.UTF8);

            assertConnectFailure(future, lookupFailure);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    @Test
    void connectPubSubAsyncShouldNotWrapConnectFailureInCompletionException() {

        RedisConnectionException lookupFailure = new RedisConnectionException("Cannot resolve node address");
        RedisClusterClient clusterClient = clientFailingSocketAddressLookup(lookupFailure);

        try {
            CompletableFuture<?> future = clusterClient.connectPubSubAsync(StringCodec.UTF8);

            assertConnectFailure(future, lookupFailure);
        } finally {
            FastShutdown.shutdown(clusterClient);
        }
    }

    private static RedisClusterClient clientFailingSocketAddressLookup(RedisConnectionException lookupFailure) {

        RedisClusterClient clusterClient = new RedisClusterClient(TestClientResources.get(),
                Collections.singletonList(RedisURI.create("redis://foo"))) {

            @Override
            protected Supplier<CompletionStage<SocketAddress>> getSocketAddressSupplier(Supplier<Partitions> partitionsSupplier,
                    Function<Partitions, Collection<RedisClusterNode>> sortFunction) {
                return () -> Futures.failed(lookupFailure);
            }

        };
        clusterClient.setPartitions(new Partitions());
        return clusterClient;
    }

    private static void assertConnectFailure(CompletableFuture<?> future, RedisConnectionException lookupFailure) {

        Throwable error = catchThrowable(() -> future.get(5, TimeUnit.SECONDS));

        assertThat(error).isInstanceOf(ExecutionException.class);
        assertThat(error.getCause()).isInstanceOf(RedisConnectionException.class)
                .hasMessage("Unable to connect to [redis://foo]").hasCause(lookupFailure);
        assertThat(causeChain(error.getCause())).doesNotHaveAnyElementsOfTypes(CompletionException.class);
    }

    private static List<Throwable> causeChain(Throwable throwable) {

        List<Throwable> chain = new ArrayList<>();
        for (Throwable t = throwable; t != null && !chain.contains(t); t = t.getCause()) {
            chain.add(t);
        }
        return chain;
    }

}
