package io.lettuce.core.cluster.topology;

import java.net.SocketAddress;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

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
     * @param codec must not be {@code null}.
     * @param socketAddress must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link StatefulRedisConnection}
     */
    <K, V> StatefulRedisConnection<K, V> connectToNode(RedisCodec<K, V> codec, SocketAddress socketAddress);

    /**
     * Connects to a {@link SocketAddress} with the given {@link RedisCodec} asynchronously.
     *
     * @param codec must not be {@code null}.
     * @param socketAddress must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link StatefulRedisConnection}
     * @since 4.4
     */
    <K, V> ConnectionFuture<StatefulRedisConnection<K, V>> connectToNodeAsync(RedisCodec<K, V> codec,
            SocketAddress socketAddress);

}
