package io.lettuce.core.failover;

import java.util.Collection;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisURI;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.api.BaseRedisClient;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.api.StatefulRedisMultiDbPubSubConnection;
import io.lettuce.core.resource.ClientResources;

/**
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
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

    public static MultiDbClient create(ClientOptions options, ClientResources resources,
            Collection<DatabaseConfig> databaseConfigs) {
        MultiDbClientImpl client = new MultiDbClientImpl(resources, databaseConfigs);
        client.setOptions(options);
        return client;
    }

    /**
     * Set the {@link ClientOptions} for the client.
     *
     * @param clientOptions the new client options
     * @throws IllegalArgumentException if {@param clientOptions} is null
     */
    public void setOptions(ClientOptions clientOptions);

    /**
     * Get the configured Redis URIs.
     *
     * @return the configured Redis URIs
     */
    Collection<RedisURI> getRedisURIs();

    /**
     * Open a new connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis connection
     */
    <K, V> StatefulRedisMultiDbConnection<K, V> connect(RedisCodec<K, V> codec);

    /**
     * Open a new connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    StatefulRedisMultiDbConnection<String, String> connect();

    /**
     * Open a new pub/sub connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys and
     * values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful pub/sub connection
     */
    <K, V> StatefulRedisMultiDbPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec);

    /**
     * Open a new pub/sub connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful pub/sub connection
     */
    StatefulRedisMultiDbPubSubConnection<String, String> connectPubSub();

}
