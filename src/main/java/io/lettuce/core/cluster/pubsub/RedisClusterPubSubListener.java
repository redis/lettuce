/*
 * Copyright 2016-2020 the original author or authors.
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
package io.lettuce.core.cluster.pubsub;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Interface for Redis Cluster Pub/Sub listeners.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.4
 */
public interface RedisClusterPubSubListener<K, V> {

    /**
     * Message received from a channel subscription.
     *
     * @param node the {@link RedisClusterNode} where the {@literal message} originates.
     * @param channel Channel.
     * @param message Message.
     */
    void message(RedisClusterNode node, K channel, V message);

    /**
     * Message received from a pattern subscription.
     *
     * @param node the {@link RedisClusterNode} where the {@literal message} originates.
     * @param pattern Pattern.
     * @param channel Channel.
     * @param message Message.
     */
    void message(RedisClusterNode node, K pattern, K channel, V message);

    /**
     * Subscribed to a channel.
     *
     * @param node the {@link RedisClusterNode} where the {@literal message} originates.
     * @param channel Channel.
     * @param count Subscription count.
     */
    void subscribed(RedisClusterNode node, K channel, long count);

    /**
     * Subscribed to a pattern.
     *
     * @param pattern Pattern.
     * @param count Subscription count.
     */
    void psubscribed(RedisClusterNode node, K pattern, long count);

    /**
     * Unsubscribed from a channel.
     *
     * @param node the {@link RedisClusterNode} where the {@literal message} originates.
     * @param channel Channel.
     * @param count Subscription count.
     */
    void unsubscribed(RedisClusterNode node, K channel, long count);

    /**
     * Unsubscribed from a pattern.
     *
     * @param node the {@link RedisClusterNode} where the {@literal message} originates.
     * @param pattern Channel.
     * @param count Subscription count.
     */
    void punsubscribed(RedisClusterNode node, K pattern, long count);

}
