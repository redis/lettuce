package io.lettuce.core.masterslave;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * Connection wrapper for {@link StatefulRedisMasterSlaveConnection}.
 *
 * @author Mark Paluch
 * @since 5.2
 */
class MasterSlaveConnectionWrapper<K, V> implements StatefulRedisMasterSlaveConnection<K, V> {

    private final StatefulRedisMasterReplicaConnection<K, V> delegate;

    public MasterSlaveConnectionWrapper(StatefulRedisMasterReplicaConnection<K, V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setReadFrom(ReadFrom readFrom) {
        delegate.setReadFrom(readFrom);
    }

    @Override
    public ReadFrom getReadFrom() {
        return delegate.getReadFrom();
    }

    @Override
    public boolean isMulti() {
        return delegate.isMulti();
    }

    @Override
    public RedisCommands<K, V> sync() {
        return delegate.sync();
    }

    @Override
    public RedisAsyncCommands<K, V> async() {
        return delegate.async();
    }

    @Override
    public RedisReactiveCommands<K, V> reactive() {
        return delegate.reactive();
    }

    @Override
    public void addListener(RedisConnectionStateListener listener) {
        this.delegate.addListener(listener);
    }

    @Override
    public void removeListener(RedisConnectionStateListener listener) {
        this.delegate.removeListener(listener);
    }

    @Override
    public void addListener(PushListener listener) {
        delegate.addListener(listener);
    }

    @Override
    public void removeListener(PushListener listener) {
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
    public Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> redisCommands) {
        return delegate.dispatch(redisCommands);
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

}
