package io.lettuce.core.multidb;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.AsyncConnectionProvider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Connection provider for master/replica setups. The connection provider
 *
 * @author Mark Paluch
 * @since 4.1
 */
// TODO: ggivo How to handle stale connections after removing endpoints?
class MultiDbConnectionProvider<K, V> {

    // connection map, contains connections per available endpoint
    private final AsyncConnectionProvider<ConnectionKey, StatefulRedisConnection<K, V>, CompletionStage<StatefulRedisConnection<K, V>>> connectionProvider;

    private boolean autoFlushCommands = true;

    private final Lock stateLock = new ReentrantLock();

    private final RedisEndpoints endpoints;

    MultiDbConnectionProvider(MultiDbClient redisClient, RedisCodec<K, V> redisCodec, RedisEndpoints endpoints,
            RedisChannelWriter multiDbWriter) {

        this.endpoints = endpoints;

        Function<ConnectionKey, CompletionStage<StatefulRedisConnection<K, V>>> connectionFactory = new DefaultConnectionFactory(
                redisClient, redisCodec, multiDbWriter);

        this.connectionProvider = new AsyncConnectionProvider<>(connectionFactory);
    }

    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync() {

        return getConnection(getActive());
    }

    protected CompletableFuture<StatefulRedisConnection<K, V>> getConnection(RedisURI endpoint) {

        return connectionProvider.getConnection(toConnectionKey(endpoint)).toCompletableFuture();
    }

    /**
     * Close all connections.
     */
    public void close() {
        closeAsync().join();
    }

    /**
     * Close all connections asynchronously.
     *
     * @since 5.1
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> closeAsync() {
        return connectionProvider.close();
    }

    /**
     * Flush pending commands on all connections.
     *
     * @see StatefulConnection#flushCommands()
     */
    public void flushCommands() {
        connectionProvider.forEach(StatefulConnection::flushCommands);
    }

    /**
     * Disable or enable auto-flush behavior for all connections.
     *
     * @param autoFlush state of autoFlush.
     * @see StatefulConnection#setAutoFlushCommands(boolean)
     */
    public void setAutoFlushCommands(boolean autoFlush) {

        stateLock.lock();
        try {
            this.autoFlushCommands = autoFlush;
            connectionProvider.forEach(connection -> connection.setAutoFlushCommands(autoFlush));
        } finally {
            stateLock.unlock();
        }
    }

    public RedisURI getActive() {
        return endpoints.getActive();
    }

    class DefaultConnectionFactory implements Function<ConnectionKey, CompletionStage<StatefulRedisConnection<K, V>>> {

        private final MultiDbClient redisClient;

        private final RedisCodec<K, V> redisCodec;

        private final RedisChannelWriter multiDbWriter;

        DefaultConnectionFactory(MultiDbClient redisClient, RedisCodec<K, V> redisCodec, RedisChannelWriter multiDbWriter) {
            this.redisClient = redisClient;
            this.redisCodec = redisCodec;
            this.multiDbWriter = multiDbWriter;
        }

        @Override
        public ConnectionFuture<StatefulRedisConnection<K, V>> apply(ConnectionKey key) {

            RedisURI endpointUri = RedisURI.builder(key.endpoint).build();

            ConnectionFuture<StatefulRedisConnection<K, V>> connectionFuture = redisClient
                    .connectToEndpointNodeAsync(redisCodec, endpointUri, multiDbWriter);

            connectionFuture.thenAccept(connection -> {
                stateLock.lock();
                try {
                    connection.setAutoFlushCommands(autoFlushCommands);
                } finally {
                    stateLock.unlock();
                }
            });

            return connectionFuture;
        }

    }

    private static ConnectionKey toConnectionKey(RedisURI redisURI) {
        return new ConnectionKey(redisURI);
    }

    /**
     * Connection to identify a connection by host/port.
     */
    static class ConnectionKey {

        private final RedisURI endpoint;

        ConnectionKey(RedisURI endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ConnectionKey))
                return false;

            ConnectionKey that = (ConnectionKey) o;

            return endpoint.equals(that.endpoint);

        }

        @Override
        public int hashCode() {
            return endpoint.hashCode();
        }

    }

}
