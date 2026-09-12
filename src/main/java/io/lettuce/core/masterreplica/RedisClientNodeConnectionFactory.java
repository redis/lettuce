package io.lettuce.core.masterreplica;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.ClientOptions;
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

    private final ClientOptions clientOptions;

    RedisClientNodeConnectionFactory(RedisClient client, ClientOptions clientOptions) {
        this.client = client;
        this.clientOptions = clientOptions;
    }

    @Override
    public <K, V> CompletableFuture<StatefulRedisConnection<K, V>> connectToNodeAsync(RedisCodec<K, V> codec,
            RedisURI redisURI) {
        return client.connectAsync(codec, redisURI, clientOptions).toCompletableFuture();
    }

}
