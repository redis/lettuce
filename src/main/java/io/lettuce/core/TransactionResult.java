/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

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
     * @return {@code true} if the transaction batch was discarded.
     * @since 5.1
     */
    boolean wasDiscarded();

    /**
     * @return {@code true} if the transaction batch was discarded.
     * @deprecated use renamed method {@link #wasDiscarded()} as Redis has no notion of rollback.
     */
    @Deprecated
    default boolean wasRolledBack() {
        return wasDiscarded();
    }

    /**
     * Returns the number of elements in this collection. If this {@link TransactionResult} contains more than
     * {@link Integer#MAX_VALUE} elements, returns {@link Integer#MAX_VALUE}.
     *
     * @return the number of elements in this collection.
     */
    int size();

    /**
     * Returns {@code true} if this {@link TransactionResult} contains no elements.
     *
     * @return {@code true} if this {@link TransactionResult} contains no elements.
     */
    boolean isEmpty();

    /**
     * Returns the element at the specified position in this {@link TransactionResult}.
     *
     * @param index index of the element to return.
     * @param <T> inferred type.
     * @return the element at the specified position in this {@link TransactionResult}.
     * @throws IndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    <T> T get(int index);

    /**
     * Returns a sequential {@code Stream} with this {@link TransactionResult} as its source.
     *
     * @return a sequential {@code Stream} over the elements in this {@link TransactionResult}.
     */
    Stream<Object> stream();

}
