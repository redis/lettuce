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
class CommandCollectingAsyncCommands<K, V> extends AbstractRedisAsyncCommands<K, V>
        implements io.lettuce.core.api.async.RedisAsyncCommands<K, V> {

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
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> AsyncCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {
        // Store the original command (unwrap if already wrapped)
        RedisCommand<K, V, ?> commandToStore = cmd;
        if (cmd instanceof AsyncCommand) {
            commandToStore = ((AsyncCommand<K, V, T>) cmd).getDelegate();
        }
        collectedCommands.add(commandToStore);

        // Return a completed async command (won't be used for actual execution)
        AsyncCommand<K, V, T> asyncCommand = new AsyncCommand<>(cmd);
        // Don't complete it - it will be completed when the transaction executes
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
