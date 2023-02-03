/*
 * Copyright 2011-2022 the original author or authors.
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author Mark Paluch
 * @author dengliming
 */
public class PubSubEndpoint<K, V> extends DefaultEndpoint {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PubSubEndpoint.class);

    private final List<RedisPubSubListener<K, V>> listeners = new CopyOnWriteArrayList<>();

    private final Set<Wrapper<K>> channels;

    private final Set<Wrapper<K>> patterns;

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientOptions client options for this connection, must not be {@code null}
     * @param clientResources client resources for this connection, must not be {@code null}.
     */
    public PubSubEndpoint(ClientOptions clientOptions, ClientResources clientResources) {

        super(clientOptions, clientResources);

        this.channels = ConcurrentHashMap.newKeySet();
        this.patterns = ConcurrentHashMap.newKeySet();
    }

    /**
     * Add a new {@link RedisPubSubListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    public void addListener(RedisPubSubListener<K, V> listener) {
        listeners.add(listener);
    }

    /**
     * Remove an existing {@link RedisPubSubListener listener}.
     *
     * @param listener the listener, must not be {@code null}.
     */
    public void removeListener(RedisPubSubListener<K, V> listener) {
        listeners.remove(listener);
    }

    protected List<RedisPubSubListener<K, V>> getListeners() {
        return listeners;
    }

    public boolean hasChannelSubscriptions() {
        return !channels.isEmpty();
    }

    public Set<K> getChannels() {
        return unwrap(this.channels);
    }

    public boolean hasPatternSubscriptions() {
        return !patterns.isEmpty();
    }

    public Set<K> getPatterns() {
        return unwrap(this.patterns);
    }

    void notifyMessage(PubSubMessage<K, V> message) {
        // drop empty messages
        if (message.type() == null || (message.pattern() == null && message.channel() == null && message.body() == null)) {
            return;
        }

        updateInternalState(message);
        try {
            notifyListeners(message);
        } catch (Exception e) {
            logger.error("Unexpected error occurred in RedisPubSubListener callback", e);
        }
    }

    protected void notifyListeners(PubSubMessage<K, V> message) {
        // update listeners
        for (RedisPubSubListener<K, V> listener : listeners) {
            switch (message.type()) {
                case message:
                    listener.message(message.channel(), message.body());
                    break;
                case pmessage:
                    listener.message(message.pattern(), message.channel(), message.body());
                    break;
                case psubscribe:
                    listener.psubscribed(message.pattern(), message.count());
                    break;
                case punsubscribe:
                    listener.punsubscribed(message.pattern(), message.count());
                    break;
                case subscribe:
                    listener.subscribed(message.channel(), message.count());
                    break;
                case unsubscribe:
                    listener.unsubscribed(message.channel(), message.count());
                    break;
                default:
                    throw new UnsupportedOperationException("Operation " + message.type() + " not supported");
            }
        }
    }

    private void updateInternalState(PubSubMessage<K, V> message) {
        // update internal state
        switch (message.type()) {
            case psubscribe:
                patterns.add(new Wrapper<>(message.pattern()));
                break;
            case punsubscribe:
                patterns.remove(new Wrapper<>(message.pattern()));
                break;
            case subscribe:
                channels.add(new Wrapper<>(message.channel()));
                break;
            case unsubscribe:
                channels.remove(new Wrapper<>(message.channel()));
                break;
            default:
                break;
        }
    }

    private Set<K> unwrap(Set<Wrapper<K>> wrapped) {

        Set<K> result = new LinkedHashSet<>(wrapped.size());

        for (Wrapper<K> channel : wrapped) {
            result.add(channel.name);
        }

        return result;
    }

    /**
     * Comparison/equality wrapper with specific {@code byte[]} equals and hashCode implementations.
     *
     * @param <K>
     */
    static class Wrapper<K> {

        protected final K name;

        public Wrapper(K name) {
            this.name = name;
        }

        @Override
        public int hashCode() {

            if (name instanceof byte[]) {
                return Arrays.hashCode((byte[]) name);
            }
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {

            if (!(obj instanceof Wrapper)) {
                return false;
            }

            Wrapper<K> that = (Wrapper<K>) obj;

            if (name instanceof byte[] && that.name instanceof byte[]) {
                return Arrays.equals((byte[]) name, (byte[]) that.name);
            }

            return name.equals(that.name);
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer();
            sb.append(getClass().getSimpleName());
            sb.append(" [name=").append(name);
            sb.append(']');
            return sb.toString();
        }

    }

}
