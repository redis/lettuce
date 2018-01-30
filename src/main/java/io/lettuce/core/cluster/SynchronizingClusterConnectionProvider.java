/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterNodeConnectionFactory.ConnectionKey;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Synchronizing provider for Redis Cluster node connections using {@link ClusterNodeConnectionFactory}.
 * <p>
 * {@link #getConnection(ConnectionKey) Connection requests} are synchronized with a shared {@link Sync synchronzer object} per
 * {@link ConnectionKey}. Multiple threads requesting a connection for the same {@link ConnectionKey} share the same
 * synchronizer and are not required to wait until a previous asynchronous connection is established but participate in existing
 * connection initializations. Shared synchronization leads to a fair synchronization amongst multiple threads waiting to obtain
 * a connection.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class SynchronizingClusterConnectionProvider<K, V> {

    private final ClusterNodeConnectionFactory<K, V> connectionFactory;
    private final Map<ConnectionKey, Sync<K, V>> connections = new ConcurrentHashMap<>();

    private volatile boolean closed;

    /**
     * Create a new {@link SynchronizingClusterConnectionProvider}.
     *
     * @param connectionFactory must not be {@literal null}.
     */
    public SynchronizingClusterConnectionProvider(ClusterNodeConnectionFactory<K, V> connectionFactory) {

        LettuceAssert.notNull(connectionFactory, "AsyncClusterConnectionFactory must not be null");
        this.connectionFactory = connectionFactory;
    }

    /**
     * Obtain a {@link StatefulRedisConnection} to a cluster node given {@link ConnectionKey}.
     *
     * @param key the {@link ConnectionKey}.
     * @return
     * @throws RedisException if a {@link RedisException} occured
     * @throws CompletionException
     */
    public StatefulRedisConnection<K, V> getConnection(ConnectionKey key) {
        return getConnectionSync(key).getConnection();
    }

    /**
     * Obtain a {@link CompletableFuture}-wrapped connection to a cluster node given {@link ConnectionKey}.
     *
     * @param key the {@link ConnectionKey}.
     * @return
     * @throws RedisException if a {@link RedisException} occured
     * @throws CompletionException
     */
    public ConnectionFuture<StatefulRedisConnection<K, V>> getConnectionAsync(ConnectionKey key) {
        return getConnectionSync(key).getConnectionAsync();
    }

    /**
     * Obtain a {@link StatefulRedisConnection} to a cluster node given {@link ConnectionKey}.
     *
     * @param key the {@link ConnectionKey}.
     * @return
     * @throws RedisException if a {@link RedisException} occured
     * @throws CompletionException
     */
    private Sync<K, V> getConnectionSync(ConnectionKey key) {

        if (closed) {
            throw new IllegalStateException("AsyncClusterConnectionProvider is already closed");
        }

        Sync<K, V> sync = connections.computeIfAbsent(key, connectionKey -> {

            InProgress<K, V> createdSync = new InProgress<>(key, connectionFactory.apply(key), connections);

            if (closed) {
                createdSync.remove = InProgress.ST_FINISHED;
                createdSync.future.thenAcceptAsync(StatefulConnection::close);
            }

            return createdSync;
        });

        return sync;
    }

    /**
     * @return number of established connections.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int getConnectionCount() {

        Object[] syncs = connections.values().toArray(new Object[0]);
        int count = 0;

        for (Object sync : syncs) {
            if (sync instanceof Finished) {
                count++;
            }
        }

        return count;
    }

    /**
     * Close all connections. Pending connections are closed using future chaining.
     */
    public void close() {

        this.closed = true;
        forEach((connectionKey, connection) -> {
            connection.close();
            connections.remove(connectionKey);
        });
    }

    /**
     * Close a connection by its {@link ConnectionKey}. Pending connections are closed using future chaining.
     *
     * @param key must not be {@literal null}.
     */
    public void close(ConnectionKey key) {

        LettuceAssert.notNull(key, "ConnectionKey must not be null!");

        Sync<K, V> sync = connections.get(key);
        if (sync != null) {
            connections.remove(key);
            sync.doWithSync(StatefulConnection::close);
        }
    }

    /**
     * Execute an action for all established and pending {@link StatefulRedisConnection}s.
     *
     * @param action the action.
     */
    public void forEach(Consumer<? super StatefulRedisConnection<K, V>> action) {
        connections.values().forEach(sync -> sync.doWithSync(action));
    }

    /**
     * Execute an action for all established and pending {@link StatefulRedisConnection}s.
     *
     * @param action the action.
     */
    public void forEach(BiConsumer<ConnectionKey, ? super StatefulRedisConnection<K, V>> action) {
        connections.forEach((key, sync) -> sync.doWithSync(action));
    }

    interface Sync<K, V> {

        /**
         * Return the {@link StatefulRedisConnection}. May cause blocking if asynchronous connection is {@link InProgress}.
         *
         * @return
         */
        StatefulRedisConnection<K, V> getConnection();

        ConnectionFuture<StatefulRedisConnection<K, V>> getConnectionAsync();

        /**
         * Apply a {@link Consumer} callback to the {@link StatefulConnection}.
         *
         * @param action
         */
        void doWithSync(Consumer<? super StatefulRedisConnection<K, V>> action);

        /**
         * Apply a {@link BiConsumer} callback to the {@link ConnectionKey} and {@link StatefulConnection}.
         *
         * @param action
         */
        void doWithSync(BiConsumer<ConnectionKey, ? super StatefulRedisConnection<K, V>> action);
    }

    static class Finished<K, V> implements Sync<K, V> {

        private final ConnectionKey key;
        private final StatefulRedisConnection<K, V> connection;
        private final ConnectionFuture<StatefulRedisConnection<K, V>> future;

        public Finished(ConnectionKey key, SocketAddress remoteAddress, StatefulRedisConnection<K, V> connection) {
            this.key = key;
            this.connection = connection;
            this.future = ConnectionFuture.from(remoteAddress, CompletableFuture.completedFuture(connection));
        }

        @Override
        public StatefulRedisConnection<K, V> getConnection() {
            return connection;
        }

        @Override
        public ConnectionFuture<StatefulRedisConnection<K, V>> getConnectionAsync() {
            return future;
        }

        @Override
        public void doWithSync(Consumer<? super StatefulRedisConnection<K, V>> action) {
            action.accept(connection);
        }

        @Override
        public void doWithSync(BiConsumer<ConnectionKey, ? super StatefulRedisConnection<K, V>> action) {
            action.accept(key, connection);
        }
    }

    static class InProgress<K, V> implements Sync<K, V> {

        private static final int ST_IN_PROGRESS = 0;
        private static final int ST_FINISHED = 1;

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private final static AtomicIntegerFieldUpdater<InProgress> REMOVE = AtomicIntegerFieldUpdater.newUpdater(
                InProgress.class, "remove");

        // Updated with AtomicIntegerFieldUpdater
        @SuppressWarnings("unused")
        private volatile int remove = ST_IN_PROGRESS;

        private final ConnectionKey key;
        private final ConnectionFuture<StatefulRedisConnection<K, V>> future;
        private final Map<ConnectionKey, Sync<K, V>> connections;

        public InProgress(ConnectionKey key, ConnectionFuture<StatefulRedisConnection<K, V>> future,
                Map<ConnectionKey, Sync<K, V>> connections) {

            this.key = key;
            this.future = future;
            this.connections = connections;
        }

        @Override
        public ConnectionFuture<StatefulRedisConnection<K, V>> getConnectionAsync() {

            return future.whenComplete((connection, throwable) -> {

                if (REMOVE.compareAndSet(this, 0, ST_FINISHED)) {

                    if (throwable == null) {
                        connections.replace(key, this, new Finished<>(key, future.getRemoteAddress(), connection));
                    } else {
                        connections.remove(key);
                    }
                }
            });
        }

        public StatefulRedisConnection<K, V> getConnection() {

            try {
                return getConnectionAsync().toCompletableFuture().join();
            } catch (CompletionException e) {
                throw RedisConnectionException.create(future.getRemoteAddress(), e.getCause());
            }
        }

        @Override
        public void doWithSync(Consumer<? super StatefulRedisConnection<K, V>> action) {
            future.thenAccept(action);
        }

        @Override
        public void doWithSync(BiConsumer<ConnectionKey, ? super StatefulRedisConnection<K, V>> action) {
            future.thenAccept(connection -> action.accept(key, connection));
        }
    }
}
