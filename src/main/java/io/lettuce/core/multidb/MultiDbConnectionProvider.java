package io.lettuce.core.multidb;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.AsyncConnectionProvider;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Connection provider for MultiDb setups. The connection provider manages connections to multiple Redis endpoints and handles
 * stale connection cleanup when endpoints are removed.
 *
 * @author Mark Paluch
 * @author Ivo Gaydazhiev
 * @since 4.1
 */
class MultiDbConnectionProvider<K, V> implements Closeable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultiDbConnectionProvider.class);

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

    /**
     * @return number of connections.
     */
    protected long getConnectionCount() {
        return connectionProvider.getConnectionCount();
    }

    /**
     * Retrieve a set of ConnectionKey's for all pooled connections that are within the pool but not within the available
     * endpoints.
     *
     * @return Set of {@link ConnectionKey}s
     */
    private Set<ConnectionKey> getStaleConnectionKeys() {

        Map<ConnectionKey, StatefulRedisConnection<K, V>> map = new ConcurrentHashMap<>();
        connectionProvider.forEach(map::put);

        Set<ConnectionKey> stale = new HashSet<>();
        Set<RedisURI> availableEndpoints = endpoints.getEndpoints();

        for (ConnectionKey connectionKey : map.keySet()) {
            if (!availableEndpoints.contains(connectionKey.endpoint)) {
                stale.add(connectionKey);
            }
        }
        return stale;
    }

    /**
     * Close stale connections that are no longer in the available endpoints.
     */
    public void closeStaleConnections() {

        logger.debug("closeStaleConnections() count before expiring: {}", getConnectionCount());

        Set<ConnectionKey> stale = getStaleConnectionKeys();

        for (ConnectionKey connectionKey : stale) {
            connectionProvider.close(connectionKey);
        }

        logger.debug("closeStaleConnections() count after expiring: {}", getConnectionCount());
    }

    /**
     * Notify the connection provider that endpoints have been updated. This will trigger cleanup of stale connections for
     * endpoints that are no longer available.
     */
    public void setEndpoints() {
        stateLock.lock();
        try {
            closeStaleConnections();
        } finally {
            stateLock.unlock();
        }
    }

    public RedisURI getActive() {
        return endpoints.getActive();
    }

    /**
     * Get the available endpoints.
     *
     * @return the Redis endpoints
     */
    public RedisEndpoints getEndpoints() {
        return endpoints;
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
