package io.lettuce.core.cluster;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Mono;

import io.lettuce.core.ConnectionState;
import io.lettuce.core.RedisURI;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.universal.StatefulRedisUniversalConnection;
import io.lettuce.core.universal.StatefulRedisUniversalConnectionImpl;
import io.lettuce.core.universal.TopologyMode;

/**
 * Orchestrates one universal connect: probe, detect, open the matching real connection, wrap it. Mirrors the role of
 * {@code AbstractRedisMultiDbConnectionBuilder}: it does not build channels, it drives {@link RedisClusterClient}'s existing
 * connect paths and assembles the facade.
 * <p>
 * The detected {@link TopologyMode} is shared with the owning client through {@code modeCache}, so only the first connect
 * probes (same timing as {@link RedisClusterClient} loading partitions on first connect).
 */
class UniversalConnectionBuilder<K, V> {

    private final RedisClusterClient client;

    private final RedisCodec<K, V> codec;

    private final AtomicReference<TopologyMode> modeCache;

    private final AtomicBoolean clusterInitialized;

    UniversalConnectionBuilder(RedisClusterClient client, RedisCodec<K, V> codec, AtomicReference<TopologyMode> modeCache,
            AtomicBoolean clusterInitialized) {
        this.client = client;
        this.codec = codec;
        this.modeCache = modeCache;
        this.clusterInitialized = clusterInitialized;
    }

    CompletableFuture<StatefulRedisUniversalConnection<K, V>> connectAsync() {

        TopologyMode known = modeCache.get();
        if (known != null) {
            return connectKnown(known);
        }

        // First connect: open the cheap connection (plain, non-routed, first seed) and read what the server says it is.
        // Everything below may run on the event loop, so nothing blocks.
        return connectToFirstSeed().thenCompose(probe -> detectAsync(probe).handle((mode, error) -> {
            if (error != null) {
                return probe.closeAsync().<StatefulRedisUniversalConnection<K, V>> thenCompose(v -> failed(error));
            }
            modeCache.compareAndSet(null, mode);
            if (mode == TopologyMode.STANDALONE) {
                return CompletableFuture.completedFuture(wrap(probe, mode)); // the probe already is the connection
            }
            return probe.closeAsync().thenCompose(v -> connectKnown(mode));
        }).thenCompose(f -> f));
    }

    private CompletableFuture<StatefulRedisUniversalConnection<K, V>> connectKnown(TopologyMode mode) {
        if (mode == TopologyMode.CLUSTER) {
            // RedisClusterClient.connectAsync requires partitions to be loaded first (the blocking connect does this
            // itself). Same protected hook the cluster client uses; runs once per client.
            CompletableFuture<?> ready = clusterInitialized.compareAndSet(false, true) ? client.initializePartitions()
                    : CompletableFuture.completedFuture(null);
            return ready.thenCompose(v -> client.connectAsync(codec)).thenApply(c -> wrap(c, mode));
        }
        return connectToFirstSeed().thenApply(c -> wrap(c, mode));
    }

    private CompletableFuture<StatefulRedisConnectionImpl<K, V>> connectToFirstSeed() {
        RedisURI seed = client.getFirstUri();
        Mono<SocketAddress> address = Mono.fromCallable(() -> client.getResources().socketAddressResolver().resolve(seed));
        // connectToNodeAsync always builds a StatefulRedisConnectionImpl
        return client.connectToNodeAsync(codec, seed.toString(), null, address).toCompletableFuture()
                .thenApply(c -> (StatefulRedisConnectionImpl<K, V>) c);
    }

    /**
     * RESP3 (the default): the handshake has already parsed {@code mode} from the {@code HELLO} reply, nothing to send. RESP2
     * (configured, or negotiated down by an old server): {@code INFO server} / {@code redis_mode}, asynchronously. Two outcomes
     * only: cluster, or not.
     */
    private CompletableFuture<TopologyMode> detectAsync(StatefulRedisConnectionImpl<K, V> probe) {

        ConnectionState state = probe.getConnectionState();

        if (state.getNegotiatedProtocolVersion() == ProtocolVersion.RESP3) {
            return CompletableFuture.completedFuture(toMode(state.getMode()));
        }

        return probe.commands(RedisReactiveCommands.<K, V> factory()).info("server").toFuture()
                .thenApply(info -> toMode(parseRedisMode(info)));
    }

    private static TopologyMode toMode(String redisMode) {
        return "cluster".equalsIgnoreCase(redisMode) ? TopologyMode.CLUSTER : TopologyMode.STANDALONE;
    }

    static String parseRedisMode(String info) {
        if (info == null) {
            return null;
        }
        for (String line : info.split("\r?\n")) {
            if (line.startsWith("redis_mode:")) {
                return line.substring("redis_mode:".length()).trim();
            }
        }
        return null;
    }

    private StatefulRedisUniversalConnection<K, V> wrap(StatefulConnection<K, V> delegate, TopologyMode mode) {
        return new StatefulRedisUniversalConnectionImpl<>(delegate, mode);
    }

    private static <T> CompletableFuture<T> failed(Throwable t) {
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(t);
        return f;
    }

}
