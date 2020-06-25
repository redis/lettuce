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
 */
public class PubSubEndpoint<K, V> extends DefaultEndpoint {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PubSubEndpoint.class);

    private static final Set<String> ALLOWED_COMMANDS_SUBSCRIBED;

    private static final Set<String> SUBSCRIBE_COMMANDS;

    private final List<RedisPubSubListener<K, V>> listeners = new CopyOnWriteArrayList<>();

    private final Set<Wrapper<K>> channels;

    private final Set<Wrapper<K>> patterns;

    private volatile boolean subscribeWritten = false;

    static {

        ALLOWED_COMMANDS_SUBSCRIBED = new HashSet<>(5, 1);

        ALLOWED_COMMANDS_SUBSCRIBED.add(CommandType.SUBSCRIBE.name());
        ALLOWED_COMMANDS_SUBSCRIBED.add(CommandType.PSUBSCRIBE.name());
        ALLOWED_COMMANDS_SUBSCRIBED.add(CommandType.UNSUBSCRIBE.name());
        ALLOWED_COMMANDS_SUBSCRIBED.add(CommandType.PUNSUBSCRIBE.name());
        ALLOWED_COMMANDS_SUBSCRIBED.add(CommandType.QUIT.name());

        SUBSCRIBE_COMMANDS = new HashSet<>(2, 1);

        SUBSCRIBE_COMMANDS.add(CommandType.SUBSCRIBE.name());
        SUBSCRIBE_COMMANDS.add(CommandType.PSUBSCRIBE.name());
    }

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
     * Remove an existing {@link RedisPubSubListener listener}..
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

    @Override
    public void notifyChannelActive(Channel channel) {
        subscribeWritten = false;
        super.notifyChannelActive(channel);
    }

    @Override
    public <K1, V1, T> RedisCommand<K1, V1, T> write(RedisCommand<K1, V1, T> command) {

        if (isSubscribed()) {
            validateCommandAllowed(command);
        }

        if (!subscribeWritten && SUBSCRIBE_COMMANDS.contains(command.getType().name())) {
            subscribeWritten = true;
        }

        return super.write(command);
    }

    @Override
    public <K1, V1> Collection<RedisCommand<K1, V1, ?>> write(Collection<? extends RedisCommand<K1, V1, ?>> redisCommands) {

        if (isSubscribed()) {
            redisCommands.forEach(PubSubEndpoint::validateCommandAllowed);
        }

        if (!subscribeWritten) {
            for (RedisCommand<K1, V1, ?> redisCommand : redisCommands) {
                if (SUBSCRIBE_COMMANDS.contains(redisCommand.getType().name())) {
                    subscribeWritten = true;
                    break;
                }
            }
        }

        return super.write(redisCommands);
    }

    private static void validateCommandAllowed(RedisCommand<?, ?, ?> command) {

        if (!ALLOWED_COMMANDS_SUBSCRIBED.contains(command.getType().name())) {

            throw new RedisException(String.format("Command %s not allowed while subscribed. Allowed commands are: %s",
                    command.getType().name(), ALLOWED_COMMANDS_SUBSCRIBED));
        }
    }

    private boolean isSubscribed() {
        return subscribeWritten && (hasChannelSubscriptions() || hasPatternSubscriptions());
    }

    public void notifyMessage(PubSubOutput<K, V, V> output) {

        // drop empty messages
        if (output.type() == null || (output.pattern() == null && output.channel() == null && output.get() == null)) {
            return;
        }

        updateInternalState(output);
        try {
            notifyListeners(output);
        } catch (Exception e) {
            logger.error("Unexpected error occurred in RedisPubSubListener callback", e);
        }
    }

    protected void notifyListeners(PubSubOutput<K, V, V> output) {
        // update listeners
        for (RedisPubSubListener<K, V> listener : listeners) {
            switch (output.type()) {
                case message:
                    listener.message(output.channel(), output.get());
                    break;
                case pmessage:
                    listener.message(output.pattern(), output.channel(), output.get());
                    break;
                case psubscribe:
                    listener.psubscribed(output.pattern(), output.count());
                    break;
                case punsubscribe:
                    listener.punsubscribed(output.pattern(), output.count());
                    break;
                case subscribe:
                    listener.subscribed(output.channel(), output.count());
                    break;
                case unsubscribe:
                    listener.unsubscribed(output.channel(), output.count());
                    break;
                default:
                    throw new UnsupportedOperationException("Operation " + output.type() + " not supported");
            }
        }
    }

    private void updateInternalState(PubSubOutput<K, V, V> output) {
        // update internal state
        switch (output.type()) {
            case psubscribe:
                patterns.add(new Wrapper<>(output.pattern()));
                break;
            case punsubscribe:
                patterns.remove(new Wrapper<>(output.pattern()));
                break;
            case subscribe:
                channels.add(new Wrapper<>(output.channel()));
                break;
            case unsubscribe:
                channels.remove(new Wrapper<>(output.channel()));
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
