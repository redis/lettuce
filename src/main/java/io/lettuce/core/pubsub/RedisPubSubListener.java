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

/**
 * Interface for redis pub/sub listeners.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public interface RedisPubSubListener<K, V> {
    /**
     * Message received from a channel subscription.
     *
     * @param channel Channel.
     * @param message Message.
     */
    void message(K channel, V message);

    /**
     * Message received from a pattern subscription.
     *
     * @param pattern Pattern
     * @param channel Channel
     * @param message Message
     */
    void message(K pattern, K channel, V message);

    /**
     * Subscribed to a channel.
     *
     * @param channel Channel
     * @param count Subscription count.
     */
    void subscribed(K channel, long count);

    /**
     * Subscribed to a pattern.
     *
     * @param pattern Pattern.
     * @param count Subscription count.
     */
    void psubscribed(K pattern, long count);

    /**
     * Unsubscribed from a channel.
     *
     * @param channel Channel
     * @param count Subscription count.
     */
    void unsubscribed(K channel, long count);

    /**
     * Unsubscribed from a pattern.
     *
     * @param pattern Channel
     * @param count Subscription count.
     */
    void punsubscribed(K pattern, long count);
}
