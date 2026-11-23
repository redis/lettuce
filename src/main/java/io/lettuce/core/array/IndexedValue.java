/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.array;

import java.util.Objects;

import io.lettuce.core.annotations.Experimental;

/**
 * Represents an indexed value in a Redis array, pairing a long index with a value.
 * <p>
 * This class is used to represent results from commands like {@code ARSCAN} and {@code ARGREP WITHVALUES}, which return
 * alternating index/value pairs.
 *
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.6
 * @see <a href="https://redis.io/docs/latest/commands/arscan/">Redis Documentation: ARSCAN</a>
 * @see <a href="https://redis.io/docs/latest/commands/argrep/">Redis Documentation: ARGREP</a>
 */
@Experimental
public class IndexedValue<V> {

    private final long index;

    private final V value;

    /**
     * Creates a new {@link IndexedValue}.
     *
     * @param index the array index.
     * @param value the value at that index.
     */
    public IndexedValue(long index, V value) {
        this.index = index;
        this.value = value;
    }

    /**
     * Gets the array index.
     *
     * @return the index.
     */
    public long getIndex() {
        return index;
    }

    /**
     * Gets the value at the index.
     *
     * @return the value, may be {@code null}.
     */
    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IndexedValue<?> that = (IndexedValue<?>) o;
        return index == that.index && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, value);
    }

    @Override
    public String toString() {
        return "IndexedValue{" + "index=" + index + ", value=" + value + '}';
    }

}
