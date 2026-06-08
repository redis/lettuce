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
class ClusterCommandCollectingAsyncCommands<K, V> extends AbstractRedisAsyncCommands<K, V>
        implements io.lettuce.core.api.async.RedisAsyncCommands<K, V> {

    private final StatefulRedisClusterConnection<K, V> clusterConnection;

    private final List<RedisCommand<K, V, ?>> collectedCommands = new ArrayList<>();

    private Integer targetSlot = null;

    private ByteBuffer firstKey = null;

    /**
     * Creates a new instance.
     *
     * @param connection the cluster connection
     * @param codec the codec for command encoding
     */
    ClusterCommandCollectingAsyncCommands(StatefulRedisClusterConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
        this.clusterConnection = connection;
    }

    @Override
    public StatefulRedisConnection<K, V> getStatefulConnection() {
        // Return the cluster connection cast to StatefulRedisConnection
        // This is safe because StatefulRedisClusterConnection extends StatefulConnection
        throw new UnsupportedOperationException("getStatefulConnection() is not supported for cluster command collecting");
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

        // Store the original command (unwrap if already wrapped)
        RedisCommand<K, V, ?> commandToStore = cmd;
        if (cmd instanceof AsyncCommand) {
            commandToStore = ((AsyncCommand<K, V, T>) cmd).getDelegate();
        }
        collectedCommands.add(commandToStore);

        // Return an async command (won't be used for actual execution)
        AsyncCommand<K, V, T> asyncCommand = new AsyncCommand<>(cmd);
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
            firstKey = encodedKey.duplicate();
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
