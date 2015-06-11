package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.AbstractRedisAsyncCommands;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Advanced asynchronous Cluster connection.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 */
public class RedisAdvancedClusterAsyncConnectionImpl<K, V> extends AbstractRedisAsyncCommands<K, V> implements
        RedisAdvancedClusterAsyncConnection<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param connection the stateful connection
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisAdvancedClusterAsyncConnectionImpl(StatefulRedisClusterConnectionImpl<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    public StatefulRedisClusterConnection<K, V> getStatefulConnection() {
        return (StatefulRedisClusterConnection<K, V>) connection;
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
