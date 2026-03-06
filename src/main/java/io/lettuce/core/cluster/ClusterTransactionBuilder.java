/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import java.nio.ByteBuffer;

import reactor.core.publisher.Mono;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Cluster-aware implementation of {@link TransactionBuilder}.
 * <p>
 * This builder delegates to an underlying node-specific builder after determining the target slot from WATCH keys. Redis
 * Cluster requires that all keys in a transaction reside on the same slot.
 * <p>
 * <b>Important:</b> When using {@link #commands()}, you are responsible for ensuring all keys map to the same slot. Otherwise,
 * CROSSSLOT errors will occur at execution time. Use Redis hash tags to ensure keys map to the same slot.
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
 *     txn2.execute(); // Throws CROSSSLOT error
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

    private Integer targetSlot;

    private K firstKey;

    private TransactionBuilder<K, V> delegate;

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
        this.targetSlot = null;
        this.firstKey = null;
        this.delegate = null;

        // Validate WATCH keys are all in same slot and determine target slot
        if (this.watchKeys != null) {
            for (K watchKey : this.watchKeys) {
                validateSlot(watchKey);
            }
        }
    }

    /**
     * Validate that a key maps to the same slot as previous keys.
     *
     * @param key the key to validate.
     * @throws RedisException if the key maps to a different slot.
     */
    private void validateSlot(K key) {
        if (key == null) {
            return;
        }

        ByteBuffer encoded = codec.encodeKey(key);
        int slot = SlotHash.getSlot(encoded);

        if (targetSlot == null) {
            targetSlot = slot;
            firstKey = key;
        } else if (!targetSlot.equals(slot)) {
            throw new RedisException(String.format(
                    "CROSSSLOT Keys in transaction must hash to the same slot. " + "Key '%s' hashes to slot %d, "
                            + "but key '%s' hashes to slot %d. " + "Use hash tags like {tag}key to ensure same slot routing.",
                    firstKey, targetSlot, key, slot));
        }
    }

    /**
     * Get or create the delegate builder for the target node.
     *
     * @return the delegate builder.
     */
    private TransactionBuilder<K, V> getDelegate() {
        if (delegate == null) {
            if (targetSlot == null) {
                throw new RedisException(
                        "Cannot determine target slot. Use WATCH keys or ensure commands use keys in the same slot.");
            }
            // Get connection to the target node
            StatefulRedisConnection<K, V> nodeConnection = getNodeConnection();
            if (watchKeys != null) {
                delegate = nodeConnection.transaction(watchKeys);
            } else {
                delegate = nodeConnection.transaction();
            }
        }
        return delegate;
    }

    /**
     * Get the node connection for the target slot.
     *
     * @return the node connection.
     */
    private StatefulRedisConnection<K, V> getNodeConnection() {
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
     * <b>Important:</b> For cluster transactions, you are responsible for ensuring all keys used via this interface map to the
     * same slot. Otherwise, CROSSSLOT errors will occur at execution time. Use Redis hash tags like {@code {tag}key} to ensure
     * same slot routing.
     *
     * @return async commands interface that collects commands for the transaction
     */
    @Override
    public io.lettuce.core.api.async.RedisAsyncCommands<K, V> commands() {
        return getDelegate().commands();
    }

    @Override
    public TransactionBuilder<K, V> addCommand(RawCommand<K, V> command) {
        getDelegate().addCommand(command);
        return this;
    }

    @Override
    public int size() {
        return delegate == null ? 0 : delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate == null || delegate.isEmpty();
    }

    @Override
    public TransactionResult execute() {
        if (delegate == null) {
            throw new RedisException("No commands added to the transaction");
        }
        return delegate.execute();
    }

    @Override
    public RedisFuture<TransactionResult> executeAsync() {
        if (delegate == null) {
            throw new RedisException("No commands added to the transaction");
        }
        return delegate.executeAsync();
    }

    @Override
    public Mono<TransactionResult> executeReactive() {
        if (delegate == null) {
            throw new RedisException("No commands added to the transaction");
        }
        return delegate.executeReactive();
    }

}
