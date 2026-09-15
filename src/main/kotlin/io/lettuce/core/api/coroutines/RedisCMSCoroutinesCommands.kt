/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */

package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import io.lettuce.core.probabilistic.CMSInfoValue
import io.lettuce.core.probabilistic.IncrementPair
import io.lettuce.core.probabilistic.MergePair

/**
 * Coroutine executed commands for Count-Min Sketch.
 *
 * @author Yordan Tsintsov
 * @param <K> Key type.
 * @param <V> Value type.
 * @see <a href="https://redis.io/docs/latest/develop/data-types/probabilistic/count-min-sketch/">Redis Count-Min Sketch</a>
 * @since 7.7
 */
@ExperimentalLettuceCoroutinesApi
interface RedisCMSCoroutinesCommands<K : Any, V : Any> {

    /**
     * Increases the count of an item by the given increment. A negative increment decrements the count; the server
     * applies it only if every counter cell of the item holds at least that amount, otherwise the command fails with a
     * `CMS: INCRBY underflow` error and the sketch is left unchanged. Negative increments require Redis 8.12 or later.
     *
     * @param key the key.
     * @param pair the item paired with the increment to add to its count.
     * @return List<Long> array-reply of the count of the item after the increment.
     */
    suspend fun cmsIncrBy(key: K, pair: IncrementPair<V>): List<Long>

    /**
     * Increases the count of several items by their given increments in a single call. A negative increment decrements the
     * count of its item; the server applies it only if every counter cell of the item holds at least that amount, otherwise
     * the command fails with a `CMS: INCRBY underflow` error. Items preceding the failing pair are still applied.
     * Negative increments require Redis 8.12 or later.
     *
     * @param key the key.
     * @param pairs the items paired with the increment to add to each item's count.
     * @return List<Long> array-reply of the count of each item after the increment, in the same order as `pairs`.
     */
    suspend fun cmsIncrBy(key: K, vararg pairs: IncrementPair<V>): List<Long>

    /**
     * Returns width, depth, total count and cell size of the sketch. The cell size is reported by Redis 8.12 and later;
     * [CMSInfoValue.getCellSize] is `null` on earlier servers.
     *
     * @param key the key.
     * @return the [CMSInfoValue] holding the sketch information.
     */
    suspend fun cmsInfo(key: K): CMSInfoValue?

    /**
     * Initializes a Count-Min Sketch to the dimensions specified by the user.
     *
     * @param key the key. An error is returned if the key already exists.
     * @param width the number of counters in each array. Reduces the error size.
     * @param depth the number of counter-arrays. Reduces the probability of an error exceeding the estimated size.
     * @return String simple-string-reply `OK` if `CMS.INITBYDIM` was executed correctly.
     */
    suspend fun cmsInitByDim(key: K, width: Long, depth: Long): String?

    /**
     * Initializes a Count-Min Sketch to the dimensions specified by the user and the number of bytes per counter cell.
     *
     * @param key the key. An error is returned if the key already exists.
     * @param width the number of counters in each array. Reduces the error size.
     * @param depth the number of counter-arrays. Reduces the probability of an error exceeding the estimated size.
     * @param cellSize the number of bytes per counter cell (`CELL_SIZE`), must be `1`, `2`, `4` or
     *        `8`. Smaller cells reduce the memory footprint but lower the maximum count a cell can hold (`255` for
     *        1-byte cells, `65535` for 2-byte cells, and so on). The server default is `4`.
     * @return String simple-string-reply `OK` if `CMS.INITBYDIM` was executed correctly.
     * @throws IllegalArgumentException if `cellSize` is not `1`, `2`, `4` or `8`.
     * @since 7.8
     */
    suspend fun cmsInitByDim(key: K, width: Long, depth: Long, cellSize: Int): String?

    /**
     * Initializes a Count-Min Sketch to accommodate requested tolerances.
     *
     * @param key the key. An error is returned if the key already exists.
     * @param error estimate size of the error.
     * @param probability the desired probability for inflated count.
     * @return String simple-string-reply `OK` if `CMS.INITBYPROB` was executed correctly.
     */
    suspend fun cmsInitByProb(key: K, error: Double, probability: Double): String?

    /**
     * Initializes a Count-Min Sketch to accommodate requested tolerances and the number of bytes per counter cell.
     *
     * @param key the key. An error is returned if the key already exists.
     * @param error estimate size of the error.
     * @param probability the desired probability for inflated count.
     * @param cellSize the number of bytes per counter cell (`CELL_SIZE`), must be `1`, `2`, `4` or
     *        `8`. Smaller cells reduce the memory footprint but lower the maximum count a cell can hold (`255` for
     *        1-byte cells, `65535` for 2-byte cells, and so on). The server default is `4`.
     * @return String simple-string-reply `OK` if `CMS.INITBYPROB` was executed correctly.
     * @throws IllegalArgumentException if `cellSize` is not `1`, `2`, `4` or `8`.
     * @since 7.8
     */
    suspend fun cmsInitByProb(key: K, error: Double, probability: Double, cellSize: Int): String?

    /**
     * Merges a single source sketch into a destination sketch. All sketches must have identical width and depth, and the
     * destination must already exist.
     *
     * @param destination the name of destination sketch. Must be initialized.
     * @param source the name of the source sketch to merge into `destination`.
     * @return String simple-string-reply `OK` if `CMS.MERGE` was executed correctly.
     */
    suspend fun cmsMerge(destination: K, source: K): String?

    /**
     * Merges several source sketches into a single destination sketch. All sketches must have identical width and depth, and
     * the destination must already exist.
     *
     * @param destination the name of destination sketch. Must be initialized.
     * @param sources the names of the source sketches to merge into `destination`.
     * @return String simple-string-reply `OK` if `CMS.MERGE` was executed correctly.
     */
    suspend fun cmsMerge(destination: K, vararg sources: K): String?

    /**
     * Merges a single source sketch into a destination sketch, scaling its contribution by the weight paired with it. All
     * sketches must have identical width and depth, and the destination must already exist.
     *
     * @param destination the name of destination sketch. Must be initialized.
     * @param pair the source sketch to merge into `destination`, paired with the multiplication factor applied to it
     *        before merging.
     * @return String simple-string-reply `OK` if `CMS.MERGE` was executed correctly.
     */
    suspend fun cmsMerge(destination: K, pair: MergePair<K>): String?

    /**
     * Merges several source sketches into a single destination sketch, scaling each source's contribution by the weight paired
     * with it. All sketches must have identical width and depth, and the destination must already exist.
     *
     * @param destination the name of destination sketch. Must be initialized.
     * @param sources the source sketches to merge into `destination`, each paired with the multiplication factor applied
     *        to it before merging.
     * @return String simple-string-reply `OK` if `CMS.MERGE` was executed correctly.
     */
    suspend fun cmsMerge(destination: K, vararg sources: MergePair<K>): String?

    /**
     * Returns the count for one item in a sketch.
     *
     * @param key the key.
     * @param value the item to query.
     * @return List<Long> array-reply of the count of the item.
     */
    suspend fun cmsQuery(key: K, value: V): List<Long>

    /**
     * Returns the count for one or more items in a sketch.
     *
     * @param key the key.
     * @param values the items to query.
     * @return List<Long> array-reply of the count of each item, in the same order as `values`.
     */
    suspend fun cmsQuery(key: K, vararg values: V): List<Long>

}

