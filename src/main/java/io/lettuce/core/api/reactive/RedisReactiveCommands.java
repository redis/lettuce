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
package io.lettuce.core.api.reactive;

import io.lettuce.core.RedisReactiveCommandsImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisAdvancedClusterReactiveCommandsImpl;
import io.lettuce.core.cluster.RedisClusterPubSubReactiveCommandsImpl;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.reactive.RedisClusterPubSubReactiveCommands;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.core.sentinel.RedisSentinelReactiveCommandsImpl;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import reactor.core.publisher.Mono;

/**
 * A complete reactive and thread-safe Redis API with 400+ Methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @author Yordan Tsintsov
 * @since 5.0
 */
public interface RedisReactiveCommands<K, V>
        extends BaseRedisReactiveCommands<K, V>, RedisAclReactiveCommands<K, V>, RedisClusterReactiveCommands<K, V>,
        RedisFunctionReactiveCommands<K, V>, RedisGeoReactiveCommands<K, V>, RedisHashReactiveCommands<K, V>,
        RedisHLLReactiveCommands<K, V>, RedisKeyReactiveCommands<K, V>, RedisListReactiveCommands<K, V>,
        RedisScriptingReactiveCommands<K, V>, RedisServerReactiveCommands<K, V>, RedisSetReactiveCommands<K, V>,
        RedisSortedSetReactiveCommands<K, V>, RedisStreamReactiveCommands<K, V>, RedisStringReactiveCommands<K, V>,
        RedisTransactionalReactiveCommands<K, V>, RedisJsonReactiveCommands<K, V>, RedisVectorSetReactiveCommands<K, V>,
        RediSearchReactiveCommands<K, V>, RedisArrayReactiveCommands<K, V>, RedisBloomFilterReactiveCommands<K, V> {

    /**
     * Obtain the {@link RedisReactiveCommands} API for the given connection.
     *
     * @param connection the stateful Redis connection, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the reactive commands API.
     * @since 7.7
     */
    static <K, V> RedisReactiveCommands<K, V> from(StatefulRedisConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        return new RedisReactiveCommandsImpl<>(connection, connection.getCodec(),
                () -> connection.getOptions().getJsonParser().get());
    }

    /**
     * Obtain the {@link RedisAdvancedClusterReactiveCommands} API for the given cluster connection.
     *
     * @param connection the stateful Redis Cluster connection, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the reactive Cluster commands API.
     * @since 7.7
     */
    static <K, V> RedisAdvancedClusterReactiveCommands<K, V> from(StatefulRedisClusterConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        return new RedisAdvancedClusterReactiveCommandsImpl<>(connection, connection.getCodec(),
                () -> connection.getOptions().getJsonParser().get());
    }

    /**
     * Obtain the {@link RedisPubSubReactiveCommands} API for the given Pub/Sub connection.
     *
     * @param connection the stateful Redis Pub/Sub connection, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the reactive Pub/Sub commands API.
     * @since 7.7
     */
    static <K, V> RedisPubSubReactiveCommands<K, V> from(StatefulRedisPubSubConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        return new RedisPubSubReactiveCommandsImpl<>(connection, connection.getCodec());
    }

    /**
     * Obtain the {@link RedisSentinelReactiveCommands} API for the given Sentinel connection.
     *
     * @param connection the stateful Redis Sentinel connection, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the reactive Sentinel commands API.
     * @since 7.7
     */
    static <K, V> RedisSentinelReactiveCommands<K, V> from(StatefulRedisSentinelConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        return new RedisSentinelReactiveCommandsImpl<>(connection, connection.getCodec(),
                () -> connection.getOptions().getJsonParser().get());
    }

    /**
     * Obtain the {@link RedisClusterPubSubReactiveCommands} API for the given Cluster Pub/Sub connection.
     *
     * @param connection the stateful Redis Cluster Pub/Sub connection, must not be {@code null}.
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the reactive Cluster Pub/Sub commands API.
     * @since 7.7
     */
    static <K, V> RedisClusterPubSubReactiveCommands<K, V> from(StatefulRedisClusterPubSubConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        return new RedisClusterPubSubReactiveCommandsImpl<>(connection, connection.getCodec());
    }

    /**
     * Authenticate to the server.
     *
     * @param password the password
     * @return String simple-string-reply
     */
    Mono<String> auth(CharSequence password);

    /**
     * Authenticate to the server with username and password. Requires Redis 6 or newer.
     *
     * @param username the username
     * @param password the password
     * @return String simple-string-reply
     * @since 6.0
     */
    Mono<String> auth(String username, CharSequence password);

    /**
     * Change the selected database for the current connection.
     *
     * @param db the database number
     * @return String simple-string-reply
     */
    Mono<String> select(int db);

    /**
     * Swap two Redis databases, so that immediately all the clients connected to a given DB will see the data of the other DB,
     * and the other way around
     *
     * @param db1 the first database number
     * @param db2 the second database number
     * @return String simple-string-reply
     */
    Mono<String> swapdb(int db1, int db2);

    /**
     * @return the underlying connection.
     * @since 6.2, will be removed with Lettuce 7 to avoid exposing the underlying connection.
     */
    @Deprecated
    StatefulRedisConnection<K, V> getStatefulConnection();

}
