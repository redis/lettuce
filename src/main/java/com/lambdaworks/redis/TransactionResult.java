package com.lambdaworks.redis;

import java.util.List;
import java.util.stream.Stream;

/**
 * Value interface for a {@code MULTI} transaction result. {@link TransactionResult} contains whether the transaction was rolled
 * back (i.e. conditional transaction using {@code WATCH}) and the {@link List} of transaction responses.
 *
 * @author Mark Paluch
 * @since 5.0
 */
public interface TransactionResult extends Iterable<Object> {

    /**
     *
     * @return {@literal true} if the transaction was rolled back
     */
    boolean wasRolledBack();

    /**
     * Returns the number of elements in this collection. If this {@link TransactionResult} contains more than
     * {@link Integer#MAX_VALUE} elements, returns {@link Integer#MAX_VALUE}.
     *
     * @return the number of elements in this collection
     */
    int size();

    /**
     * Returns {@literal true} if this {@link TransactionResult} contains no elements.
     *
     * @return {@literal true} if this {@link TransactionResult} contains no elements
     */
    boolean isEmpty();

    /**
     * Returns the element at the specified position in this {@link TransactionResult}.
     *
     * @param index index of the element to return
     * @param <T> inferred type
     * @return the element at the specified position in this {@link TransactionResult}
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    <T> T get(int index);

    /**
     * Returns a sequential {@code Stream} with this {@link TransactionResult} as its source.
     *
     * @return a sequential {@code Stream} over the elements in this {@link TransactionResult}
     */
    Stream<Object> stream();
}
