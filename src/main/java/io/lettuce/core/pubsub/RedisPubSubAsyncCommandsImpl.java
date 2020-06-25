/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.pubsub;

import java.util.List;
import java.util.Map;

import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * An asynchronous and thread-safe API for a Redis pub/sub connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class RedisPubSubAsyncCommandsImpl<K, V> extends RedisAsyncCommandsImpl<K, V> implements RedisPubSubAsyncCommands<K, V> {

    private final PubSubCommandBuilder<K, V> commandBuilder;

    /**
     * Initialize a new connection.
     *
     * @param connection the connection .
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisPubSubAsyncCommandsImpl(StatefulRedisPubSubConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.commandBuilder = new PubSubCommandBuilder<>(codec);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> psubscribe(K... patterns) {
        return (RedisFuture<Void>) dispatch(commandBuilder.psubscribe(patterns));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> punsubscribe(K... patterns) {
        return (RedisFuture<Void>) dispatch(commandBuilder.punsubscribe(patterns));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> subscribe(K... channels) {
        return (RedisFuture<Void>) dispatch(commandBuilder.subscribe(channels));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> unsubscribe(K... channels) {
        return (RedisFuture<Void>) dispatch(commandBuilder.unsubscribe(channels));
    }

    @Override
    public RedisFuture<Long> publish(K channel, V message) {
        return dispatch(commandBuilder.publish(channel, message));
    }

    @Override
    public RedisFuture<List<K>> pubsubChannels(K channel) {
        return dispatch(commandBuilder.pubsubChannels(channel));
    }

    @Override
    public RedisFuture<Map<K, Long>> pubsubNumsub(K... channels) {
        return dispatch(commandBuilder.pubsubNumsub(channels));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StatefulRedisPubSubConnection<K, V> getStatefulConnection() {
        return (StatefulRedisPubSubConnection<K, V>) super.getStatefulConnection();
    }

}
