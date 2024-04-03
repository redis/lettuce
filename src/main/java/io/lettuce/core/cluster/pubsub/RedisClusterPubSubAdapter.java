package io.lettuce.core.cluster.pubsub;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Convenience adapter with an empty implementation of all {@link RedisClusterPubSubListener} callback methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.4
 */
public class RedisClusterPubSubAdapter<K, V> implements RedisClusterPubSubListener<K, V> {

    @Override
    public void message(RedisClusterNode node, K channel, V message) {
        // empty adapter method
    }

    @Override
    public void message(RedisClusterNode node, K pattern, K channel, V message) {
        // empty adapter method
    }

    @Override
    public void subscribed(RedisClusterNode node, K channel, long count) {
        // empty adapter method
    }

    @Override
    public void psubscribed(RedisClusterNode node, K pattern, long count) {
        // empty adapter method
    }

    @Override
    public void unsubscribed(RedisClusterNode node, K channel, long count) {
        // empty adapter method
    }

    @Override
    public void punsubscribed(RedisClusterNode node, K pattern, long count) {
        // empty adapter method
    }

    @Override
    public void ssubscribed(RedisClusterNode node, K channel, long count) {
        // empty adapter method
    }

}
