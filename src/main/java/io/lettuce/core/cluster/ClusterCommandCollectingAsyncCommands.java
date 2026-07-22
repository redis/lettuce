/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.cluster;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.AbstractRedisAsyncCommands;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.RedisCommand;

/**
 * A cluster-aware async commands implementation that collects commands instead of dispatching them.
 * <p>
 * This class validates that all keys in collected commands belong to the same Redis Cluster slot. If keys span multiple slots,
 * a {@link RedisException} is thrown during command collection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 7.6
 */
class ClusterCommandCollectingAsyncCommands<K, V> extends AbstractRedisAsyncCommands<K, V> implements RedisAsyncCommands<K, V> {

    private final List<RedisCommand<K, V, ?>> collectedCommands = new ArrayList<>();

    private Integer targetSlot = null;

    /**
     * Creates a new instance.
     *
     * @param connection the cluster connection
     * @param codec the codec for command encoding
     */
    ClusterCommandCollectingAsyncCommands(StatefulRedisClusterConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        // This facade only collects commands for a cluster transaction; it spans multiple nodes and therefore has no single
        // StatefulRedisConnection to expose (the backing connection is a StatefulRedisClusterConnection, not type-compatible).
        // No command builder invokes this during collection, so it is never reached on the normal path; we fail fast with a
        // clear message rather than returning null (which would defer the failure to a later NPE) if it is called directly.
        throw new UnsupportedOperationException(
                "A cluster transaction collector spans multiple nodes and does not expose a single StatefulRedisConnection; "
                        + "obtain the cluster connection via RedisClusterClient instead.");
    }

    /**
     * Overrides dispatch to collect commands instead of sending them. Also validates that all keys belong to the same slot.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> AsyncCommand<K, V, T> dispatch(RedisCommand<K, V, T> cmd) {
        // Extract and validate key slot
        CommandArgs<K, V> args = cmd.getArgs();
        if (args != null) {
            ByteBuffer encodedKey = args.getFirstEncodedKey();
            if (encodedKey != null) {
                validateSlot(encodedKey);
            }
        }

        // Store and return the same AsyncCommand instance so the TransactionBundle can complete the exact future the caller
        // holds once EXEC arrives (or fail/cancel it on discard/abort). See CommandCollectingAsyncCommands#dispatch.
        AsyncCommand<K, V, T> asyncCommand = (cmd instanceof AsyncCommand) ? (AsyncCommand<K, V, T>) cmd
                : new AsyncCommand<>(cmd);
        collectedCommands.add(asyncCommand);
        return asyncCommand;
    }

    /**
     * Validate that a key maps to the same slot as previous keys.
     *
     * @param encodedKey the encoded key to validate
     * @throws RedisException if the key maps to a different slot
     */
    private void validateSlot(ByteBuffer encodedKey) {
        int slot = SlotHash.getSlot(encodedKey);

        if (targetSlot == null) {
            targetSlot = slot;
        } else if (!targetSlot.equals(slot)) {
            throw new RedisException(String.format("CROSSSLOT Keys in transaction must hash to the same slot. "
                    + "First key hashes to slot %d, but current key hashes to slot %d. "
                    + "Use hash tags like {tag}key to ensure same slot routing.", targetSlot, slot));
        }
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

    /**
     * Returns the target slot determined from the first key.
     *
     * @return the target slot, or null if no keys have been added
     */
    Integer getTargetSlot() {
        return targetSlot;
    }

    /**
     * Sets the initial target slot (e.g., from WATCH keys).
     *
     * @param slot the target slot
     */
    void setTargetSlot(Integer slot) {
        this.targetSlot = slot;
    }

}
