/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.util.function.Consumer;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.TransactionCommands;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Default {@link TransactionCommands} implementation. Stateless (holds only the connection), so it is safe to cache one
 * instance per connection via {@link StatefulRedisConnection#commands(io.lettuce.core.api.CommandsFactory)}; each
 * {@link #create()} still mints a fresh, single-use {@link TransactionBuilder}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 */
public class DefaultTransactionCommands<K, V> implements TransactionCommands<K, V> {

    private final StatefulRedisConnection<K, V> connection;

    /**
     * Create a new {@link DefaultTransactionCommands}.
     *
     * @param connection the connection transactions are dispatched on, must not be {@code null}.
     */
    public DefaultTransactionCommands(StatefulRedisConnection<K, V> connection) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        this.connection = connection;
    }

    @Override
    public TransactionBuilder<K, V> create() {
        return connection.transaction();
    }

    @Override
    @SafeVarargs
    public final TransactionBuilder<K, V> create(K... watchKeys) {
        return (watchKeys != null && watchKeys.length > 0) ? connection.transaction(watchKeys) : connection.transaction();
    }

    @Override
    public TransactionResult transactional(Consumer<RedisAsyncCommands<K, V>> transactionBody) {
        return transactional(transactionBody, (K[]) null);
    }

    @Override
    @SafeVarargs
    public final TransactionResult transactional(Consumer<RedisAsyncCommands<K, V>> transactionBody, K... watchKeys) {
        TransactionBuilder<K, V> tx = create(watchKeys);
        transactionBody.accept(tx.queue());
        return tx.execute();
    }

}
