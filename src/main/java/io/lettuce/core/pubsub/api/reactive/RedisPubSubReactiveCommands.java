/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.pubsub.api.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * Asynchronous and thread-safe Redis PubSub API.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.0
 */
public interface RedisPubSubReactiveCommands<K, V> extends RedisReactiveCommands<K, V> {

    /**
     * Flux for messages ({@literal pmessage}) received though pattern subscriptions. The connection needs to be subscribed to
     * one or more patterns using {@link #psubscribe(Object[])}.
     * <p>
     * Warning! This method uses {@link reactor.core.publisher.FluxSink.OverflowStrategy#BUFFER} This does unbounded buffering
     * and may lead to {@link OutOfMemoryError}. Use {@link #observePatterns(FluxSink.OverflowStrategy)} to specify a different
     * strategy.
     * </p>
     *
     * @return hot Flux for subscriptions to {@literal pmessage}'s.
     */
    Flux<PatternMessage<K, V>> observePatterns();

    /**
     * Flux for messages ({@literal pmessage}) received though pattern subscriptions. The connection needs to be subscribed to
     * one or more patterns using {@link #psubscribe(Object[])}.
     *
     * @param overflowStrategy the overflow strategy to use.
     * @return hot Flux for subscriptions to {@literal pmessage}'s.
     */
    Flux<PatternMessage<K, V>> observePatterns(FluxSink.OverflowStrategy overflowStrategy);

    /**
     * Flux for messages ({@literal message}) received though channel subscriptions. The connection needs to be subscribed to
     * one or more channels using {@link #subscribe(Object[])}.
     *
     * <p>
     * Warning! This method uses {@link reactor.core.publisher.FluxSink.OverflowStrategy#BUFFER} This does unbounded buffering
     * and may lead to {@link OutOfMemoryError}. Use {@link #observeChannels(FluxSink.OverflowStrategy)} to specify a different
     * strategy.
     * </p>
     *
     * @return hot Flux for subscriptions to {@literal message}'s.
     */
    Flux<ChannelMessage<K, V>> observeChannels();

    /**
     * Flux for messages ({@literal message}) received though channel subscriptions. The connection needs to be subscribed to
     * one or more channels using {@link #subscribe(Object[])}.
     *
     * @param overflowStrategy the overflow strategy to use.
     * @return hot Flux for subscriptions to {@literal message}'s.
     */
    Flux<ChannelMessage<K, V>> observeChannels(FluxSink.OverflowStrategy overflowStrategy);

    /**
     * Listen for messages published to channels matching the given patterns.
     *
     * @param patterns the patterns
     * @return Flux&lt;Success&gt; Flux for {@code psubscribe} command
     */
    Mono<Void> psubscribe(K... patterns);

    /**
     * Stop listening for messages posted to channels matching the given patterns.
     *
     * @param patterns the patterns
     * @return Flux&lt;Success&gt; Flux for {@code punsubscribe} command
     */
    Mono<Void> punsubscribe(K... patterns);

    /**
     * Listen for messages published to the given channels.
     *
     * @param channels the channels
     * @return Flux&lt;Success&gt; Flux for {@code subscribe} command
     */
    Mono<Void> subscribe(K... channels);

    /**
     * Stop listening for messages posted to the given channels.
     *
     * @param channels the channels
     * @return Flux&lt;Success&gt; Flux for {@code unsubscribe} command.
     */
    Mono<Void> unsubscribe(K... channels);

    /**
     * @return the underlying connection.
     */
    StatefulRedisPubSubConnection<K, V> getStatefulConnection();
}
