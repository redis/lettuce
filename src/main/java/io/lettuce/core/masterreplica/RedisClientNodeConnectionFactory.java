package io.lettuce.core.masterreplica;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;

/**
 * {@link NodeConnectionFactory} implementation that on {@link RedisClient}.
 *
 * @author Mark Paluch
 */
class RedisClientNodeConnectionFactory implements NodeConnectionFactory {

    private final RedisClient client;

    RedisClientNodeConnectionFactory(RedisClient client) {
        this.client = client;
    }

    @Override
    public <K, V> CompletableFuture<StatefulRedisConnection<K, V>> connectToNodeAsync(RedisCodec<K, V> codec,
            RedisURI redisURI) {
        return client.connectAsync(codec, redisURI).toCompletableFuture();
    }

}
