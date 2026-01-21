/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.internal;

/**
 * Simple immutable pair of two values.
 *
 * @param <T1> type of first value
 * @param <T2> type of second value
 * @author Mark Paluch
 * @since 6.7
 */
public class Pair<T1, T2> {

    private final T1 first;

    private final T2 second;

    private Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Create a new {@link Pair}.
     *
     * @param first the first value
     * @param second the second value
     * @param <T1> type of first value
     * @param <T2> type of second value
     * @return the {@link Pair}
     */
    public static <T1, T2> Pair<T1, T2> of(T1 first, T2 second) {
        return new Pair<>(first, second);
    }

    /**
     * @return the first value
     */
    public T1 getFirst() {
        return first;
    }

    /**
     * @return the second value
     */
    public T2 getSecond() {
        return second;
    }

}
