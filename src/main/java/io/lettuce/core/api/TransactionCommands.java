/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api;

import java.util.function.Consumer;

import io.lettuce.core.DefaultTransactionCommands;
import io.lettuce.core.TransactionBuilder;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.async.RedisAsyncCommands;

/**
 * Stateless entry point for atomic bundled transactions, obtained through the connection's
 * {@link StatefulRedisConnection#commands(CommandsFactory) commands(factory)} acquisition:
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     TransactionCommands<String, String> tx = connection.commands(TransactionCommands.factory());
 *     TransactionResult result = tx.transactional(t -> {
 *         t.set("key", "value");
 *         t.incr("counter");
 *     });
 * }
 * </pre>
 *
 * <p>
 * The entry point itself is cached once per connection (it holds no per-transaction state). Each {@link #create()} still mints
 * a <em>fresh</em>, single-use {@link TransactionBuilder}; the functional {@code transactional(...)} forms create and drain a
 * builder for you. This aligns transaction acquisition with the {@code commands(factory)} direction rather than adding bespoke
 * connection accessors, while keeping {@link StatefulRedisConnection#transaction()} available as a thin convenience.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 * @see StatefulRedisConnection#commands(CommandsFactory)
 * @see TransactionBuilder
 */
public interface TransactionCommands<K, V> {

    /**
     * Create a new, single-use transaction builder.
     *
     * @return a fresh {@link TransactionBuilder}.
     */
    TransactionBuilder<K, V> create();

    /**
     * Create a new, single-use transaction builder with WATCH keys.
     * <p>
     * The watched keys enable <em>watch-then-blind-write</em> only; a bundle cannot read-then-decide (see
     * {@link StatefulRedisConnection#transaction(Object[])}).
     *
     * @param watchKeys the keys to watch.
     * @return a fresh {@link TransactionBuilder} with WATCH.
     */
    @SuppressWarnings("unchecked")
    TransactionBuilder<K, V> create(K... watchKeys);

    /**
     * Execute a transaction synchronously using a functional builder. Equivalent to
     * {@link io.lettuce.core.api.sync.RedisCommands#transactional(Consumer)}.
     *
     * @param transactionBody consumer that receives a commands interface to add commands to the transaction.
     * @return the transaction result.
     */
    TransactionResult transactional(Consumer<RedisAsyncCommands<K, V>> transactionBody);

    /**
     * Execute a transaction synchronously with WATCH keys using a functional builder.
     *
     * @param transactionBody consumer that receives a commands interface to add commands to the transaction.
     * @param watchKeys the keys to watch.
     * @return the transaction result.
     */
    @SuppressWarnings("unchecked")
    TransactionResult transactional(Consumer<RedisAsyncCommands<K, V>> transactionBody, K... watchKeys);

    /**
     * Returns the {@link CommandsFactory} that creates {@link TransactionCommands}, for use with
     * {@link StatefulRedisConnection#commands(CommandsFactory)}:
     *
     * <pre>
     *
     * {
     *     &#64;code
     *     TransactionCommands<K, V> tx = connection.commands(TransactionCommands.factory());
     * }
     * </pre>
     *
     * @param <K> Key type
     * @param <V> Value type
     * @return the factory that creates {@link TransactionCommands}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static <K, V> CommandsFactory<StatefulRedisConnection<K, V>, TransactionCommands<K, V>> factory() {
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
        private static final CommandsFactory INSTANCE = CommandsFactory.of(TransactionCommands.class,
                (StatefulRedisConnection c) -> new DefaultTransactionCommands(c));

    }

}
