package io.lettuce.core.cluster.pubsub.api.async;

import java.util.function.Predicate;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * Asynchronous and thread-safe Redis Cluster PubSub API. Operations are executed either on the main connection or a
 * {@link PubSubAsyncNodeSelection}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.4
 */
public interface RedisClusterPubSubAsyncCommands<K, V> extends RedisPubSubAsyncCommands<K, V> {

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterPubSubConnection<K, V> getStatefulConnection();

    /**
     * Select all upstream nodes.
     *
     * @return API with asynchronous executed commands on a selection of upstream cluster nodes.
     * @deprecated since 6.0 in favor of {@link #upstream()} (use {@link #primaries()} for primary terminology).
     */
    @Deprecated
    default PubSubAsyncNodeSelection<K, V> masters() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.UPSTREAM));
    }

    /**
     * Select all upstream nodes.
     *
     * @return API with asynchronous executed commands on a selection of upstream cluster nodes.
     */
    default PubSubAsyncNodeSelection<K, V> upstream() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.UPSTREAM));
    }

    /**
     * Select all primary nodes.
     *
     * @return API with asynchronous executed commands on a selection of primary cluster nodes.
     * @since 7.3
     */
    default PubSubAsyncNodeSelection<K, V> primaries() {
        return upstream();
    }

    /**
     * Select all replicas.
     *
     * @return API with asynchronous executed commands on a selection of replica cluster nodes.
     * @deprecated since 5.2, use {@link #replicas()}
     */
    @Deprecated
    default PubSubAsyncNodeSelection<K, V> slaves() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA));
    }

    /**
     * Select all replicas.
     *
     * @param predicate Predicate to filter nodes
     * @return API with asynchronous executed commands on a selection of replica cluster nodes.
     * @deprecated since 5.2, use {@link #replicas(Predicate)}
     */
    @Deprecated
    default PubSubAsyncNodeSelection<K, V> slaves(Predicate<RedisClusterNode> predicate) {
        return nodes(
                redisClusterNode -> predicate.test(redisClusterNode) && redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA));
    }

    /**
     * Select all replicas.
     *
     * @return API with asynchronous executed commands on a selection of replica cluster nodes.
     * @since 5.2
     */
    @Deprecated
    default PubSubAsyncNodeSelection<K, V> replicas() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA));
    }

    /**
     * Select all replicas.
     *
     * @param predicate Predicate to filter nodes
     * @return API with asynchronous executed commands on a selection of replica cluster nodes.
     * @since 5.2
     */
    default PubSubAsyncNodeSelection<K, V> replicas(Predicate<RedisClusterNode> predicate) {
        return nodes(
                redisClusterNode -> predicate.test(redisClusterNode) && redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA));
    }

    /**
     * Select all known cluster nodes.
     *
     * @return API with asynchronous executed commands on a selection of all cluster nodes.
     */
    default PubSubAsyncNodeSelection<K, V> all() {
        return nodes(redisClusterNode -> true);
    }

    /**
     * Select nodes by a predicate.
     *
     * @param predicate Predicate to filter nodes
     * @return API with asynchronous executed commands on a selection of cluster nodes matching {@code predicate}
     */
    PubSubAsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate);

}
