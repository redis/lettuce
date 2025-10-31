/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
 * @author Ali Takavci
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
        super(connection, codec, null);
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
    public RedisFuture<List<K>> pubsubShardChannels(K pattern) {
        return dispatch(commandBuilder.pubsubShardChannels(pattern));
    }

    @Override
    public RedisFuture<Map<K, Long>> pubsubShardNumsub(K... shardChannels) {
        return dispatch(commandBuilder.pubsubShardNumsub(shardChannels));
    }

    @Override
    public RedisFuture<Long> spublish(K shardChannel, V message) {
        return dispatch(commandBuilder.spublish(shardChannel, message));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> ssubscribe(K... channels) {
        return (RedisFuture<Void>) dispatch(commandBuilder.ssubscribe(channels));
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<Void> sunsubscribe(K... channels) {
        // Mark these channels as intentionally unsubscribed to prevent auto-resubscription
        StatefulRedisPubSubConnection<K, V> connection = getStatefulConnection();
        if (connection instanceof StatefulRedisPubSubConnectionImpl) {
            StatefulRedisPubSubConnectionImpl<K, V> impl = (StatefulRedisPubSubConnectionImpl<K, V>) connection;
            for (K channel : channels) {
                impl.markIntentionalUnsubscribe(channel);
            }
        }
        return (RedisFuture<Void>) dispatch(commandBuilder.sunsubscribe(channels));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StatefulRedisPubSubConnection<K, V> getStatefulConnection() {
        return (StatefulRedisPubSubConnection<K, V>) super.getStatefulConnection();
    }

}
