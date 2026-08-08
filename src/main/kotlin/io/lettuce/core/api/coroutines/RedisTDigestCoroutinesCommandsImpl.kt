/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.reactive.RedisTDigestReactiveCommands
import io.lettuce.core.probabilistic.TDigestInfoValue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for T-Digest sketch.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisTDigestCoroutinesCommandsImpl<K : Any, V : Any>(
    internal val ops: RedisTDigestReactiveCommands<K, V>
) : RedisTDigestCoroutinesCommands<K, V> {

    override suspend fun tdigestAdd(key: K, value: V): String? =
        ops.tdigestAdd(key, value).awaitFirstOrNull()

    override suspend fun tdigestAdd(key: K, vararg values: V): String? =
        ops.tdigestAdd(key, *values).awaitFirstOrNull()

    override suspend fun tdigestByRank(key: K, rank: Long): List<Double> =
        ops.tdigestByRank(key, rank).asFlow().toList()

    override suspend fun tdigestByRank(key: K, vararg ranks: Long): List<Double> =
        ops.tdigestByRank(key, *ranks).asFlow().toList()

    override suspend fun tdigestByRevRank(key: K, reverseRank: Long): List<Double> =
        ops.tdigestByRevRank(key, reverseRank).asFlow().toList()

    override suspend fun tdigestByRevRank(key: K, vararg reverseRanks: Long): List<Double> =
        ops.tdigestByRevRank(key, *reverseRanks).asFlow().toList()

    override suspend fun tdigestCDF(key: K, value: V): List<Double> =
        ops.tdigestCDF(key, value).asFlow().toList()

    override suspend fun tdigestCDF(key: K, vararg values: V): List<Double> =
        ops.tdigestCDF(key, *values).asFlow().toList()

    override suspend fun tdigestCreate(key: K): String? =
        ops.tdigestCreate(key).awaitFirstOrNull()

    override suspend fun tdigestCreate(key: K, compression: Long): String? =
        ops.tdigestCreate(key, compression).awaitFirstOrNull()

    override suspend fun tdigestInfo(key: K): TDigestInfoValue? =
        ops.tdigestInfo(key).awaitFirstOrNull()

    override suspend fun tdigestMax(key: K): Double? =
        ops.tdigestMax(key).awaitFirstOrNull()

    override suspend fun tdigestMerge(destination: K, sourceKey: K): String? =
        ops.tdigestMerge(destination, sourceKey).awaitFirstOrNull()

    override suspend fun tdigestMerge(destination: K, sourceKey: K, compression: Long): String? =
        ops.tdigestMerge(destination, sourceKey, compression).awaitFirstOrNull()

    override suspend fun tdigestMerge(destination: K, sourceKey: K, compression: Long, override: Boolean): String? =
        ops.tdigestMerge(destination, sourceKey, compression, override).awaitFirstOrNull()

    override suspend fun tdigestMerge(destination: K, vararg sourceKeys: K): String? =
        ops.tdigestMerge(destination, *sourceKeys).awaitFirstOrNull()

    override suspend fun tdigestMerge(destination: K, compression: Long, vararg sourceKeys: K): String? =
        ops.tdigestMerge(destination, compression, *sourceKeys).awaitFirstOrNull()

    override suspend fun tdigestMerge(
        destination: K,
        compression: Long,
        override: Boolean,
        vararg sourceKeys: K
    ): String? =
        ops.tdigestMerge(destination, compression, override, *sourceKeys).awaitFirstOrNull()

    override suspend fun tdigestMin(key: K): Double? =
        ops.tdigestMin(key).awaitFirstOrNull()

    override suspend fun tdigestQuantile(key: K, quantile: Double): List<Double> =
        ops.tdigestQuantile(key, quantile).asFlow().toList()

    override suspend fun tdigestQuantile(key: K, vararg quantiles: Double): List<Double> =
        ops.tdigestQuantile(key, *quantiles).asFlow().toList()

    override suspend fun tdigestRank(key: K, value: V): List<Long> =
        ops.tdigestRank(key, value).asFlow().toList()

    override suspend fun tdigestRank(key: K, vararg values: V): List<Long> =
        ops.tdigestRank(key, *values).asFlow().toList()

    override suspend fun tdigestReset(key: K): String? =
        ops.tdigestReset(key).awaitFirstOrNull()

    override suspend fun tdigestRevRank(key: K, value: V): List<Long> =
        ops.tdigestRevRank(key, value).asFlow().toList()

    override suspend fun tdigestRevRank(key: K, vararg values: V): List<Long> =
        ops.tdigestRevRank(key, *values).asFlow().toList()

    override suspend fun tdigestTrimmedMean(key: K, lowCutQuantile: Double, highCutQuantile: Double): Double? =
        ops.tdigestTrimmedMean(key, lowCutQuantile, highCutQuantile).awaitFirstOrNull()

}
