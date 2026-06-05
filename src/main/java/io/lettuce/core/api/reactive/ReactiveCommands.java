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
import io.lettuce.core.api.Commands;
import io.lettuce.core.api.CommandsBuilder;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.RedisAdvancedClusterReactiveCommandsImpl;
import io.lettuce.core.cluster.RedisClusterPubSubReactiveCommandsImpl;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.RedisSentinelReactiveCommandsImpl;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import reactor.core.publisher.Mono;

/**
 * Common reactive command surface shared by every connection family (standalone, cluster, Pub/Sub, sentinel). This is the value
 * type returned by {@link StatefulConnection#commands(CommandsBuilder) connection.commands(ReactiveCommands.reactive())} and
 * the supertype of the family-specific reactive interfaces.
 * <p>
 * It aggregates the granular reactive command interfaces but, unlike {@code RedisReactiveCommands}, does not expose
 * {@code getStatefulConnection()} — that method's covariant return type differs per family and would prevent a common
 * supertype.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 7.7
 */
public interface ReactiveCommands<K, V> extends Commands<K, V>, BaseRedisReactiveCommands<K, V>, RedisAclReactiveCommands<K, V>,
        RedisClusterReactiveCommands<K, V>, RedisFunctionReactiveCommands<K, V>, RedisGeoReactiveCommands<K, V>,
        RedisHashReactiveCommands<K, V>, RedisHLLReactiveCommands<K, V>, RedisKeyReactiveCommands<K, V>,
        RedisListReactiveCommands<K, V>, RedisScriptingReactiveCommands<K, V>, RedisServerReactiveCommands<K, V>,
        RedisSetReactiveCommands<K, V>, RedisSortedSetReactiveCommands<K, V>, RedisStreamReactiveCommands<K, V>,
        RedisStringReactiveCommands<K, V>, RedisTransactionalReactiveCommands<K, V>, RedisJsonReactiveCommands<K, V>,
        RedisVectorSetReactiveCommands<K, V>, RediSearchReactiveCommands<K, V>, RedisArrayReactiveCommands<K, V> {

    /**
     * Builder that resolves the reactive command API matching a connection's family. Use with
     * {@link StatefulConnection#commands(CommandsBuilder)}:
     *
     * <pre class="code">
     * 
     * ReactiveCommands&lt;K, V&gt; reactive = connection.commands(ReactiveCommands.reactive());
     * </pre>
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @return the reactive commands builder.
     * @since 7.7
     */
    static <K, V> CommandsBuilder<K, V, ReactiveCommands<K, V>> builder() {
        return new CommandsBuilder<K, V, ReactiveCommands<K, V>>() {

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public Class<ReactiveCommands<K, V>> cacheKey() {
                return (Class) ReactiveCommands.class;
            }

            @Override
            public ReactiveCommands<K, V> from(StatefulRedisConnection<K, V> connection) {
                return new RedisReactiveCommandsImpl<>(connection, connection.getCodec(),
                        () -> connection.getOptions().getJsonParser().get());
            }

            @Override
            public ReactiveCommands<K, V> from(StatefulRedisClusterConnection<K, V> connection) {
                return new RedisAdvancedClusterReactiveCommandsImpl<>(connection, connection.getCodec(),
                        () -> connection.getOptions().getJsonParser().get());
            }

            @Override
            public ReactiveCommands<K, V> from(StatefulRedisPubSubConnection<K, V> connection) {
                return new RedisPubSubReactiveCommandsImpl<>(connection, connection.getCodec());
            }

            @Override
            public ReactiveCommands<K, V> fromClusterPubSub(StatefulRedisClusterPubSubConnection<K, V> connection) {
                return new RedisClusterPubSubReactiveCommandsImpl<>(connection, connection.getCodec());
            }

            @Override
            public ReactiveCommands<K, V> from(StatefulRedisSentinelConnection<K, V> connection) {
                return new RedisSentinelReactiveCommandsImpl<>(connection, connection.getCodec(),
                        () -> connection.getOptions().getJsonParser().get());
            }

        };
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
     * and the other way around.
     *
     * @param db1 the first database number
     * @param db2 the second database number
     * @return String simple-string-reply
     */
    Mono<String> swapdb(int db1, int db2);

}
