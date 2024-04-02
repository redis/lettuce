package io.lettuce.core.cluster.pubsub;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Interface for Redis Cluster Pub/Sub listeners.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Ali Takavci
 * @since 4.4
 */
public interface RedisClusterShardedPubSubListener<K, V> extends RedisClusterPubSubListener<K, V>{

    /**
     * Subscribed to a shard channel.
     *
     * @param node the {@link RedisClusterNode} from which the {@code message} originates.
     * @param shardChannel Shard channel
     * @param count Subscription count.
     */
    void ssubscribed(RedisClusterNode node, K shardChannel, long count);

}
