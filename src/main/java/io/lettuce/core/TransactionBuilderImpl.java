/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import reactor.core.publisher.Mono;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.ReactiveTransactionBuilder;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.protocol.TransactionBundle;

/**
 * Default implementation of {@link TransactionBuilder} and {@link ReactiveTransactionBuilder}.
 * <p>
 * Uses composition with {@link CommandCollectingAsyncCommands} to leverage the full async API while collecting commands for
 * batch execution. This provides complete Redis command coverage (400+ commands) without code duplication.
 * <p>
 * This implementation supports both sync/async execution (via {@link TransactionBuilder}) and reactive execution (via
 * {@link ReactiveTransactionBuilder}).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 */
class TransactionBuilderImpl<K, V> implements ReactiveTransactionBuilder<K, V> {

    private final StatefulRedisConnection<K, V> connection;

    private final RedisCodec<K, V> codec;

    private final CommandCollectingAsyncCommands<K, V> collector;

    private final K[] watchKeys;

    private final AtomicBoolean executed = new AtomicBoolean();

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
    public RedisAsyncCommands<K, V> queue() {
        return collector;
    }

    @Override
    public TransactionBuilder<K, V> addCommand(RawCommand<K, V> rawCommand) {
        if (rawCommand == null) {
            throw new IllegalArgumentException("RawCommand must not be null");
        }
        collector.dispatch(rawCommand.toCommand());
        return this;
    }

    @Override
    public TransactionResult execute() {
        // Mirror the rest of the sync API (FutureSyncInvocationHandler): honor the connection command timeout and surface
        // the standard RedisCommandTimeoutException / RedisCommandExecutionException instead of blocking unbounded on get().
        return Futures.awaitOrCancel(executeAsync(), connection.getTimeout().toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<TransactionResult> executeAsync() {
        if (!executed.compareAndSet(false, true)) {
            throw new IllegalStateException("Transaction has already been executed; create a new transaction builder");
        }
        List<RedisCommand<K, V, ?>> commandList = collector.drainCommands();
        TransactionBundle.checkSize(commandList.size(), connection.getOptions().getMaxTransactionBundleSize());
        TransactionBundle<K, V> bundle = watchKeys != null ? new TransactionBundle<>(codec, commandList, watchKeys)
                : new TransactionBundle<>(codec, commandList);
        if (connection instanceof StatefulRedisConnectionImpl) {
            return ((StatefulRedisConnectionImpl<K, V>) connection).dispatchTransactionBundle(bundle);
        }
        throw new UnsupportedOperationException("Connection does not support transaction bundles");
    }

    @Override
    public Mono<TransactionResult> executeReactive() {
        // Cold publisher: defer so the commands are drained and the bundle is dispatched on subscribe, not at assembly.
        // A second subscription re-invokes executeAsync(), which fails fast via the one-shot guard.
        return Mono.defer(() -> Mono.fromFuture(executeAsync().toCompletableFuture()));
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
