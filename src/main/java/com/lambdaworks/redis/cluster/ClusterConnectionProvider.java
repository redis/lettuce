package com.lambdaworks.redis.cluster;

import java.io.Closeable;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.api.StatefulRedisConnection;

/**
 * Connection provider for cluster operations.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
interface ClusterConnectionProvider extends Closeable {
    /**
     * Provide a connection for the intent and cluster slot.
     * 
     * @param intent {@link com.lambdaworks.redis.cluster.ClusterConnectionProvider.Intent#READ} or
     *        {@link com.lambdaworks.redis.cluster.ClusterConnectionProvider.Intent#WRITE} {@literal READ} connections will be
     *        provided in {@literal READONLY} mode
     * @param slot hash slot
     * @return a valid connection which handles the slot.
     * @throws RedisException if no know node can be found for the slot
     */
    <K, V> StatefulRedisConnection<K, V> getConnection(Intent intent, int slot);

    /**
     * Provide a connection for the intent and host/port.
     * 
     * @param intent {@link com.lambdaworks.redis.cluster.ClusterConnectionProvider.Intent#READ} or
     *        {@link com.lambdaworks.redis.cluster.ClusterConnectionProvider.Intent#WRITE} {@literal READ} connections will be
     *        provided in {@literal READONLY} mode
     * @param host host of the node
     * @param port port of the node
     * @return a valid connection to the given host.
     */
    <K, V> StatefulRedisConnection<K, V> getConnection(Intent intent, String host, int port);

    /**
     * Close the connections and free all resources.
     */
    @Override
    void close();

    /**
     * Reset the writer state. Queued commands will be canceled and the internal state will be reset. This is useful when the
     * internal state machine gets out of sync with the connection.
     */
    void reset();

    public static enum Intent {
        READ, WRITE;
    }
}
