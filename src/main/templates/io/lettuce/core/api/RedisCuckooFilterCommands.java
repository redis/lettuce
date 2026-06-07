/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

import java.util.List;

import io.lettuce.core.cf.CfInfoValue;
import io.lettuce.core.cf.CfScanDumpValue;
import io.lettuce.core.cf.arguments.CfInsertArgs;
import io.lettuce.core.cf.arguments.CfReserveArgs;

/**
 * ${intent} for Cuckoo Filter.
 *
 * @author Yordan Tsintsov
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href="https://redis.io/docs/latest/develop/data-types/probabilistic/cuckoo-filter/">Redis Cuckoo Filter</a>
 * @since 7.7
 */
public interface RedisCuckooFilterCommands<K, V> {

    /**
     * Creates an empty Cuckoo filter with a single sub-filter for the initial specified capacity.
     *
     * @param key the key.
     * @param capacity the capacity.
     * @return String simple-string-reply {@code OK} if {@code CF.RESERVE} was executed correctly.
     */
    String cfReserve(K key, long capacity);

    /**
     * Creates an empty Cuckoo filter with a single sub-filter for the initial specified capacity.
     *
     * @param key the key.
     * @param capacity the capacity.
     * @param args the reserve arguments.
     * @return String simple-string-reply {@code OK} if {@code CF.RESERVE} was executed correctly.
     */
    String cfReserve(K key, long capacity, CfReserveArgs args);

    /**
     * Add an item to the Cuckoo Filter. A filter will be created if one does not exist.
     *
     * @param key the key.
     * @param value the value.
     * @return Boolean integer-reply {@code true} if the item was added, {@code false} if it was already in the filter.
     * @throws io.lettuce.core.RedisCommandExecutionException if the filter is full and cannot expand.
     */
    Boolean cfAdd(K key, V value);

    /**
     * Add an item to the Cuckoo Filter only if the item does not already exist. A filter will be created if one does not exist.
     *
     * @param key the key.
     * @param value the value.
     * @return Boolean integer-reply {@code true} if the item was added, {@code false} if it was already in the filter.
     */
    Boolean cfAddNx(K key, V value);

    /**
     * Add one or more items to a Cuckoo Filter. A filter will be created if one does not exist.
     *
     * @param key the key.
     * @param values the values.
     * @return List&lt;Boolean&gt; one entry per item: {@code true} if the item was added, {@code null} if the item could not be
     *         added because the filter is full (server reply {@code -1}). CF.INSERT does not report already-existing items.
     *         <p>
     *         Note: over RESP3 a full-filter result may surface as {@code false}/{@code Value.just(false)} instead of
     *         {@code null}/{@code Value.empty()}, as the server encodes -1 as a boolean.
     *         </p>
     */
    List<Boolean> cfInsert(K key, V... values);

    /**
     * Add one or more items to a Cuckoo Filter. A filter will be created if one does not exist.
     *
     * @param key the key.
     * @param args the insert arguments.
     * @param values the values.
     * @return List&lt;Boolean&gt; one entry per item: {@code true} if the item was added, {@code null} if the item could not be
     *         added because the filter is full (server reply {@code -1}). CF.INSERT does not report already-existing items.
     *         <p>
     *         Note: over RESP3 a full-filter result may surface as {@code false}/{@code Value.just(false)} instead of
     *         {@code null}/{@code Value.empty()}, as the server encodes -1 as a boolean.
     *         </p>
     */
    List<Boolean> cfInsert(K key, CfInsertArgs args, V... values);

    /**
     * Add one or more items to a Cuckoo Filter only if the items do not already exist. A filter will be created if one does not
     * exist.
     *
     * @param key the key.
     * @param values the values.
     * @return List&lt;Boolean&gt; one entry per item: {@code true} if the item was added, {@code false} if the item already
     *         exists, {@code null} if the item could not be added because the filter is full (server reply {@code -1}).
     *         <p>
     *         Note: over RESP3 a full-filter result may surface as {@code false}/{@code Value.just(false)} instead of
     *         {@code null}/{@code Value.empty()}, as the server encodes -1 as a boolean.
     *         </p>
     */
    List<Boolean> cfInsertNx(K key, V... values);

    /**
     * Add one or more items to a Cuckoo Filter only if the items do not already exist. A filter will be created if one does not
     * exist.
     *
     * @param key the key.
     * @param args the insert arguments.
     * @param values the values.
     * @return List&lt;Boolean&gt; one entry per item: {@code true} if the item was added, {@code false} if the item already
     *         exists, {@code null} if the item could not be added because the filter is full (server reply {@code -1}).
     *         <p>
     *         Note: over RESP3 a full-filter result may surface as {@code false}/{@code Value.just(false)} instead of
     *         {@code null}/{@code Value.empty()}, as the server encodes -1 as a boolean.
     *         </p>
     */
    List<Boolean> cfInsertNx(K key, CfInsertArgs args, V... values);

    /**
     * Check if an item exists in the Cuckoo Filter.
     *
     * @param key the key.
     * @param value the value.
     * @return Boolean integer-reply {@code true} if the item may exist in the filter, {@code false} if it definitely does not.
     */
    Boolean cfExists(K key, V value);

    /**
     * Check if one or more items exist in the Cuckoo Filter.
     *
     * @param key the key.
     * @param values the values.
     * @return List&lt;Boolean&gt; of {@code true} or {@code false} where {@code true} means that, with high probability, the
     *         item was already added to the filter, and {@code false} means that the item was definitely not added to the
     *         filter.
     */
    List<Boolean> cfMExists(K key, V... values);

    /**
     * Delete an item from the Cuckoo Filter.
     *
     * <p>
     * <b>Warning:</b> Deleting an item that was not previously added to the filter may cause false negatives for items that are
     * actually present. Only delete items that have been previously added.
     * </p>
     *
     * @param key the key.
     * @param value the value.
     * @return Boolean integer-reply {@code true} if the item was deleted, {@code false} if the item was not found.
     */
    Boolean cfDel(K key, V value);

    /**
     * Return the number of times an item may be in the Cuckoo Filter.
     *
     * @param key the key.
     * @param value the value.
     * @return Long integer-reply the number of times the item may be in the filter.
     */
    Long cfCount(K key, V value);

    /**
     * Begins an incremental save of the Cuckoo filter.
     *
     * @param key the key.
     * @param cursor the cursor.
     * @return CfScanDumpValue the scan dump value.
     */
    CfScanDumpValue cfScanDump(K key, long cursor);

    /**
     * Restores a Cuckoo filter previously saved using CF.SCANDUMP.
     *
     * @param key the key.
     * @param cursor the cursor.
     * @param data the data.
     * @return String simple-string-reply {@code OK} if {@code CF.LOADCHUNK} was executed correctly.
     */
    String cfLoadChunk(K key, long cursor, byte[] data);

    /**
     * Get information about the Cuckoo Filter.
     *
     * @param key the key.
     * @return CfInfoValue the information about the filter.
     */
    CfInfoValue cfInfo(K key);

}
