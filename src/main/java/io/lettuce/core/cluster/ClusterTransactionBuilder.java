/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import reactor.core.publisher.Mono;

import io.lettuce.core.*;
import io.lettuce.core.api.reactive.ReactiveTransactionBuilder;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.protocol.TransactionBundle;

/**
 * Cluster-aware implementation of {@link TransactionBuilder} and {@link ReactiveTransactionBuilder}.
 * <p>
 * This builder collects commands locally and validates that all keys hash to the same slot. At execution time, it routes the
 * transaction to the appropriate cluster node.
 * <p>
 * <b>Important:</b> All keys used in the transaction must map to the same slot. If keys span multiple slots, a
 * {@link RedisException} is thrown during command collection.
 * <p>
 * <b>Hash Tags:</b> Keys {@code {user123}:name} and {@code {user123}:email} will both hash to the slot determined by
 * {@code user123}.
 * <p>
 * Example usage:
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Keys with same hash tag - will succeed
 *     TransactionBuilder<String, String> txn = connection.transaction();
 *     txn.queue().set("{user}:name", "John");
 *     txn.queue().set("{user}:email", "john@example.com");
 *     txn.execute();
 *
 *     // Keys without hash tags - will fail with CROSSSLOT error
 *     TransactionBuilder<String, String> txn2 = connection.transaction();
 *     txn2.queue().set("user:name", "John");
 *     txn2.queue().set("product:price", "100"); // Different slot!
 *     // Throws CROSSSLOT error immediately when second command is added
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 * @see TransactionBuilder
 * @see ReactiveTransactionBuilder
 * @see io.lettuce.core.cluster.SlotHash
 */
public class ClusterTransactionBuilder<K, V> implements ReactiveTransactionBuilder<K, V> {

    private final StatefulRedisClusterConnection<K, V> clusterConnection;

    private final RedisCodec<K, V> codec;

    private final K[] watchKeys;

    private final ClusterCommandCollectingAsyncCommands<K, V> collector;

    private final AtomicBoolean executed = new AtomicBoolean();

    /**
     * Create a new ClusterTransactionBuilder.
     *
     * @param clusterConnection the cluster connection.
     * @param codec the codec.
     * @param watchKeys optional keys to WATCH (also used to determine target slot).
     */
    @SafeVarargs
    public ClusterTransactionBuilder(StatefulRedisClusterConnection<K, V> clusterConnection, RedisCodec<K, V> codec,
            K... watchKeys) {
        LettuceAssert.notNull(clusterConnection, "ClusterConnection must not be null");
        LettuceAssert.notNull(codec, "Codec must not be null");

        this.clusterConnection = clusterConnection;
        this.codec = codec;
        this.watchKeys = (watchKeys != null && watchKeys.length > 0) ? watchKeys : null;
        this.collector = new ClusterCommandCollectingAsyncCommands<>(clusterConnection, codec);

        // Validate WATCH keys are all in same slot and set target slot
        if (this.watchKeys != null) {
            Integer targetSlot = null;
            K firstKey = null;
            for (K watchKey : this.watchKeys) {
                if (watchKey != null) {
                    ByteBuffer encoded = codec.encodeKey(watchKey);
                    int slot = SlotHash.getSlot(encoded);
                    if (targetSlot == null) {
                        targetSlot = slot;
                        firstKey = watchKey;
                    } else if (!targetSlot.equals(slot)) {
                        throw new RedisException(String.format(
                                "CROSSSLOT Keys in transaction must hash to the same slot. "
                                        + "Key '%s' hashes to slot %d, but key '%s' hashes to slot %d. "
                                        + "Use hash tags like {tag}key to ensure same slot routing.",
                                firstKey, targetSlot, watchKey, slot));
                    }
                }
            }
            if (targetSlot != null) {
                collector.setTargetSlot(targetSlot);
            }
        }
    }

    /**
     * Returns an async commands interface for adding ANY Redis command to the transaction.
     * <p>
     * Commands added via this interface are validated for slot consistency. If a command uses a key that hashes to a different
     * slot than previous keys, a {@link RedisException} is thrown immediately.
     *
     * @return async commands interface that collects commands for the transaction
     */
    @Override
    public io.lettuce.core.api.async.RedisAsyncCommands<K, V> queue() {
        return collector;
    }

    @Override
    public TransactionBuilder<K, V> addCommand(RawCommand<K, V> command) {
        // Raw commands are slot-validated on the same fail-fast path as typed commands: the collector inspects the
        // command's first encoded key (CommandArgs#getFirstEncodedKey) and throws CROSSSLOT if it maps to a different slot.
        // A raw command whose key is not marked via CommandArgs#addKey carries no detectable key and is therefore the
        // caller's responsibility (it routes to the current target slot and may fail server-side with CROSSSLOT).
        collector.dispatch(command.toCommand());
        return this;
    }

    @Override
    public int size() {
        return collector.size();
    }

    @Override
    public boolean isEmpty() {
        return collector.isEmpty();
    }

    @Override
    public TransactionResult execute() {
        // Honor the connection command timeout and surface the standard timeout/execution exceptions, matching the
        // standalone path and the rest of the sync API instead of blocking unbounded via join().
        return Futures.awaitOrCancel(executeAsync(), clusterConnection.getTimeout().toNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<TransactionResult> executeAsync() {
        if (!executed.compareAndSet(false, true)) {
            throw new IllegalStateException("Transaction has already been executed; create a new transaction builder");
        }
        Integer targetSlot = collector.getTargetSlot();
        if (targetSlot == null) {
            // No key seen (empty transaction, or only key-less commands) - we cannot determine which node to route to.
            throw new RedisException("Cannot execute cluster transaction without a target slot. "
                    + "Use WATCH keys or add commands with keys to determine the target node.");
        }
        RedisClusterNode masterNode = clusterConnection.getPartitions().getMasterBySlot(targetSlot);
        if (masterNode == null) {
            throw new RedisException("Cannot find master node for slot " + targetSlot);
        }

        // Build the bundle now (drains the collected commands), then resolve the node connection asynchronously so neither
        // the calling thread nor a reactive subscriber thread is blocked on connection acquisition (previously .join()).
        List<RedisCommand<K, V, ?>> commands = collector.drainCommands();
        TransactionBundle.checkSize(commands.size(), clusterConnection.getOptions().getMaxTransactionBundleSize());
        TransactionBundle<K, V> bundle = watchKeys != null ? new TransactionBundle<>(codec, commands, watchKeys)
                : new TransactionBundle<>(codec, commands);

        CompletionStage<TransactionResult> result = clusterConnection.getConnectionAsync(masterNode.getNodeId())
                .thenCompose(nodeConnection -> {
                    if (nodeConnection instanceof StatefulRedisConnectionImpl) {
                        return ((StatefulRedisConnectionImpl<K, V>) nodeConnection).dispatchTransactionBundle(bundle)
                                .toCompletableFuture();
                    }
                    CompletableFuture<TransactionResult> failed = new CompletableFuture<>();
                    failed.completeExceptionally(
                            new UnsupportedOperationException("Node connection does not support transaction bundles"));
                    return failed;
                });

        return new PipelinedRedisFuture<>(result);
    }

    @Override
    public Mono<TransactionResult> executeReactive() {
        // Cold publisher: defer so the commands are drained and the bundle is dispatched on subscribe, not at assembly.
        // A second subscription re-invokes executeAsync(), which fails fast via the one-shot guard.
        return Mono.defer(() -> Mono.fromFuture(executeAsync().toCompletableFuture()));
    }

}
