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
package io.lettuce.core.masterreplica;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.masterslave.StatefulRedisMasterSlaveConnection;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * Connection wrapper for {@link StatefulRedisMasterSlaveConnection}.
 *
 * @author Mark Paluch
 * @since 5.2
 */
class MasterReplicaConnectionWrapper<K, V> implements StatefulRedisMasterReplicaConnection<K, V> {

    private final StatefulRedisMasterSlaveConnection<K, V> delegate;

    public MasterReplicaConnectionWrapper(StatefulRedisMasterSlaveConnection<K, V> delegate) {
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
    public void setTimeout(Duration timeout) {
        delegate.setTimeout(timeout);
    }

    @Override
    @Deprecated
    public void setTimeout(long timeout, TimeUnit unit) {
        delegate.setTimeout(timeout, unit);
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
    @Deprecated
    public void reset() {
        delegate.reset();
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
