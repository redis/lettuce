package io.lettuce.core.cluster.pubsub;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Interface for Redis Cluster Pub/Sub listeners.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.4
 */
public interface RedisClusterPubSubListener<K, V> {

    /**
     * Message received from a channel subscription.
     *
     * @param node the {@link RedisClusterNode} from which the {@code message} originates.
     * @param channel Channel.
     * @param message Message.
     */
    void message(RedisClusterNode node, K channel, V message);

    /**
     * Message received from a pattern subscription.
     *
     * @param node the {@link RedisClusterNode} from which the {@code message} originates.
     * @param pattern Pattern
     * @param channel Channel
     * @param message Message
     */
    void message(RedisClusterNode node, K pattern, K channel, V message);

    /**
     * Subscribed to a channel.
     *
     * @param node the {@link RedisClusterNode} from which the {@code message} originates.
     * @param channel Channel
     * @param count Subscription count.
     */
    void subscribed(RedisClusterNode node, K channel, long count);

    /**
     * Subscribed to a pattern.
     *
     * @param node the {@link RedisClusterNode} from which the {@code message} originates.
     * @param pattern Pattern.
     * @param count Subscription count.
     */
    void psubscribed(RedisClusterNode node, K pattern, long count);

    /**
     * Unsubscribed from a channel.
     *
     * @param node the {@link RedisClusterNode} from which the {@code message} originates.
     * @param channel Channel
     * @param count Subscription count.
     */
    void unsubscribed(RedisClusterNode node, K channel, long count);

    /**
     * Unsubscribed from a pattern.
     *
     * @param node the {@link RedisClusterNode} from which the {@code message} originates.
     * @param pattern Channel
     * @param count Subscription count.
     */
    void punsubscribed(RedisClusterNode node, K pattern, long count);

    /**
     * Subscribed to a shard channel.
     *
     * @param node the {@link RedisClusterNode} from which the {@code message} originates.
     * @param shardChannel Shard channel
     * @param count Subscription count.
     * @since 7.0
     */
    default void ssubscribed(RedisClusterNode node, K shardChannel, long count) {
        subscribed(node, shardChannel, count);
    }

}
