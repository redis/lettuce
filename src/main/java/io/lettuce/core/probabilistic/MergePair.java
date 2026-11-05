/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.probabilistic;

import java.util.Objects;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A key paired with a weight for use with {@code CMS.MERGE}.
 *
 * @param <K> the key type
 * @author Yordan Tsintsov
 * @since 7.7
 */
public class MergePair<K> {

    private final K key;

    private final long weight;

    /**
     * Creates a new {@link MergePair}.
     *
     * @param key the key, must not be {@code null}.
     * @param weight the weight.
     */
    public MergePair(K key, long weight) {
        LettuceAssert.notNull(key, "Key must not be null");
        this.key = key;
        this.weight = weight;
    }

    /**
     * Creates a new {@link MergePair}.
     *
     * @param key the key, must not be {@code null}.
     * @param weight the weight.
     * @param <K> the key type.
     * @return a new {@link MergePair}.
     */
    public static <K> MergePair<K> of(K key, long weight) {
        return new MergePair<>(key, weight);
    }

    /**
     * Returns the key.
     *
     * @return the key.
     */
    public K getKey() {
        return key;
    }

    /**
     * Returns the weight.
     *
     * @return the weight.
     */
    public long getWeight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MergePair<?> that = (MergePair<?>) o;
        return weight == that.weight && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, weight);
    }

    @Override
    public String toString() {
        return "MergePair{" + key + " -> " + weight + '}';
    }

}
