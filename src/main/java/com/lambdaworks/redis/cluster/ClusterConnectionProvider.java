package com.lambdaworks.redis.cluster;

import java.io.Closeable;

import com.lambdaworks.redis.RedisAsyncConnectionImpl;

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
     * @param intent
     * @param slot
     * @return a valid connection which handles the slot.
     */
    <K, V> RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, int slot);

    /**
     * Provide a connection for the intent and host/port.
     * 
     * @param intent
     * @param host
     * @param port
     * @return a valid connection to the given host.
     */
    <K, V> RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, String host, int port);

    /**
     * Close the connections and free all resources.
     */
    @Override
    void close();

    public static enum Intent {
        READ, WRITE;
    }
}
