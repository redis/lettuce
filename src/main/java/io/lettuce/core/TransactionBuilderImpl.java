/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.util.List;
import java.util.concurrent.ExecutionException;

import reactor.core.publisher.Mono;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.protocol.TransactionBundle;

/**
 * Default implementation of {@link TransactionBuilder}.
 * <p>
 * Uses composition with {@link CommandCollectingAsyncCommands} to leverage the full async API while collecting commands for
 * batch execution. This provides complete Redis command coverage (400+ commands) without code duplication.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 */
class TransactionBuilderImpl<K, V> implements TransactionBuilder<K, V> {

    private final StatefulRedisConnection<K, V> connection;

    private final RedisCodec<K, V> codec;

    private final CommandCollectingAsyncCommands<K, V> collector;

    private final K[] watchKeys;

    /**
     * Creates a new {@link TransactionBuilderImpl}.
     *
     * @param connection the connection to use for execution
     * @param codec the codec for command encoding
     * @param watchKeys optional keys to WATCH
     */
    @SafeVarargs
    TransactionBuilderImpl(StatefulRedisConnection<K, V> connection, RedisCodec<K, V> codec, K... watchKeys) {
        LettuceAssert.notNull(connection, "Connection must not be null");
        LettuceAssert.notNull(codec, "Codec must not be null");
        this.connection = connection;
        this.codec = codec;
        this.collector = new CommandCollectingAsyncCommands<>(connection, codec);
        this.watchKeys = (watchKeys != null && watchKeys.length > 0) ? watchKeys : null;
    }

    @Override
    public RedisAsyncCommands<K, V> commands() {
        return collector;
    }

    @Override
    public TransactionBuilder<K, V> addCommand(RawCommand<K, V> rawCommand) {
        if (rawCommand == null) {
            throw new IllegalArgumentException("RawCommand must not be null");
        }
        collector.dispatch(rawCommand.toCommand(codec));
        return this;
    }

    @Override
    public TransactionResult execute() {
        try {
            return executeAsync().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw ExceptionFactory.createExecutionException(cause != null ? cause.getMessage() : e.getMessage(), cause);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<TransactionResult> executeAsync() {
        List<RedisCommand<K, V, ?>> commandList = collector.drainCommands();
        TransactionBundle<K, V> bundle = watchKeys != null ? new TransactionBundle<>(codec, commandList, watchKeys)
                : new TransactionBundle<>(codec, commandList);
        if (connection instanceof StatefulRedisConnectionImpl) {
            return ((StatefulRedisConnectionImpl<K, V>) connection).dispatchTransactionBundle(bundle);
        }
        throw new UnsupportedOperationException("Connection does not support transaction bundles");
    }

    @Override
    public Mono<TransactionResult> executeReactive() {
        return Mono.fromFuture(executeAsync()::toCompletableFuture);
    }

    @Override
    public int size() {
        return collector.size();
    }

    @Override
    public boolean isEmpty() {
        return collector.isEmpty();
    }

}
