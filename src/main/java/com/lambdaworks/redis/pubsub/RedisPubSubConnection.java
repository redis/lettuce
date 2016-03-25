package com.lambdaworks.redis.pubsub;

import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 * @deprecated Use {@link RedisPubSubAsyncCommands}
 */
@Deprecated
public interface RedisPubSubConnection<K, V> extends RedisAsyncConnection<K, V> {

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
     * @return RedisFuture&lt;Void&gt; Future to synchronize {@code psubscribe} completion
     */
    RedisFuture<Void> psubscribe(K... patterns);

    /**
     * Stop listening for messages posted to channels matching the given patterns.
     * 
     * @param patterns the patterns
     * @return RedisFuture&lt;Void&gt; Future to synchronize {@code punsubscribe} completion
     */
    RedisFuture<Void> punsubscribe(K... patterns);

    /**
     * Listen for messages published to the given channels.
     * 
     * @param channels the channels
     * @return RedisFuture&lt;Void&gt; Future to synchronize {@code subscribe} completion
     */
    RedisFuture<Void> subscribe(K... channels);

    /**
     * Stop listening for messages posted to the given channels.
     * 
     * @param channels the channels
     * @return RedisFuture&lt;Void&gt; Future to synchronize {@code unsubscribe} completion.
     */
    RedisFuture<Void> unsubscribe(K... channels);
}
