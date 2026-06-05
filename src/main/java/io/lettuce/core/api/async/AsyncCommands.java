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
package io.lettuce.core.api.async;

import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.Commands;
import io.lettuce.core.api.CommandsFactory;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisAdvancedClusterAsyncCommandsImpl;
import io.lettuce.core.cluster.RedisClusterPubSubAsyncCommandsImpl;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.RedisPubSubAsyncCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.RedisSentinelAsyncCommandsImpl;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;

/**
 * Common asynchronous command surface shared by every connection family (standalone, cluster, Pub/Sub, sentinel). This is the
 * value type returned by {@link StatefulConnection#commands(CommandsFactory) connection.commands(AsyncCommands.async())} and the
 * supertype of the family-specific asynchronous interfaces.
 * <p>
 * It aggregates the granular asynchronous command interfaces but, unlike {@code RedisAsyncCommands}, does not expose
 * {@code getStatefulConnection()} — that method's covariant return type differs per family and would prevent a common
 * supertype.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 7.7
 */
public interface AsyncCommands<K, V> extends Commands<K, V>, BaseRedisAsyncCommands<K, V>, RedisAclAsyncCommands<K, V>,
        RedisClusterAsyncCommands<K, V>, RedisFunctionAsyncCommands<K, V>, RedisGeoAsyncCommands<K, V>,
        RedisHashAsyncCommands<K, V>, RedisHLLAsyncCommands<K, V>, RedisKeyAsyncCommands<K, V>, RedisListAsyncCommands<K, V>,
        RedisScriptingAsyncCommands<K, V>, RedisServerAsyncCommands<K, V>, RedisSetAsyncCommands<K, V>,
        RedisSortedSetAsyncCommands<K, V>, RedisStreamAsyncCommands<K, V>, RedisStringAsyncCommands<K, V>,
        RedisTransactionalAsyncCommands<K, V>, RedisJsonAsyncCommands<K, V>, RedisVectorSetAsyncCommands<K, V>,
        RediSearchAsyncCommands<K, V>, RedisArrayAsyncCommands<K, V> {

    /**
     * Factory that resolves the asynchronous command API matching a connection's family. Use with
     * {@link StatefulConnection#commands(CommandsFactory)}:
     *
     * <pre class="code">
     * AsyncCommands&lt;K, V&gt; async = connection.commands(AsyncCommands.async());
     * </pre>
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the asynchronous commands factory.
     * @since 7.7
     */
    static <K, V> CommandsFactory<K, V, AsyncCommands<K, V>> async() {
        return new CommandsFactory<K, V, AsyncCommands<K, V>>() {

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public Class<AsyncCommands<K, V>> cacheKey() {
                return (Class) AsyncCommands.class;
            }

            @Override
            public AsyncCommands<K, V> from(StatefulRedisConnection<K, V> connection) {
                return new RedisAsyncCommandsImpl<>(connection, connection.getCodec(),
                        () -> connection.getOptions().getJsonParser().get());
            }

            @Override
            public AsyncCommands<K, V> from(StatefulRedisClusterConnection<K, V> connection) {
                return new RedisAdvancedClusterAsyncCommandsImpl<>(connection, connection.getCodec(),
                        () -> connection.getOptions().getJsonParser().get());
            }

            @Override
            public AsyncCommands<K, V> from(StatefulRedisPubSubConnection<K, V> connection) {
                return new RedisPubSubAsyncCommandsImpl<>(connection, connection.getCodec());
            }

            @Override
            public AsyncCommands<K, V> fromClusterPubSub(StatefulRedisClusterPubSubConnection<K, V> connection) {
                return new RedisClusterPubSubAsyncCommandsImpl<>(connection, connection.getCodec());
            }

            @Override
            public AsyncCommands<K, V> from(StatefulRedisSentinelConnection<K, V> connection) {
                return new RedisSentinelAsyncCommandsImpl<>(connection, connection.getCodec());
            }

        };
    }

    /**
     * Authenticate to the server.
     *
     * @param password the password
     * @return String simple-string-reply
     */
    RedisFuture<String> auth(CharSequence password);

    /**
     * Authenticate to the server with username and password. Requires Redis 6 or newer.
     *
     * @param username the username
     * @param password the password
     * @return String simple-string-reply
     * @since 6.0
     */
    RedisFuture<String> auth(String username, CharSequence password);

    /**
     * Change the selected database for the current connection.
     *
     * @param db the database number
     * @return String simple-string-reply
     */
    RedisFuture<String> select(int db);

    /**
     * Swap two Redis databases.
     *
     * @param db1 the first database number
     * @param db2 the second database number
     * @return String simple-string-reply
     */
    RedisFuture<String> swapdb(int db1, int db2);

}
