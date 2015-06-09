package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.BaseRedisAsyncCommands;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.cluster.api.StatefulClusterConnection;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Advanced asynchronous Cluster connection.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 */
public class RedisAdvancedClusterAsyncConnectionImpl<K, V> extends BaseRedisAsyncCommands<K, V> implements
        RedisAdvancedClusterAsyncConnection<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisAdvancedClusterAsyncConnectionImpl(StatefulClusterConnectionImpl<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    public StatefulClusterConnection<K, V> getStatefulConnection() {
        return (StatefulClusterConnection<K, V>) connection;
    }

    @Override
    public RedisClusterAsyncConnection<K, V> getConnection(String nodeId) {
        return getStatefulConnection().getConnection(nodeId).async();
    }

    @Override
    public RedisClusterAsyncConnection<K, V> getConnection(String host, int port) {
        return getStatefulConnection().getConnection(host, port).async();
    }

}
