/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.RedisCommand;

/**
 * An async commands implementation that collects commands instead of dispatching them. Used internally by
 * {@link TransactionBuilderImpl} to build command lists for bundled transactions.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 */
class CommandCollectingAsyncCommands<K, V> extends AbstractRedisAsyncCommands<K, V> implements RedisAsyncCommands<K, V> {

    private final StatefulRedisConnection<K, V> statefulConnection;

    private final List<RedisCommand<K, V, ?>> collectedCommands = new ArrayList<>();

    /**
     * Creates a new instance.
     *
     * @param connection the actual connection (used only for metadata, not for dispatching)
     * @param codec the codec for command encoding
     */
    CommandCollectingAsyncCommands(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.statefulConnection = connection;
    }

    @Override
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        return statefulConnection;
    }

    /**
     * Overrides dispatch to collect commands instead of sending them. The commands are stored for later batch execution.
     * <p>
     * The {@link AsyncCommand} that is returned to the caller is the very instance that is stored and later handed to the
     * {@link io.lettuce.core.protocol.TransactionBundle}. This is what allows the bundle to complete the exact future the
     * caller holds once the transaction's {@code EXEC} response arrives (or to fail/cancel it if the transaction is discarded
     * or aborted), instead of leaving it dangling.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> AsyncCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {
        AsyncCommand<K, V, T> asyncCommand = (cmd instanceof AsyncCommand) ? (AsyncCommand<K, V, T>) cmd
                : new AsyncCommand<>(cmd);
        collectedCommands.add(asyncCommand);
        return asyncCommand;
    }

    /**
     * Returns all collected commands and clears the internal list.
     *
     * @return the list of collected commands
     */
    List<RedisCommand<K, V, ?>> drainCommands() {
        List<RedisCommand<K, V, ?>> result = new ArrayList<>(collectedCommands);
        collectedCommands.clear();
        return result;
    }

    /**
     * Returns the current number of collected commands.
     *
     * @return number of commands
     */
    int size() {
        return collectedCommands.size();
    }

    /**
     * Returns whether no commands have been collected.
     *
     * @return true if no commands collected
     */
    boolean isEmpty() {
        return collectedCommands.isEmpty();
    }

}
