/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.Pair
import io.lettuce.core.api.reactive.RedisTopKReactiveCommands
import io.lettuce.core.probabilistic.topk.TopKInfoValue
import io.lettuce.core.probabilistic.topk.TopKListValue
import io.lettuce.core.probabilistic.topk.arguments.TopKReserveArgs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for basic commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.7
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisTopKCoroutinesCommandsImpl<K : Any, V : Any>(
    internal val ops: RedisTopKReactiveCommands<K, V>
) : RedisTopKCoroutinesCommands<K, V> {

    override suspend fun topKAdd(key: K, value: V): List<String?> =
        ops.topKAdd(key, value).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun topKAdd(key: K, vararg values: V): List<String> =
        ops.topKAdd(key, *values).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun topKIncrBy(
        key: K,
        pair: Pair<V, Long>
    ): List<String?> =
        ops.topKIncrBy(key, pair).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun topKIncrBy(
        key: K,
        vararg pairs: Pair<V, Long>
    ): List<String> =
        ops.topKIncrBy(key, *pairs).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun topKInfo(key: K): TopKInfoValue? =
        ops.topKInfo(key).awaitFirstOrNull()

    override suspend fun topKList(key: K): List<String> =
        ops.topKList(key).asFlow().toList()

    override suspend fun topKList(
        key: K,
        withCount: Boolean
    ): List<TopKListValue> =
        ops.topKList(key, withCount).asFlow().toList()

    override suspend fun topKQuery(key: K, value: V): List<Boolean> =
        ops.topKQuery(key, value).asFlow().toList()

    override suspend fun topKQuery(key: K, vararg values: V): List<Boolean> =
        ops.topKQuery(key, *values).asFlow().toList()

    override suspend fun topKReserve(key: K, k: Long): String? =
        ops.topKReserve(key, k).awaitFirstOrNull()

    override suspend fun topKReserve(
        key: K,
        k: Long,
        args: TopKReserveArgs
    ): String? =
        ops.topKReserve(key, k, args).awaitFirstOrNull()

}
