/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core;

/**
 * A simple pair class.
 *
 * @param <T1> the type of the first element
 * @param <T2> the type of the second element
 */
public class Pair<T1, T2> {

    private T1 first;

    private T2 second;

    /**
     * Creates a new pair.
     *
     * @param first the first element of the pair
     * @param second the second element of the pair
     */
    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Returns the first element of the pair.
     *
     * @return the first element of the pair
     */
    public T1 getFirst() {
        return first;
    }

    /**
     * Returns the second element of the pair.
     *
     * @return the second element of the pair
     */
    public T2 getSecond() {
        return second;
    }

}
