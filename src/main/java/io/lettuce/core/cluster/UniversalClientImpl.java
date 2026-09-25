package io.lettuce.core.cluster;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.universal.StatefulRedisUniversalConnection;
import io.lettuce.core.universal.TopologyMode;
import io.lettuce.core.universal.UniversalClient;

/**
 * {@link UniversalClient} implementation. Lives in this package to reach {@link RedisClusterClient}'s package-private
 * node-connection path for the detection probe and the standalone delegate.
 * <p>
 * Composes rather than extends {@link RedisClusterClient}: {@code RedisClusterClient.connect(codec)} returns
 * {@link StatefulRedisClusterConnection}, which a subclass cannot override to return {@link StatefulRedisUniversalConnection}
 * (unrelated return types). Composition also keeps the cluster API from leaking via downcast.
 * <p>
 * Internal API.
 */
public final class UniversalClientImpl implements UniversalClient {

    private final RedisClusterClient inner;

    /**
     * Detected once, on the first connect, and cached for the client's lifetime. Same timing as {@link RedisClusterClient},
     * which loads its partitions on first connect rather than at creation.
     */
    private final AtomicReference<TopologyMode> mode = new AtomicReference<>();

    private final AtomicBoolean clusterInitialized = new AtomicBoolean();

    private UniversalClientImpl(RedisClusterClient inner) {
        this.inner = inner;
    }

    public static UniversalClient create(ClientResources resources, Iterable<RedisURI> seeds) {
        LettuceAssert.notNull(seeds, "Seed URIs must not be null");
        RedisClusterClient inner = resources == null ? RedisClusterClient.create(seeds)
                : RedisClusterClient.create(resources, seeds);
        return new UniversalClientImpl(inner);
    }

    @Override
    public <K, V> StatefulRedisUniversalConnection<K, V> connect(RedisCodec<K, V> codec) {
        try {
            return connectAsync(codec).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw cause instanceof RedisException ? (RedisException) cause : RedisConnectionException.create(cause);
        }
    }

    @Override
    public <K, V> CompletableFuture<StatefulRedisUniversalConnection<K, V>> connectAsync(RedisCodec<K, V> codec) {
        LettuceAssert.notNull(codec, "RedisCodec must not be null");
        return new UniversalConnectionBuilder<>(inner, codec, mode, clusterInitialized).connectAsync();
    }

    /**
     * @return the detected topology, or {@code null} before the first connect.
     */
    public TopologyMode getTopologyMode() {
        return mode.get();
    }

    @Override
    public void setOptions(ClusterClientOptions options) {
        inner.setOptions(options);
    }

    @Override
    public ClusterClientOptions getOptions() {
        return inner.getClusterClientOptions();
    }

    // ---- BaseRedisClient: delegation ----

    @Override
    public ClientResources getResources() {
        return inner.getResources();
    }

    @Override
    public void addListener(RedisConnectionStateListener listener) {
        inner.addListener(listener);
    }

    @Override
    public void removeListener(RedisConnectionStateListener listener) {
        inner.removeListener(listener);
    }

    @Override
    public void addListener(CommandListener listener) {
        inner.addListener(listener);
    }

    @Override
    public void removeListener(CommandListener listener) {
        inner.removeListener(listener);
    }

    @Override
    public void shutdown() {
        inner.shutdown();
    }

    @Override
    public void shutdown(Duration quietPeriod, Duration timeout) {
        inner.shutdown(quietPeriod, timeout);
    }

    @Override
    public void shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
        inner.shutdown(quietPeriod, timeout, timeUnit);
    }

    @Override
    public CompletableFuture<Void> shutdownAsync() {
        return inner.shutdownAsync();
    }

    @Override
    public CompletableFuture<Void> shutdownAsync(long quietPeriod, long timeout, TimeUnit timeUnit) {
        return inner.shutdownAsync(quietPeriod, timeout, timeUnit);
    }

    @Override
    public void close() {
        shutdown();
    }

}
