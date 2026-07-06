/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

import java.util.List;

import io.lettuce.core.probabilistic.IncrementPair;
import io.lettuce.core.probabilistic.TopKInfoValue;
import io.lettuce.core.probabilistic.TopKListValue;
import io.lettuce.core.probabilistic.arguments.TopKReserveArgs;

/**
 * ${intent} for Top-K.
 *
 * @author Yordan Tsintsov
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href="https://redis.io/docs/latest/develop/data-types/probabilistic/top-k/">Redis Top-K</a>
 * @since 7.7
 */
public interface RedisTopKCommands<K, V> {

    /**
     * Adds a {@code value} to a Top-k sketch. If a {@code value} enters the Top-K sketch, the value that is expelled (if any)
     * is returned. This allows dynamic heavy-hitter detection of values being entered or expelled from Top-K sketch.
     *
     * @param key the key.
     * @param value the value.
     * @return List&lt;String&gt; where each element is the value expelled from the Top-K sketch when the corresponding value
     *         entered it, or {@code null} if no value was expelled.
     */
    List<String> topKAdd(K key, V value);

    /**
     * Adds a {@code value} to a Top-k sketch. Multiple values can be added at the same time. If a {@code value} enters the
     * Top-K sketch, the value that is expelled (if any) is returned. This allows dynamic heavy-hitter detection of values being
     * entered or expelled from Top-K sketch.
     *
     * @param key the key.
     * @param values the values.
     * @return List&lt;String&gt; where each element is the value expelled from the Top-K sketch when the corresponding value
     *         entered it, or {@code null} if no value was expelled.
     */
    List<String> topKAdd(K key, V... values);

    /**
     * Increments the count of a {@code value} in a Top-K sketch by the given {@code increment}. If the {@code value} enters the
     * Top-K sketch, the value that is expelled (if any) is returned.
     *
     * @param key the key.
     * @param value the value.
     * @param increment the increment.
     * @return List&lt;String&gt; where each element is the value expelled from the Top-K sketch when the corresponding value
     *         entered it, or {@code null} if no value was expelled.
     */
    List<String> topKIncrBy(K key, V value, long increment);

    /**
     * Adds a {@code value} to a Top-k sketch. Multiple values can be added at the same time. If a {@code value} enters the
     * Top-K sketch, the value that is expelled (if any) is returned. This allows dynamic heavy-hitter detection of values being
     * entered or expelled from Top-K sketch.
     *
     * @param key the key.
     * @param pairs the value and increment pairs.
     * @return List&lt;String&gt; where each element is the value expelled from the Top-K sketch when the corresponding value
     *         entered it, or {@code null} if no value was expelled.
     */
    List<String> topKIncrBy(K key, IncrementPair<V>... pairs);

    /**
     * Returns information about the Top-K sketch.
     *
     * @param key the key.
     * @return {@link TopKInfoValue} the information about the sketch.
     */
    TopKInfoValue topKInfo(K key);

    /**
     * Returns the full list of values in Top-K sketch.
     *
     * @param key the key.
     * @return List&lt;String&gt; containing the current top-k values in the sketch.
     */
    List<String> topKList(K key);

    /**
     * Returns the full list of values in Top-K sketch paired with their count if {@code withCount} is true.
     *
     * @param key the key.
     * @param withCount whether to return the count of each value.
     * @return List&lt;TopKListValue&gt; containing the current top-k values in the sketch paired with their count.
     */
    List<TopKListValue> topKList(K key, boolean withCount);

    /**
     * Checks whether {@code value} is one of the top-k elements of the sketch.
     *
     * @param key the key.
     * @param value the value.
     * @return List&lt;Boolean&gt; where {@code true} means the corresponding value is one of the top-k elements of the sketch,
     *         {@code false} otherwise.
     */
    List<Boolean> topKQuery(K key, V value);

    /**
     * Checks whether {@code values} are in the Top-K sketch.
     *
     * @param key the key.
     * @param values the values.
     * @return List&lt;Boolean&gt; where {@code true} means the corresponding value is one of the top-k elements of the sketch,
     *         {@code false} otherwise.
     */
    List<Boolean> topKQuery(K key, V... values);

    /**
     * Initializes a Top-K sketch with specified parameters.
     *
     * @param key the key.
     * @param k the maximum number of values to keep.
     * @return String simple-string-reply {@code OK} if the sketch was created.
     */
    String topKReserve(K key, long k);

    /**
     * Initializes a Top-K sketch with specified parameters.
     *
     * @param key the key.
     * @param k the maximum number of values to keep.
     * @param args the reserve arguments.
     * @return String simple-string-reply {@code OK} if the sketch was created.
     */
    String topKReserve(K key, long k, TopKReserveArgs args);

}
