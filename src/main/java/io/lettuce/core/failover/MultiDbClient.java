package io.lettuce.core.failover;

import java.util.Collection;

import io.lettuce.core.ConnectionFuture;
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

    /**
     * Get the configured Redis URIs.
     *
     * @return the configured Redis URIs
     */
    Collection<RedisURI> getRedisURIs();

    /**
     * Open a new multi database connection to a Redis server. Use the supplied {@link RedisCodec codec} to encode/decode keys
     * and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful Redis connection
     */
    <K, V> StatefulRedisMultiDbConnection<K, V> connect(RedisCodec<K, V> codec);

    /**
     * Open a new multi database connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful Redis connection
     */
    StatefulRedisMultiDbConnection<String, String> connect();

    /**
     * Open a new multi database pub/sub connection to a Redis server. Use the supplied {@link RedisCodec codec} to
     * encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new stateful pub/sub connection
     */
    <K, V> StatefulRedisMultiDbPubSubConnection<K, V> connectPubSub(RedisCodec<K, V> codec);

    /**
     * Open a new multi database pub/sub connection to a Redis server that treats keys and values as UTF-8 strings.
     *
     * @return A new stateful pub/sub connection
     */
    StatefulRedisMultiDbPubSubConnection<String, String> connectPubSub();

    /**
     * Open asynchronously a new multi database connection to a Redis server. Use the supplied {@link RedisCodec codec} to
     * encode/decode keys and values.
     *
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}
     * @param <K> Key type
     * @param <V> Value type
     * @return a {@link CompletableFuture} that is notified with the connection progress.
     * @since 7.4
     */
    public <K, V> ConnectionFuture<StatefulRedisMultiDbConnection<K, V>> connectAsync(RedisCodec<K, V> codec);

}
