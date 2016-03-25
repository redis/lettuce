package com.lambdaworks.redis.pubsub.api.sync;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;

/**
 * 
 * Synchronous and thread-safe Redis PubSub API.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface RedisPubSubCommands<K, V> extends RedisCommands<K, V> {

    /**
     * Add a new listener.
     * 
     * @param listener Listener.
     */
    void addListener(RedisPubSubListener<K, V> listener);

    /**
     * Remove an existing listener.
     * 
     * @param listener Listener.
     */
    void removeListener(RedisPubSubListener<K, V> listener);

    /**
     * Listen for messages published to channels matching the given patterns.
     * 
     * @param patterns the patterns
     */
    void psubscribe(K... patterns);

    /**
     * Stop listening for messages posted to channels matching the given patterns.
     * 
     * @param patterns the patterns
     */
    void punsubscribe(K... patterns);

    /**
     * Listen for messages published to the given channels.
     * 
     * @param channels the channels
     */
    void subscribe(K... channels);

    /**
     * Stop listening for messages posted to the given channels.
     * 
     * @param channels the channels
     */
    void unsubscribe(K... channels);

    /**
     * @return the underlying connection.
     */
    StatefulRedisPubSubConnection<K, V> getStatefulConnection();
}
