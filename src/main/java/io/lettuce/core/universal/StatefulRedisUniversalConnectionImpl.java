package io.lettuce.core.universal;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.api.CommandsFactory;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * Facade over the real connection chosen by {@link UniversalClient}. Holds either a {@link StatefulRedisConnection} or a
 * {@link StatefulRedisClusterConnection} and forwards everything to it. Routing, redirects and topology refresh live in the
 * delegate, untouched.
 * <p>
 * Internal API.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class StatefulRedisUniversalConnectionImpl<K, V> implements StatefulRedisUniversalConnection<K, V> {

    private final StatefulConnection<K, V> delegate;

    private final TopologyMode mode;

    public StatefulRedisUniversalConnectionImpl(StatefulConnection<K, V> delegate, TopologyMode mode) {
        LettuceAssert.notNull(delegate, "Delegate connection must not be null");
        LettuceAssert.notNull(mode, "TopologyMode must not be null");
        if (mode == TopologyMode.CLUSTER) {
            LettuceAssert.isTrue(delegate instanceof StatefulRedisClusterConnection,
                    "CLUSTER mode requires a StatefulRedisClusterConnection delegate");
        } else {
            LettuceAssert.isTrue(delegate instanceof StatefulRedisConnection,
                    "STANDALONE mode requires a StatefulRedisConnection delegate");
        }
        this.delegate = delegate;
        this.mode = mode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T commands(CommandsFactory<? extends StatefulConnection<K, V>, T> factory) {

        LettuceAssert.notNull(factory, "CommandsFactory must not be null");

        try {
            if (mode == TopologyMode.CLUSTER) {
                return ((StatefulRedisClusterConnection<K, V>) delegate)
                        .commands((CommandsFactory<StatefulRedisClusterConnection<K, V>, T>) factory);
            }
            return ((StatefulRedisConnection<K, V>) delegate)
                    .commands((CommandsFactory<StatefulRedisConnection<K, V>, T>) factory);
        } catch (ClassCastException e) {
            // the existing factories' builders are typed on their connection; applying one to the other connection type
            // fails at the lambda boundary. Translate into something actionable.
            throw new IllegalStateException(String.format(
                    "Connected to a %s deployment but the supplied CommandsFactory (%s) targets a different connection type",
                    mode, factory.key()), e);
        }
    }

    @Override
    public TopologyMode getTopologyMode() {
        return mode;
    }

    /**
     * @return the underlying connection. Exposed for tests and diagnostics; prefer {@link #commands(CommandsFactory)}.
     */
    public StatefulConnection<K, V> getDelegate() {
        return delegate;
    }

    // ---- StatefulConnection: pure delegation ----

    @Override
    public void addListener(RedisConnectionStateListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(RedisConnectionStateListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public void setTimeout(Duration timeout) {
        delegate.setTimeout(timeout);
    }

    @Override
    public Duration getTimeout() {
        return delegate.getTimeout();
    }

    @Override
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command) {
        return delegate.dispatch(command);
    }

    @Override
    public Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> commands) {
        return delegate.dispatch(commands);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return delegate.closeAsync();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public ClientOptions getOptions() {
        return delegate.getOptions();
    }

    @Override
    public ClientResources getResources() {
        return delegate.getResources();
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        delegate.setAutoFlushCommands(autoFlush);
    }

    @Override
    public void flushCommands() {
        delegate.flushCommands();
    }

    @Override
    public RedisCodec<K, V> getCodec() {
        return delegate.getCodec();
    }

}
