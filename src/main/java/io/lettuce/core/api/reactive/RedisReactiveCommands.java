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
import io.lettuce.core.api.CommandsFactory;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands;
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

    /**
     * Create a new reactive transaction builder for atomic transaction dispatch.
     * <p>
     * This method creates a {@link ReactiveTransactionBuilder} that collects commands and dispatches them atomically as a
     * single MULTI/EXEC block. Use {@link ReactiveTransactionBuilder#executeReactive()} to execute the transaction and receive
     * the result as a {@link Mono}.
     * <p>
     * Example usage:
     *
     * <pre>
     *
     * {
     *     &#64;code
     *     ReactiveTransactionBuilder<String, String> txn = connection.reactive().transaction();
     *     txn.queue().set("key1", "value1");
     *     txn.queue().incr("counter");
     *     Mono<TransactionResult> result = txn.executeReactive();
     * }
     * </pre>
     *
     * @return a new reactive transaction builder
     * @since 7.6
     */
    ReactiveTransactionBuilder<K, V> transaction();

    /**
     * Create a new reactive transaction builder with WATCH support for optimistic locking.
     * <p>
     * The specified keys will be watched before the transaction is executed. If any of the watched keys are modified by another
     * client before the transaction executes, the entire transaction will be aborted.
     *
     * @param watchKeys the keys to watch
     * @return a new reactive transaction builder with WATCH
     * @since 7.6
     */
    @SuppressWarnings("unchecked")
    ReactiveTransactionBuilder<K, V> transaction(K... watchKeys);

    /**
     * Execute a transaction reactively using a functional builder pattern.
     * <p>
     * This method provides a more natural API for reactive chains. The transaction is built via the consumer and executed when
     * the returned {@link Mono} is subscribed to.
     * <p>
     * Example usage:
     *
     * <pre>
     *
     * {
     *     &#64;code
     *     // Simple usage
     *     Mono<TransactionResult> result = commands.transactional(txn -> {
     *         txn.set("key1", "value1");
     *         txn.incr("counter");
     *     });
     *
     *     // Composing with reactive data sources
     *     Mono<TransactionResult> result = keyMono.flatMap(key -> valueMono.flatMap(value -> commands.transactional(txn -> {
     *         txn.set(key, value);
     *         txn.incr("counter");
     *     })));
     * }
     * </pre>
     *
     * @param transactionBody consumer that receives a commands interface to add commands to the transaction
     * @return a Mono that emits the transaction result when subscribed
     * @since 7.6
     */
    Mono<TransactionResult> transactional(
            java.util.function.Consumer<io.lettuce.core.api.async.RedisAsyncCommands<K, V>> transactionBody);

    /**
     * Execute a transaction reactively with WATCH keys using a functional builder pattern.
     *
     * @param transactionBody consumer that receives a commands interface to add commands to the transaction
     * @param watchKeys the keys to watch
     * @return a Mono that emits the transaction result when subscribed
     * @since 7.6
     */
    @SuppressWarnings("unchecked")
    Mono<TransactionResult> transactional(
            java.util.function.Consumer<io.lettuce.core.api.async.RedisAsyncCommands<K, V>> transactionBody, K... watchKeys);

    /**
     * Returns the {@link CommandsFactory} that creates {@link RedisReactiveCommands}, for use with
     * {@link io.lettuce.core.api.StatefulRedisConnection#commands(CommandsFactory)}:
     *
     * <pre>
     *
     * {
     *     &#64;code
     *     RedisReactiveCommands<K, V> reactive = connection.commands(RedisReactiveCommands.factory());
     * }
     * </pre>
     *
     * @param <K> Key type
     * @param <V> Value type
     * @return the factory that creates {@link RedisReactiveCommands}
     * @since 7.7
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static <K, V> CommandsFactory<StatefulRedisConnection<K, V>, RedisReactiveCommands<K, V>> factory() {
        return (CommandsFactory) FactoryHolder.INSTANCE;
    }

    /**
     * Holds the singleton factory so {@link #factory()} returns one shared instance (no per-call allocation); {@code factory()}
     * re-applies {@code <K, V>}.
     */
    final class FactoryHolder {

        private FactoryHolder() {
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static final CommandsFactory INSTANCE = CommandsFactory.of(RedisReactiveCommands.class,
                (StatefulRedisConnection c) -> new RedisReactiveCommandsImpl(c, c.getCodec(),
                        () -> c.getOptions().getJsonParser().get()));

    }

}
