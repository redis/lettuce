package io.lettuce.core.pubsub;

/**
 * Interface for Redis Pub/Sub listeners.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Ali Takavci
 */
public interface RedisShardedPubSubListener<K, V> extends RedisPubSubListener<K, V>{

    /**
     * Subscribed to a Shard channel.
     *
     * @param shardChannel Shard channel
     * @param count Subscription count.
     */
    void ssubscribed(K chashardChannelnnel, long count);

}
