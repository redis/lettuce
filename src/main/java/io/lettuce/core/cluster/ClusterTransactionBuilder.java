/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import java.nio.ByteBuffer;
import java.util.List;

import reactor.core.publisher.Mono;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.protocol.TransactionBundle;

/**
 * Cluster-aware implementation of {@link TransactionBuilder}.
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
 *     txn.commands().set("{user}:name", "John");
 *     txn.commands().set("{user}:email", "john@example.com");
 *     txn.execute();
 *
 *     // Keys without hash tags - will fail with CROSSSLOT error
 *     TransactionBuilder<String, String> txn2 = connection.transaction();
 *     txn2.commands().set("user:name", "John");
 *     txn2.commands().set("product:price", "100"); // Different slot!
 *     // Throws CROSSSLOT error immediately when second command is added
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 * @see TransactionBuilder
 * @see io.lettuce.core.cluster.SlotHash
 */
public class ClusterTransactionBuilder<K, V> implements TransactionBuilder<K, V> {

    private final StatefulRedisClusterConnection<K, V> clusterConnection;

    private final RedisCodec<K, V> codec;

    private final K[] watchKeys;

    private final ClusterCommandCollectingAsyncCommands<K, V> collector;

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
     * Get the node connection for the target slot.
     *
     * @return the node connection.
     */
    private StatefulRedisConnection<K, V> getNodeConnection() {
        Integer targetSlot = collector.getTargetSlot();
        if (targetSlot == null) {
            throw new RedisException("No keys have been added to the transaction yet");
        }
        RedisClusterNode masterNode = clusterConnection.getPartitions().getMasterBySlot(targetSlot);
        if (masterNode == null) {
            throw new RedisException("Cannot find master node for slot " + targetSlot);
        }
        return clusterConnection.getConnectionAsync(masterNode.getNodeId()).join();
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
    public io.lettuce.core.api.async.RedisAsyncCommands<K, V> commands() {
        return collector;
    }

    @Override
    public TransactionBuilder<K, V> addCommand(RawCommand<K, V> command) {
        // For raw commands, we cannot easily extract keys to validate slots
        // The user is responsible for ensuring slot consistency
        collector.dispatch(command.toCommand(codec));
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
        return executeAsync().toCompletableFuture().join();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RedisFuture<TransactionResult> executeAsync() {
        Integer targetSlot = collector.getTargetSlot();
        if (collector.isEmpty() && targetSlot == null) {
            // Empty transaction with no target slot - need at least WATCH keys or a command with key
            throw new RedisException("Cannot execute empty cluster transaction without target slot. "
                    + "Use WATCH keys or add commands with keys to determine target node.");
        }

        // Get the node connection for the target slot
        StatefulRedisConnection<K, V> nodeConnection = getNodeConnection();

        // Create bundle with collected commands
        List<RedisCommand<K, V, ?>> commands = collector.drainCommands();
        TransactionBundle<K, V> bundle = watchKeys != null ? new TransactionBundle<>(codec, commands, watchKeys)
                : new TransactionBundle<>(codec, commands);

        // Dispatch to the node connection
        if (nodeConnection instanceof StatefulRedisConnectionImpl) {
            return ((StatefulRedisConnectionImpl<K, V>) nodeConnection).dispatchTransactionBundle(bundle);
        }
        throw new UnsupportedOperationException("Node connection does not support transaction bundles");
    }

    @Override
    public Mono<TransactionResult> executeReactive() {
        return Mono.fromFuture(executeAsync()::toCompletableFuture);
    }

}
