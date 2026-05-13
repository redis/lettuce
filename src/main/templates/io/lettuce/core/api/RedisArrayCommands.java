/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.api;

import io.lettuce.core.Range;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.array.*;

import java.util.List;
import java.util.Map;

/**
 * ${intent} for the Redis Array data type.
 * <p>
 * Arrays are a Redis data type for sparse, indexed collections that support efficient element access, range operations, pattern
 * matching, and aggregation.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @see <a href="https://redis.io/docs/latest/develop/data-types/arrays/">Redis Arrays</a>
 * @since 7.6
 */
public interface RedisArrayCommands<K, V> {

    /**
     * Set a value at the given index in the array stored at {@code key}.
     *
     * @param key the key of the array.
     * @param index the index to set.
     * @param value the value to store.
     * @return the number of new slots created (1 if new, 0 if updated).
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arset/">Redis Documentation: ARSET</a>
     */
    @Experimental
    Long arset(K key, long index, V value);

    /**
     * Set multiple index-value pairs in the array stored at {@code key}.
     *
     * @param key the key of the array.
     * @param indexValueMap map of index to value pairs.
     * @return the number of new slots created.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/armset/">Redis Documentation: ARMSET</a>
     */
    @Experimental
    Long armset(K key, Map<Long, V> indexValueMap);

    /**
     * Get the value at the given index in the array stored at {@code key}.
     *
     * @param key the key of the array.
     * @param index the index to get.
     * @return the value at the index, or {@code null} if the index is empty or the key does not exist.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arget/">Redis Documentation: ARGET</a>
     */
    @Experimental
    V arget(K key, long index);

    /**
     * Get the values at multiple indices in the array stored at {@code key}.
     *
     * @param key the key of the array.
     * @param indices the indices to get.
     * @return a list of values (with {@code null} for empty slots).
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/armget/">Redis Documentation: ARMGET</a>
     */
    @Experimental
    List<V> armget(K key, long... indices);

    /**
     * Delete a single element at the given index. Returns 1 if the element existed, 0 otherwise.
     * <p>
     * This single-index overload provides boolean-like semantics ("was this element there?"), distinct from the multi-index
     * varargs form which returns a count.
     *
     * @param key the key of the array.
     * @param index the index to delete.
     * @return 1 if the element was deleted, 0 if it was not found.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/ardel/">Redis Documentation: ARDEL</a>
     */
    @Experimental
    Long ardel(K key, long index);

    /**
     * Delete elements at the given indices. Returns the count of elements actually deleted.
     *
     * @param key the key of the array.
     * @param indices the indices to delete.
     * @return the number of elements deleted.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/ardel/">Redis Documentation: ARDEL</a>
     */
    @Experimental
    Long ardel(K key, long... indices);

    /**
     * Delete elements in the given ranges. Each range is a start/end pair (inclusive).
     *
     * @param key the key of the array.
     * @param ranges one or more ranges to delete.
     * @return the number of elements deleted.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/ardelrange/">Redis Documentation: ARDELRANGE</a>
     */
    @Experimental
    @SuppressWarnings("unchecked")
    Long ardelrange(K key, Range<Long>... ranges);

    /**
     * Get the logical length of the array (max index + 1).
     *
     * @param key the key of the array.
     * @return the logical length, or 0 if the key does not exist.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arlen/">Redis Documentation: ARLEN</a>
     */
    @Experimental
    Long arlen(K key);

    /**
     * Get the count of populated (non-empty) slots in the array.
     *
     * @param key the key of the array.
     * @return the count of populated slots, or 0 if the key does not exist.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arcount/">Redis Documentation: ARCOUNT</a>
     */
    @Experimental
    Long arcount(K key);

    /**
     * Get all values in a range, including {@code null} for empty slots.
     * <p>
     * The range must not exceed 1,000,000 items.
     *
     * @param key the key of the array.
     * @param range the range of indices.
     * @return a list of values (with {@code null} for empty slots).
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/argetrange/">Redis Documentation: ARGETRANGE</a>
     */
    @Experimental
    List<V> argetrange(K key, Range<Long> range);

    /**
     * Get the next insert index for the array.
     *
     * @param key the key of the array.
     * @return the next insert index, or 0 if the key does not exist, or {@code null} if the cursor is exhausted.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arnext/">Redis Documentation: ARNEXT</a>
     */
    @Experimental
    Long arnext(K key);

