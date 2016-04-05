package com.lambdaworks.redis.pubsub.api.rx;

import com.lambdaworks.redis.api.rx.Success;
import rx.Observable;

import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;

/**
 * Asynchronous and thread-safe Redis PubSub API.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public interface RedisPubSubReactiveCommands<K, V> extends RedisReactiveCommands<K, V> {

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
     * Observable for messages ({@literal pmessage}) received though pattern subscriptions. The connection needs to be
     * subscribed to one or more patterns using {@link #psubscribe(Object[])}.
     * 
     * @return hot observable for subscriptions to {@literal pmessage}'s.
     */
    Observable<PatternMessage<K, V>> observePatterns();

    /**
     * Observable for messages ({@literal message}) received though channel subscriptions. The connection needs to be subscribed
     * to one or more channels using {@link #subscribe(Object[])}.
     * 
     * @return hot observable for subscriptions to {@literal message}'s.
     */
    Observable<ChannelMessage<K, V>> observeChannels();

    /**
     * Listen for messages published to channels matching the given patterns.
     * 
     * @param patterns the patterns
     * @return Observable&lt;Success&gt; Observable for {@code psubscribe} command
     */
    Observable<Success> psubscribe(K... patterns);

    /**
     * Stop listening for messages posted to channels matching the given patterns.
     * 
     * @param patterns the patterns
     * @return Observable&lt;Success&gt; Observable for {@code punsubscribe} command
     */
    Observable<Success> punsubscribe(K... patterns);

    /**
     * Listen for messages published to the given channels.
     * 
     * @param channels the channels
     * @return Observable&lt;Success&gt; Observable for {@code subscribe} command
     */
    Observable<Success> subscribe(K... channels);

    /**
     * Stop listening for messages posted to the given channels.
     * 
     * @param channels the channels
     * @return Observable&lt;Success&gt; Observable for {@code unsubscribe} command.
     */
    Observable<Success> unsubscribe(K... channels);

    /**
     * @return the underlying connection.
     */
    StatefulRedisPubSubConnection<K, V> getStatefulConnection();
}
