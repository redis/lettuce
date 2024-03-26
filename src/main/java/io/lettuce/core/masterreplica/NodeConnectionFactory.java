package io.lettuce.core.masterreplica;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

/**
 * Factory interface to obtain {@link StatefulRedisConnection connections} to Redis nodes.
 *
 * @author Mark Paluch
 * @since 4.4
 */
interface NodeConnectionFactory {

    /**
     * Connects to a {@link SocketAddress} with the given {@link RedisCodec} asynchronously.
     *
     * @param codec must not be {@code null}.
     * @param redisURI must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new {@link StatefulRedisConnection}
     */
    <K, V> CompletableFuture<StatefulRedisConnection<K, V>> connectToNodeAsync(RedisCodec<K, V> codec, RedisURI redisURI);

}