    /**
     * Get the last N items in insertion order.
     *
     * @param key the key of the array.
     * @param count the number of items to return.
     * @return a list of values in insertion order.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arlastitems/">Redis Documentation: ARLASTITEMS</a>
     */
    @Experimental
    List<V> arlastitems(K key, long count);

    /**
     * Get the last N items in reverse insertion order.
     *
     * @param key the key of the array.
     * @param count the number of items to return.
     * @return a list of values in reverse insertion order.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arlastitems/">Redis Documentation: ARLASTITEMS</a>
     */
    @Experimental
    List<V> arlastitemsRev(K key, long count);

    /**
     * Scan populated entries in a range, returning index/value pairs.
     *
     * @param key the key of the array.
     * @param scanArgs the scan arguments (range and optional limit).
     * @return a list of {@link IndexedValue} pairs.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arscan/">Redis Documentation: ARSCAN</a>
     */
    @Experimental
    List<IndexedValue<V>> arscan(K key, ArScanArgs scanArgs);

    /**
     * Search for elements matching predicates, returning matching indices.
     *
     * @param key the key of the array.
     * @param grepArgs the grep arguments (range, predicates, flags).
     * @return a list of matching indices.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/argrep/">Redis Documentation: ARGREP</a>
     */
    @Experimental
    List<Long> argrep(K key, ArGrepArgs grepArgs);

    /**
     * Search for elements matching predicates, returning index/value pairs.
     *
     * @param key the key of the array.
     * @param grepArgs the grep arguments (range, predicates, flags). WITHVALUES is automatically set.
     * @return a list of matching {@link IndexedValue} pairs.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/argrep/">Redis Documentation: ARGREP</a>
     */
    @Experimental
    List<IndexedValue<V>> argrepWithValues(K key, ArGrepArgs grepArgs);

    /**
     * Perform an aggregate operation (SUM, MIN, MAX) over elements in a range.
     *
     * @param key the key of the array.
     * @param range the range of indices.
     * @param operation the aggregate operation.
     * @return the result as a value, or {@code null} if the range is empty or values are non-numeric.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arop/">Redis Documentation: AROP</a>
     */
    @Experimental
    V aropAggregate(K key, Range<Long> range, ArAggregateType operation);

    /**
     * Perform a count/bitwise operation (AND, OR, XOR, USED) over elements in a range.
     *
     * @param key the key of the array.
     * @param range the range of indices.
     * @param operation the count operation.
     * @return the result as a long.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arop/">Redis Documentation: AROP</a>
     */
    @Experimental
    Long aropCount(K key, Range<Long> range, ArCountType operation);

    /**
     * Count occurrences of a value in a range (AROP MATCH).
     *
     * @param key the key of the array.
     * @param range the range of indices.
     * @param matchValue the value to match.
     * @return the count of matching elements.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arop/">Redis Documentation: AROP</a>
     */
    @Experimental
    Long aropMatch(K key, Range<Long> range, V matchValue);

    /**
     * Insert one or more values at the next available index.
     *
     * @param key the key of the array.
     * @param values the values to insert.
     * @return the last index used.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arinsert/">Redis Documentation: ARINSERT</a>
     */
    @Experimental
    @SuppressWarnings("unchecked")
    Long arinsert(K key, V... values);

    /**
     * Insert values in a ring buffer fashion, wrapping around when the size is exceeded.
     *
     * @param key the key of the array.
     * @param size the ring buffer size.
     * @param values the values to insert.
     * @return the last index used.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arring/">Redis Documentation: ARRING</a>
     */
    @Experimental
    @SuppressWarnings("unchecked")
    Long arring(K key, long size, V... values);

    /**
     * Set the insert cursor to a specific index.
     *
     * @param key the key of the array.
     * @param index the index to seek to.
     * @return 1 if the key exists, 0 if it does not.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arseek/">Redis Documentation: ARSEEK</a>
     */
    @Experimental
    Long arseek(K key, long index);

    /**
     * Get metadata about the array (7 top-level fields).
     *
     * @param key the key of the array.
     * @return the array metadata.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arinfo/">Redis Documentation: ARINFO</a>
     */
    @Experimental
    ArrayMetadata arinfo(K key);

    /**
     * Get extended metadata about the array (12 fields including per-slice stats).
     *
     * @param key the key of the array.
     * @return the extended array metadata.
     * @since 7.6
     * @see <a href="https://redis.io/docs/latest/commands/arinfo/">Redis Documentation: ARINFO</a>
     */
    @Experimental
    ArrayFullMetadata arinfoFull(K key);

}
