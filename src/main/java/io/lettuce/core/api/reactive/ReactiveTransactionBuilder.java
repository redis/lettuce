/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api.reactive;

import reactor.core.publisher.Mono;

import io.lettuce.core.TransactionBuilder;
import io.lettuce.core.TransactionResult;

/**
 * Reactive extension of {@link TransactionBuilder} that adds reactive execution support.
 * <p>
 * This interface extends the base {@link TransactionBuilder} with reactive execution capabilities using Project Reactor. It is
 * located in the reactive package to isolate the Reactor dependency from non-reactive code paths.
 * <p>
 * Obtain an instance via {@link RedisReactiveCommands#transaction()} or by casting the result of
 * {@link io.lettuce.core.api.StatefulRedisConnection#transaction()} when reactive execution is needed.
 * <p>
 * Example usage:
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Via reactive commands (preferred)
 *     ReactiveTransactionBuilder<String, String> txn = connection.reactive().transaction();
 *     txn.queue().set("key1", "value1");
 *     txn.queue().incr("counter");
 *     Mono<TransactionResult> result = txn.executeReactive();
 *
 *     // Or cast from connection.transaction() if needed
 *     ReactiveTransactionBuilder<String, String> txn = (ReactiveTransactionBuilder<String, String>) connection.transaction();
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 * @see TransactionBuilder
 * @see TransactionResult
 */
public interface ReactiveTransactionBuilder<K, V> extends TransactionBuilder<K, V> {

    /**
     * Execute the transaction reactively.
     * <p>
     * This method dispatches all collected commands atomically as a single MULTI/EXEC block when the returned {@link Mono} is
     * subscribed to.
     *
     * @return a Mono that emits the transaction result when subscribed
     */
    Mono<TransactionResult> executeReactive();

}
