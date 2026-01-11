package io.lettuce.core.primaryreplica;

import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.masterreplica.MasterReplica;

/**
 * Primary-Replica connection API.
 * <p>
 * This API allows connections to Redis Primary/Replica setups which run either in a static Primary/Replica setup or are managed
 * by Redis Sentinel. Primary-Replica connections can discover topologies and select a source for read operations using
 * {@link io.lettuce.core.ReadFrom}.
 * </p>
 *
 * @author Mark Paluch
 * @since 7.3
 */
@SuppressWarnings("deprecation")
public class PrimaryReplica {

    /**
     * Open a new connection to a Redis Primary-Replica server/servers using the supplied {@link RedisURI} and the supplied
     * {@link RedisCodec codec} to encode/decode keys.
     * <p>
     * This {@link PrimaryReplica} performs auto-discovery of nodes using either Redis Sentinel or Primary/Replica. A
     * {@link RedisURI} can point to either a primary or a replica host.
     * </p>
     *
     * @param redisClient the Redis client.
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection.
     */
    public static <K, V> StatefulRedisPrimaryReplicaConnection<K, V> connect(RedisClient redisClient, RedisCodec<K, V> codec,
            RedisURI redisURI) {
        return MasterReplica.connect(redisClient, codec, redisURI);
    }

    /**
     * Open asynchronously a new connection to a Redis Primary-Replica server/servers using the supplied {@link RedisURI} and
     * the supplied {@link RedisCodec codec} to encode/decode keys.
     * <p>
     * This {@link PrimaryReplica} performs auto-discovery of nodes using either Redis Sentinel or Primary/Replica. A
     * {@link RedisURI} can point to either a primary or a replica host.
     * </p>
     *
     * @param redisClient the Redis client.
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURI the Redis server to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return {@link CompletableFuture} that is notified once the connect is finished.
     */
    public static <K, V> CompletableFuture<StatefulRedisPrimaryReplicaConnection<K, V>> connectAsync(RedisClient redisClient,
            RedisCodec<K, V> codec, RedisURI redisURI) {
        return MasterReplica.connectAsync(redisClient, codec, redisURI)
                .thenApply(connection -> (StatefulRedisPrimaryReplicaConnection<K, V>) connection);
    }

    /**
     * Open a new connection to a Redis Primary-Replica server/servers using the supplied {@link RedisURI} and the supplied
     * {@link RedisCodec codec} to encode/decode keys.
     * <p>
     * This {@link PrimaryReplica} performs auto-discovery of nodes if the URI is a Redis Sentinel URI. Primary/Replica URIs
     * will be treated as static topology and no additional hosts are discovered in such case. Redis Standalone Primary/Replica
     * will discover the roles of the supplied {@link RedisURI URIs} and issue commands to the appropriate node.
     * </p>
     *
     * @param redisClient the Redis client.
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURIs the Redis server(s) to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return a new connection.
     */
    public static <K, V> StatefulRedisPrimaryReplicaConnection<K, V> connect(RedisClient redisClient, RedisCodec<K, V> codec,
            Iterable<RedisURI> redisURIs) {
        return MasterReplica.connect(redisClient, codec, redisURIs);
    }

    /**
     * Open asynchronously a new connection to a Redis Primary-Replica server/servers using the supplied {@link RedisURI} and
     * the supplied {@link RedisCodec codec} to encode/decode keys.
     * <p>
     * This {@link PrimaryReplica} performs auto-discovery of nodes if the URI is a Redis Sentinel URI. Primary/Replica URIs
     * will be treated as static topology and no additional hosts are discovered in such case. Redis Standalone Primary/Replica
     * will discover the roles of the supplied {@link RedisURI URIs} and issue commands to the appropriate node.
     * </p>
     *
     * @param redisClient the Redis client.
     * @param codec Use this codec to encode/decode keys and values, must not be {@code null}.
     * @param redisURIs the Redis server(s) to connect to, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return {@link CompletableFuture} that is notified once the connect is finished.
     */
    public static <K, V> CompletableFuture<StatefulRedisPrimaryReplicaConnection<K, V>> connectAsync(RedisClient redisClient,
            RedisCodec<K, V> codec, Iterable<RedisURI> redisURIs) {
        return MasterReplica.connectAsync(redisClient, codec, redisURIs)
                .thenApply(connection -> (StatefulRedisPrimaryReplicaConnection<K, V>) connection);
    }

}
