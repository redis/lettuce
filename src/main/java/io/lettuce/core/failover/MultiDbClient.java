package io.lettuce.core.failover;

import java.util.Collection;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.BaseRedisClient;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.resource.ClientResources;

/**
 * @author Ali Takavci
 * @since 7.1
 */
public interface MultiDbClient extends BaseRedisClient {

    public static MultiDbClient create(Collection<DatabaseConfig> databaseConfigs) {
        if (databaseConfigs == null || databaseConfigs.isEmpty()) {
            throw new IllegalArgumentException("Database configs must not be empty");
        }
        return new MultiDbClientImpl(databaseConfigs);
    }

    public static MultiDbClient create(ClientResources resources, Collection<DatabaseConfig> databaseConfigs) {
        if (resources == null) {
            throw new IllegalArgumentException("Client resources must not be null");
        }
        if (databaseConfigs == null || databaseConfigs.isEmpty()) {
            throw new IllegalArgumentException("Database configs must not be empty");
        }
        return new MultiDbClientImpl(resources, databaseConfigs);
    }

    Collection<RedisURI> getRedisURIs();

    <K, V> StatefulRedisMultiDbConnection<K, V> connect(RedisCodec<K, V> codec);

    StatefulRedisMultiDbConnection<String, String> connect();

    <K, V> StatefulRedisMultiDbPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec);

    StatefulRedisMultiDbPubSubConnection<String, String> connectPubSub();

}
