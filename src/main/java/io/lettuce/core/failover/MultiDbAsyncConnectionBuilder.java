package io.lettuce.core.failover;

import java.util.Map;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.resource.ClientResources;

/**
 * Builder for creating regular (non-PubSub) multi-database Redis connections.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Lettuce Contributors
 */
class MultiDbAsyncConnectionBuilder<K, V> extends
        AbstractRedisMultiDbConnectionBuilder<StatefulRedisMultiDbConnection<K, V>, StatefulRedisConnection<K, V>, K, V> {

    /**
     * Creates a new regular multi-database connection builder.
     *
     * @param client the multi-database client instance
     * @param resources the client resources for event loops and thread pools
     * @param codec the codec for encoding/decoding keys and values
     */
    MultiDbAsyncConnectionBuilder(MultiDbClientImpl client, ClientResources resources, RedisCodec<K, V> codec) {
        super(client, resources, codec);
    }

    @Override
    protected ConnectionFuture<StatefulRedisConnection<K, V>> connectAsync(RedisCodec<K, V> codec, RedisURI uri) {
        return client.connectAsync(codec, uri);
    }

    @Override
    protected StatefulRedisMultiDbConnection<K, V> createMultiDbConnection(
            RedisDatabaseImpl<StatefulRedisConnection<K, V>> selected,
            Map<RedisURI, RedisDatabaseImpl<StatefulRedisConnection<K, V>>> databases, RedisCodec<K, V> codec,
            HealthStatusManager healthStatusManager, RedisDatabaseAsyncCompletion<StatefulRedisConnection<K, V>> completion) {

        return new StatefulRedisMultiDbConnectionImpl<>(selected, databases, resources, codec, this::createRedisDatabaseAsync,
                healthStatusManager, completion);
    }

}
