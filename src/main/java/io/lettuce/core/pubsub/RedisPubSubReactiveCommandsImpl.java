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
package io.lettuce.core.pubsub;

import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.api.reactive.ChannelMessage;
import io.lettuce.core.pubsub.api.reactive.PatternMessage;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;

/**
 * A reactive and thread-safe API for a Redis pub/sub connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class RedisPubSubReactiveCommandsImpl<K, V> extends RedisReactiveCommandsImpl<K, V> implements
        RedisPubSubReactiveCommands<K, V> {

    private final PubSubCommandBuilder<K, V> commandBuilder;

    /**
     * Initialize a new connection.
     *
     * @param connection the connection .
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisPubSubReactiveCommandsImpl(StatefulRedisPubSubConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.commandBuilder = new PubSubCommandBuilder<>(codec);
    }

    @Override
    public Flux<PatternMessage<K, V>> observePatterns() {
        return observePatterns(FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public Flux<PatternMessage<K, V>> observePatterns(FluxSink.OverflowStrategy overflowStrategy) {

        return Flux.create(sink -> {

            RedisPubSubAdapter<K, V> listener = new RedisPubSubAdapter<K, V>() {

                @Override
                public void message(K pattern, K channel, V message) {
                    sink.next(new PatternMessage<>(pattern, channel, message));
                }
            };

            StatefulRedisPubSubConnection<K, V> statefulConnection = getStatefulConnection();
            statefulConnection.addListener(listener);

            sink.onDispose(() -> {
                statefulConnection.removeListener(listener);
            });

        }, overflowStrategy);
    }

    @Override
    public Flux<ChannelMessage<K, V>> observeChannels() {
        return observeChannels(FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public Flux<ChannelMessage<K, V>> observeChannels(FluxSink.OverflowStrategy overflowStrategy) {

        return Flux.create(sink -> {

            RedisPubSubAdapter<K, V> listener = new RedisPubSubAdapter<K, V>() {

                @Override
                public void message(K channel, V message) {
                    sink.next(new ChannelMessage<>(channel, message));
                }
            };

            StatefulRedisPubSubConnection<K, V> statefulConnection = getStatefulConnection();
            statefulConnection.addListener(listener);

            sink.onDispose(() -> {
                statefulConnection.removeListener(listener);
            });

        }, overflowStrategy);
    }

    @Override
    public Mono<Void> psubscribe(K... patterns) {
        return createMono(() -> commandBuilder.psubscribe(patterns)).then();
    }

    @Override
    public Mono<Void> punsubscribe(K... patterns) {
        return createFlux(() -> commandBuilder.punsubscribe(patterns)).then();
    }

    @Override
    public Mono<Void> subscribe(K... channels) {
        return createFlux(() -> commandBuilder.subscribe(channels)).then();
    }

    @Override
    public Mono<Void> unsubscribe(K... channels) {
        return createFlux(() -> commandBuilder.unsubscribe(channels)).then();
    }

    @Override
    public Mono<Long> publish(K channel, V message) {
        return createMono(() -> commandBuilder.publish(channel, message));
    }

    @Override
    public Flux<K> pubsubChannels(K channel) {
        return createDissolvingFlux(() -> commandBuilder.pubsubChannels(channel));
    }

    @Override
    public Mono<Map<K, Long>> pubsubNumsub(K... channels) {
        return createMono(() -> commandBuilder.pubsubNumsub(channels));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StatefulRedisPubSubConnection<K, V> getStatefulConnection() {
        return (StatefulRedisPubSubConnection<K, V>) super.getStatefulConnection();
    }

}
