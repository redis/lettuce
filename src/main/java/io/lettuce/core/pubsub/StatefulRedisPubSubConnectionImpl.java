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

import java.lang.reflect.Array;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.StatefulRedisConnectionImpl;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.ConnectionWatchdog;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * An thread-safe pub/sub connection to a Redis server. Multiple threads may share one {@link StatefulRedisPubSubConnectionImpl}
 *
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
public class StatefulRedisPubSubConnectionImpl<K, V> extends StatefulRedisConnectionImpl<K, V>
        implements StatefulRedisPubSubConnection<K, V> {

    private final PubSubEndpoint<K, V> endpoint;

    /**
     * Initialize a new connection.
     *
     * @param endpoint the {@link PubSubEndpoint}
     * @param writer the writer used to write commands
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     */
    public StatefulRedisPubSubConnectionImpl(PubSubEndpoint<K, V> endpoint, RedisChannelWriter writer, RedisCodec<K, V> codec,
            Duration timeout) {

        super(writer, codec, timeout);

        this.endpoint = endpoint;
    }

    /**
     * Add a new listener.
     *
     * @param listener Listener.
     */
    @Override
    public void addListener(RedisPubSubListener<K, V> listener) {
        endpoint.addListener(listener);
    }

    /**
     * Remove an existing listener.
     *
     * @param listener Listener.
     */
    @Override
    public void removeListener(RedisPubSubListener<K, V> listener) {
        endpoint.removeListener(listener);
    }

    @Override
    public RedisPubSubAsyncCommands<K, V> async() {
        return (RedisPubSubAsyncCommands<K, V>) async;
    }

    @Override
    protected RedisPubSubAsyncCommandsImpl<K, V> newRedisAsyncCommandsImpl() {
        return new RedisPubSubAsyncCommandsImpl<>(this, codec);
    }

    @Override
    public RedisPubSubCommands<K, V> sync() {
        return (RedisPubSubCommands<K, V>) sync;
    }

    @Override
    protected RedisPubSubCommands<K, V> newRedisSyncCommandsImpl() {
        return syncHandler(async(), RedisPubSubCommands.class);
    }

    @Override
    public RedisPubSubReactiveCommands<K, V> reactive() {
        return (RedisPubSubReactiveCommands<K, V>) reactive;
    }

    @Override
    protected RedisPubSubReactiveCommandsImpl<K, V> newRedisReactiveCommandsImpl() {
        return new RedisPubSubReactiveCommandsImpl<>(this, codec);
    }

    /**
     * Re-subscribe to all previously subscribed channels and patterns.
     *
     * @return list of the futures of the {@literal subscribe} and {@literal psubscribe} commands.
     */
    protected List<RedisFuture<Void>> resubscribe() {

        List<RedisFuture<Void>> result = new ArrayList<>();

        if (endpoint.hasChannelSubscriptions()) {
            result.add(async().subscribe(toArray(endpoint.getChannels())));
        }

        if (endpoint.hasPatternSubscriptions()) {
            result.add(async().psubscribe(toArray(endpoint.getPatterns())));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private <T> T[] toArray(Collection<T> c) {
        Class<T> cls = (Class<T>) c.iterator().next().getClass();
        T[] array = (T[]) Array.newInstance(cls, c.size());
        return c.toArray(array);
    }

    @Override
    public void activated() {
        super.activated();
        for (RedisFuture<Void> command : resubscribe()) {
            command.exceptionally(throwable -> {
                if (throwable instanceof RedisCommandExecutionException) {
                    InternalLoggerFactory.getInstance(getClass()).warn("Re-subscribe failed: " + command.getError());
                }
                return null;
            });
        }
    }

}
