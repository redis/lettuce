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

import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.async.RedisClusterPubSubAsyncCommands;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.async.RedisSentinelAsyncCommands;

/**
 * A complete asynchronous and thread-safe Redis API with 400+ Methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @author Tihomir Mateev
 * @author Yordan Tsintsov
 * @since 3.0
 */
public interface RedisAsyncCommands<K, V> extends BaseRedisAsyncCommands<K, V>, RedisAclAsyncCommands<K, V>,
        RedisClusterAsyncCommands<K, V>, RedisFunctionAsyncCommands<K, V>, RedisGeoAsyncCommands<K, V>,
        RedisHashAsyncCommands<K, V>, RedisHLLAsyncCommands<K, V>, RedisKeyAsyncCommands<K, V>, RedisListAsyncCommands<K, V>,
        RedisScriptingAsyncCommands<K, V>, RedisServerAsyncCommands<K, V>, RedisSetAsyncCommands<K, V>,
        RedisSortedSetAsyncCommands<K, V>, RedisStreamAsyncCommands<K, V>, RedisStringAsyncCommands<K, V>,
        RedisTransactionalAsyncCommands<K, V>, RedisJsonAsyncCommands<K, V>, RedisVectorSetAsyncCommands<K, V>,
        RediSearchAsyncCommands<K, V>, RedisArrayAsyncCommands<K, V>, RedisBloomFilterAsyncCommands<K, V> {

    /**
     * Obtain the {@link RedisAsyncCommands} API for the given connection.
     *
     * @param connection the stateful Redis connection, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the async commands API.
     * @since 7.0
     */
    static <K, V> RedisAsyncCommands<K, V> from(StatefulRedisConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        if (connection instanceof StatefulRedisPubSubConnection) {
            return from((StatefulRedisPubSubConnection<K, V>) connection);
        }
        return RedisChannelHandler.getCommands(connection, RedisAsyncCommands.class);
    }

    /**
     * Obtain the {@link RedisAdvancedClusterAsyncCommands} API for the given cluster connection.
     *
     * @param connection the stateful Redis Cluster connection, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the async Cluster commands API.
     * @since 7.0
     */
    static <K, V> RedisAdvancedClusterAsyncCommands<K, V> from(StatefulRedisClusterConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        return RedisChannelHandler.getCommands(connection, RedisAdvancedClusterAsyncCommands.class);
    }

    /**
     * Obtain the {@link RedisPubSubAsyncCommands} API for the given Pub/Sub connection.
     *
     * @param connection the stateful Redis Pub/Sub connection, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the async Pub/Sub commands API.
     * @since 7.0
     */
    static <K, V> RedisPubSubAsyncCommands<K, V> from(StatefulRedisPubSubConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        if (connection instanceof StatefulRedisClusterPubSubConnection) {
            return from((StatefulRedisClusterPubSubConnection<K, V>) connection);
        }
        return RedisChannelHandler.getCommands(connection, RedisPubSubAsyncCommands.class);
    }

    /**
     * Obtain the {@link RedisSentinelAsyncCommands} API for the given Sentinel connection.
     *
     * @param connection the stateful Redis Sentinel connection, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the async Sentinel commands API.
     * @since 7.0
     */
    static <K, V> RedisSentinelAsyncCommands<K, V> from(StatefulRedisSentinelConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        return RedisChannelHandler.getCommands(connection, RedisSentinelAsyncCommands.class);
    }

    /**
     * Obtain the {@link RedisClusterPubSubAsyncCommands} API for the given Cluster Pub/Sub connection.
     *
     * @param connection the stateful Redis Cluster Pub/Sub connection, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the async Cluster Pub/Sub commands API.
     * @since 7.0
     */
    static <K, V> RedisClusterPubSubAsyncCommands<K, V> from(StatefulRedisClusterPubSubConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        return RedisChannelHandler.getCommands(connection, RedisClusterPubSubAsyncCommands.class);
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
     * Swap two Redis databases, so that immediately all the clients connected to a given DB will see the data of the other DB,
     * and the other way around
     *
     * @param db1 the first database number
     * @param db2 the second database number
     * @return String simple-string-reply
     */
    RedisFuture<String> swapdb(int db1, int db2);

    /**
     * @return the underlying connection.
     * @since 6.2, will be removed with Lettuce 7 to avoid exposing the underlying connection.
     */
    @Deprecated
    StatefulRedisConnection<K, V> getStatefulConnection();

    /**
     * @return the currently configured instance of the {@link JsonParser}
     * @since 6.5
     */
    JsonParser getJsonParser();

}
