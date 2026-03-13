/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import io.lettuce.core.api.async.RedisAsyncCommands;
import reactor.core.publisher.Mono;

/**
 * Builder interface for constructing Redis transactions that are executed atomically as a single unit.
 * <p>
 * Unlike the traditional {@code MULTI}/{@code EXEC} approach where commands are sent individually, a {@link TransactionBuilder}
 * collects all commands and dispatches them atomically when {@link #execute()}, {@link #executeAsync()}, or
 * {@link #executeReactive()} is called. This ensures thread-safety when sharing connections across multiple threads.
 * <p>
 * Use {@link #commands()} to access the full Redis async commands API (400+ commands). Commands invoked on this interface are
 * collected for batch execution rather than being dispatched immediately.
 * <p>
 * Example usage:
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     TransactionBuilder<String, String> txn = connection.transaction();
 *     txn.commands().set("key1", "value1");
 *     txn.commands().set("key2", "value2");
 *     txn.commands().incr("counter");
 *     TransactionResult result = txn.execute();
 *
 *     String setResult = result.get(0); // "OK"
 *     Long counterValue = result.get(2); // new counter value
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 * @see TransactionResult
 */
public interface TransactionBuilder<K, V> {

    /**
     * Returns an async commands interface for adding ANY Redis command to the transaction.
     * <p>
     * This provides access to all 400+ Redis async commands, enabling complete command coverage. Commands invoked on the
     * returned interface are collected for batch execution rather than being dispatched immediately.
     * <p>
     * Example usage:
     *
     * <pre>
     * 
     * {
     *     &#64;code
     *     TransactionBuilder<String, String> txn = connection.transaction();
     *     txn.commands().set("key1", "value1");
     *     txn.commands().hset("hash", "field", "value");
     *     txn.commands().zadd("zset", 1.0, "member");
     *     txn.commands().lpush("list", "item1", "item2");
     *     TransactionResult result = txn.execute();
     * }
     * </pre>
     *
     * @return async commands interface that collects commands for the transaction
     */
    RedisAsyncCommands<K, V> commands();

    /**
     * Add a raw command to the transaction.
     * <p>
     * This method allows adding any Redis command that may not be directly supported by the async commands interface.
     *
     * @param args the command with its arguments.
     * @return this builder for chaining.
     */
    TransactionBuilder<K, V> addCommand(RawCommand<K, V> args);

    /**
     * Execute the transaction synchronously.
     * <p>
     * This method dispatches all collected commands atomically as a single MULTI/EXEC block and waits for the result.
     *
     * @return the transaction result containing the results of all commands.
     * @throws io.lettuce.core.RedisException if the transaction fails.
     */
    TransactionResult execute();

    /**
     * Execute the transaction asynchronously.
     * <p>
     * This method dispatches all collected commands atomically as a single MULTI/EXEC block and returns immediately with a
     * future.
     *
     * @return a future that completes with the transaction result.
     */
    RedisFuture<TransactionResult> executeAsync();

    /**
     * Execute the transaction reactively.
     * <p>
     * This method dispatches all collected commands atomically as a single MULTI/EXEC block when the returned Mono is
     * subscribed to.
     *
     * @return a Mono that emits the transaction result.
     */
    Mono<TransactionResult> executeReactive();

    /**
     * Get the number of commands currently in this transaction.
     *
     * @return the number of commands.
     */
    int size();

    /**
     * Check if this transaction contains no commands.
     *
     * @return {@code true} if no commands have been added.
     */
    boolean isEmpty();

}
