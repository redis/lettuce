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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisException;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.ConcurrentSet;

/**
 * @author Mark Paluch
 */
public class PubSubEndpoint<K, V> extends DefaultEndpoint {

    private static final Set<String> ALLOWED_COMMANDS_SUBSCRIBED;
    private final List<RedisPubSubListener<K, V>> listeners = new CopyOnWriteArrayList<>();
    private final Set<K> channels;
    private final Set<K> patterns;

    static {

        ALLOWED_COMMANDS_SUBSCRIBED = new HashSet<>(5, 1);

        ALLOWED_COMMANDS_SUBSCRIBED.add(CommandType.SUBSCRIBE.name());
        ALLOWED_COMMANDS_SUBSCRIBED.add(CommandType.PSUBSCRIBE.name());
        ALLOWED_COMMANDS_SUBSCRIBED.add(CommandType.UNSUBSCRIBE.name());
        ALLOWED_COMMANDS_SUBSCRIBED.add(CommandType.PUNSUBSCRIBE.name());
        ALLOWED_COMMANDS_SUBSCRIBED.add(CommandType.QUIT.name());
    }

    /**
     * Initialize a new instance that handles commands from the supplied queue.
     *
     * @param clientOptions client options for this connection, must not be {@literal null}
     * @param clientResources client resources for this connection, must not be {@literal null}.
     */
    public PubSubEndpoint(ClientOptions clientOptions, ClientResources clientResources) {

        super(clientOptions, clientResources);

        this.channels = new ConcurrentSet<>();
        this.patterns = new ConcurrentSet<>();
    }

    /**
     * Add a new {@link RedisPubSubListener listener}.
     *
     * @param listener the listener, must not be {@literal null}.
     */
    public void addListener(RedisPubSubListener<K, V> listener) {
        listeners.add(listener);
    }

    /**
     * Remove an existing {@link RedisPubSubListener listener}..
     *
     * @param listener the listener, must not be {@literal null}.
     */
    public void removeListener(RedisPubSubListener<K, V> listener) {
        listeners.remove(listener);
    }

    protected List<RedisPubSubListener<K, V>> getListeners() {
        return listeners;
    }

    public Set<K> getChannels() {
        return channels;
    }

    public Set<K> getPatterns() {
        return patterns;
    }

    @Override
    public <K1, V1, T> RedisCommand<K1, V1, T> write(RedisCommand<K1, V1, T> command) {

        if (!channels.isEmpty() || !patterns.isEmpty()) {

            if (!ALLOWED_COMMANDS_SUBSCRIBED.contains(command.getType().name())) {

                throw new RedisException(String.format("Command %s not allowed while subscribed. Allowed commands are: %s",
                        command.getType().name(), ALLOWED_COMMANDS_SUBSCRIBED));
            }
        }

        return super.write(command);
    }

    public void notifyMessage(PubSubOutput<K, V, V> output) {

        // drop empty messages
        if (output.type() == null || (output.pattern() == null && output.channel() == null && output.get() == null)) {
            return;
        }

        updateInternalState(output);
        notifyListeners(output);
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
                patterns.add(output.pattern());
                break;
            case punsubscribe:
                patterns.remove(output.pattern());
                break;
            case subscribe:
                channels.add(output.channel());
                break;
            case unsubscribe:
                channels.remove(output.channel());
                break;
            default:
                break;
        }
    }
}
