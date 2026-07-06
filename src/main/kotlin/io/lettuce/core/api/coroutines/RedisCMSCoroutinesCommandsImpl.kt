/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.reactive.RedisCMSReactiveCommands
import io.lettuce.core.probabilistic.CMSInfoValue
import io.lettuce.core.probabilistic.IncrementPair
import io.lettuce.core.probabilistic.MergePair
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for Count-Min Sketch commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisCMSCoroutinesCommandsImpl<K : Any, V : Any>(
    internal val ops: RedisCMSReactiveCommands<K, V>
) : RedisCMSCoroutinesCommands<K, V> {

    override suspend fun cmsIncrBy(key: K, pair: IncrementPair<V>): List<Long> =
        ops.cmsIncrBy(key, pair).asFlow().toList()

    override suspend fun cmsIncrBy(key: K, vararg pairs: IncrementPair<V>): List<Long> =
        ops.cmsIncrBy(key, *pairs).asFlow().toList()

    override suspend fun cmsInfo(key: K): CMSInfoValue? =
        ops.cmsInfo(key).awaitFirstOrNull()

    override suspend fun cmsInitByDim(key: K, width: Long, depth: Long): String? =
        ops.cmsInitByDim(key, width, depth).awaitFirstOrNull()

    override suspend fun cmsInitByProb(key: K, error: Double, probability: Double): String? =
        ops.cmsInitByProb(key, error, probability).awaitFirstOrNull()

    override suspend fun cmsMerge(destination: K, source: K): String? =
        ops.cmsMerge(destination, source).awaitFirstOrNull()

    override suspend fun cmsMerge(destination: K, vararg sources: K): String? =
        ops.cmsMerge(destination, *sources).awaitFirstOrNull()

    override suspend fun cmsMerge(destination: K, source: K, weight: Long): String? =
        ops.cmsMerge(destination, source, weight).awaitFirstOrNull()

    override suspend fun cmsMerge(destination: K, vararg sources: MergePair<K>): String? =
        ops.cmsMerge(destination, *sources).awaitFirstOrNull()

    override suspend fun cmsQuery(key: K, vararg values: V): List<Long> =
        ops.cmsQuery(key, *values).asFlow().toList()

}
