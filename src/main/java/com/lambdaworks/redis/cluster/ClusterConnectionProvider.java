package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisAsyncConnectionImpl;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:41
 */
interface ClusterConnectionProvider {
    /**
     * Provide a connection for the intent and cluster slot.
     * 
     * @param intent
     * @param slot
     * @return
     */
    <K, V> RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, int slot);

    /**
     * Provide a connection for the intent and host/port.
     * 
     * @param intent
     * @param host
     * @param port
     * @return
     */
    <K, V> RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, String host, int port);

    public static enum Intent {
        READ, WRITE;
    }
}
