package com.lambdaworks.redis.cluster.topology;

import java.net.SocketAddress;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Factory interface to obtain {@link StatefulRedisConnection connections} to Redis cluster nodes.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public interface NodeConnectionFactory {

    /**
     * Connects to a {@link SocketAddress} with the given {@link RedisCodec}.
     *
     * @param codec must not be {@literal null}.
     * @param socketAddress must not be {@literal null}.
     * @param <K>
     * @param <V>
     * @return a new {@link StatefulRedisConnection}
     */
    <K, V> StatefulRedisConnection<K, V> connectToNode(RedisCodec<K, V> codec, SocketAddress socketAddress);
}
