/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.pubsub.api.rx;

import com.lambdaworks.redis.api.rx.Success;
import rx.Observable;

import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.pubsub.RedisPubSubListener;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;

/**
 * Reactive and thread-safe Redis PubSub API.
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
     * @deprecated Use {@link #getStatefulConnection()} and
     *             {@link StatefulRedisPubSubConnection#addListener(RedisPubSubListener)}.
     */
    @Deprecated
    void addListener(RedisPubSubListener<K, V> listener);

    /**
     * Remove an existing listener.
     * 
     * @param listener Listener.
     * @deprecated Use {@link #getStatefulConnection()} and
     *             {@link StatefulRedisPubSubConnection#removeListener(RedisPubSubListener)}.
     */
    @Deprecated
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
