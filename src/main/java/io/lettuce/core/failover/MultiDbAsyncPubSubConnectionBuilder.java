package io.lettuce.core.failover;

import java.util.Map;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;

/**
 * Builder for creating PubSub multi-database Redis connections.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author Lettuce Contributors
 */
class MultiDbAsyncPubSubConnectionBuilder<K, V> extends
        AbstractRedisMultiDbConnectionBuilder<StatefulRedisMultiDbPubSubConnection<K, V>, StatefulRedisPubSubConnection<K, V>, K, V> {

    /**
     * Creates a new PubSub multi-database connection builder.
     *
     * @param client the multi-database client instance
     * @param resources the client resources for event loops and thread pools
     * @param codec the codec for encoding/decoding keys and values
     * @param multiDbOptions the multi-database configuration
     */
    MultiDbAsyncPubSubConnectionBuilder(MultiDbClientImpl client, ClientResources resources, RedisCodec<K, V> codec,
            MultiDbOptions multiDbOptions) {
        super(client, resources, codec, multiDbOptions);
    }

    @Override
    protected ConnectionFuture<StatefulRedisPubSubConnection<K, V>> connectAsync(RedisCodec<K, V> codec, RedisURI uri) {
        return client.connectPubSubAsync(codec, uri);
    }

    @Override
    protected StatefulRedisMultiDbPubSubConnection<K, V> createMultiDbConnection(
            RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>> selected,
            Map<RedisURI, RedisDatabaseImpl<StatefulRedisPubSubConnection<K, V>>> databases, RedisCodec<K, V> codec,
            HealthStatusManager healthStatusManager,
            RedisDatabaseAsyncCompletion<StatefulRedisPubSubConnection<K, V>> completion, MultiDbOptions multiDbOptions) {

        return new StatefulRedisMultiDbPubSubConnectionImpl<>(selected, databases, resources, codec,
                this::createRedisDatabaseAsync, healthStatusManager, completion, multiDbOptions);
    }

}
