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
package io.lettuce.core.api;

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;

/**
 * Builds a command API surface ({@code T}) for a connection, with one factory method per connection family.
 * <p>
 * This is the extension point for command "flavors" (reactive, and in the future RxJava, Mutiny, …). Each flavor provides a
 * {@code CommandsBuilder} implementation; the compiler obliges it to handle every connection family. Conversely, adding a new
 * connection family adds a method here, obliging every flavor to handle it.
 * <p>
 * A connection resolves the correct factory method based on its own family — the caller never selects it. Use through
 * {@link StatefulConnection#commands(CommandsBuilder)}:
 *
 * <pre class="code">
 * 
 * RedisReactiveCommands&lt;K, V&gt; reactive = connection.commands(RedisReactiveCommands.reactive());
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @param <T> the command API surface produced by this builder.
 * @since 7.7
 */
public interface CommandsBuilder<K, V, T extends Commands<K, V>> {

    /**
     * @return the type produced by this builder, used as the cache key so repeated calls return the same instance.
     */
    Class<T> cacheKey();

    /**
     * Create the command API for a standalone connection.
     *
     * @param connection the standalone connection.
     * @return the command API instance.
     */
    T fromStandalone(StatefulRedisConnection<K, V> connection);

    /**
     * Create the command API for a Redis Cluster connection.
     *
     * @param connection the cluster connection.
     * @return the command API instance.
     */
    T fromCluster(StatefulRedisClusterConnection<K, V> connection);

    /**
     * Create the command API for a Pub/Sub connection.
     *
     * @param connection the Pub/Sub connection.
     * @return the command API instance.
     */
    T fromPubSub(StatefulRedisPubSubConnection<K, V> connection);

    /**
     * Create the command API for a Redis Cluster Pub/Sub connection.
     *
     * @param connection the cluster Pub/Sub connection.
     * @return the command API instance.
     */
    T fromClusterPubSub(StatefulRedisClusterPubSubConnection<K, V> connection);

    /**
     * Create the command API for a Redis Sentinel connection.
     *
     * @param connection the Sentinel connection.
     * @return the command API instance.
     */
    T fromSentinel(StatefulRedisSentinelConnection<K, V> connection);

}
