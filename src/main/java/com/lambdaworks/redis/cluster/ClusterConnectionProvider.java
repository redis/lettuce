package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisAsyncConnectionImpl;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:41
 */
interface ClusterConnectionProvider {
    <K, V> RedisAsyncConnectionImpl<K, V> getConnection(int hash, Intent intent);

    public static enum Intent {
        READ, WRITE;
    }
}
