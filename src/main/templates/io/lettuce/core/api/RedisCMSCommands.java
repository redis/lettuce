/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api;

import java.util.List;

import io.lettuce.core.probabilistic.CMSInfoValue;
import io.lettuce.core.probabilistic.IncrementPair;
import io.lettuce.core.probabilistic.MergePair;

/**
 * ${intent} for Count-Min Sketch.
 *
 * @author Yordan Tsintsov
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href="https://redis.io/docs/latest/develop/data-types/probabilistic/count-min-sketch/">Redis Count-Min Sketch</a>
 * @since 7.7
 */
public interface RedisCMSCommands<K, V> {

    /**
     * Increases the count of an item by the given increment.
     *
     * @param key the key.
     * @param pair the item paired with the increment to add to its count.
     * @return List&lt;Long&gt; array-reply of the count of the item after the increment.
     */
    List<Long> cmsIncrBy(K key, IncrementPair<V> pair);

    /**
     * Increases the count of several items by their given increments in a single call.
     *
     * @param key the key.
     * @param pairs the items paired with the increment to add to each item's count.
     * @return List&lt;Long&gt; array-reply of the count of each item after the increment, in the same order as {@code pairs}.
     */
    List<Long> cmsIncrBy(K key, IncrementPair<V>... pairs);

    /**
     * Returns width, depth and total count of the sketch.
     *
     * @param key the key.
     * @return the {@link CMSInfoValue} holding the sketch information.
     */
    CMSInfoValue cmsInfo(K key);

    /**
     * Initializes a Count-Min Sketch to the dimensions specified by the user.
     *
     * @param key the key. An error is returned if the key already exists.
     * @param width the number of counters in each array. Reduces the error size.
     * @param depth the number of counter-arrays. Reduces the probability of an error exceeding the estimated size.
     * @return String simple-string-reply {@code OK} if {@code CMS.INITBYDIM} was executed correctly.
     */
    String cmsInitByDim(K key, long width, long depth);

    /**
     * Initializes a Count-Min Sketch to accommodate requested tolerances.
     *
     * @param key the key. An error is returned if the key already exists.
     * @param error estimate size of the error.
     * @param probability the desired probability for inflated count.
     * @return String simple-string-reply {@code OK} if {@code CMS.INITBYPROB} was executed correctly.
     */
    String cmsInitByProb(K key, double error, double probability);

    /**
     * Merges a single source sketch into a destination sketch. All sketches must have identical width and depth, and the
     * destination must already exist.
     *
     * @param destination the name of destination sketch. Must be initialized.
     * @param source the name of the source sketch to merge into {@code destination}.
     * @return String simple-string-reply {@code OK} if {@code CMS.MERGE} was executed correctly.
     */
    String cmsMerge(K destination, K source);

    /**
     * Merges several source sketches into a single destination sketch. All sketches must have identical width and depth, and
     * the destination must already exist.
     *
     * @param destination the name of destination sketch. Must be initialized.
     * @param sources the names of the source sketches to merge into {@code destination}.
     * @return String simple-string-reply {@code OK} if {@code CMS.MERGE} was executed correctly.
     */
    String cmsMerge(K destination, K... sources);

    /**
     * Merges a single source sketch into a destination sketch, scaling its contribution by the given weight. All sketches must
     * have identical width and depth, and the destination must already exist.
     *
     * @param destination the name of destination sketch. Must be initialized.
     * @param source the name of the source sketch to merge into {@code destination}.
     * @param weight the multiplication factor applied to the source sketch before merging.
     * @return String simple-string-reply {@code OK} if {@code CMS.MERGE} was executed correctly.
     */
    String cmsMerge(K destination, K source, long weight);

    /**
     * Merges several source sketches into a single destination sketch, scaling each source's contribution by the weight paired
     * with it. All sketches must have identical width and depth, and the destination must already exist.
     *
     * @param destination the name of destination sketch. Must be initialized.
     * @param sources the source sketches to merge into {@code destination}, each paired with the multiplication factor applied
     *        to it before merging.
     * @return String simple-string-reply {@code OK} if {@code CMS.MERGE} was executed correctly.
     */
    String cmsMerge(K destination, MergePair<K>... sources);

    /**
     * Returns the count for one or more items in a sketch.
     *
     * @param key the key.
     * @param values the items to query.
     * @return List&lt;Long&gt; array-reply of the count of each item, in the same order as {@code values}.
     */
    List<Long> cmsQuery(K key, V... values);

}
