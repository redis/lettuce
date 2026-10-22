/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.Range
import io.lettuce.core.api.reactive.RedisTimeSeriesReactiveCommands
import io.lettuce.core.timeseries.TsInfoValue
import io.lettuce.core.timeseries.arguments.TsAlterArgs
import io.lettuce.core.timeseries.arguments.TsCreateArgs
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for Redis TimeSeries.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisTimeSeriesCoroutinesCommandsImpl<K : Any, V : Any>(
    internal val ops: RedisTimeSeriesReactiveCommands<K, V>
) : RedisTimeSeriesCoroutinesCommands<K, V> {

    override suspend fun tsCreate(key: K): String? =
        ops.tsCreate(key).awaitFirstOrNull()

    override suspend fun tsCreate(key: K, args: TsCreateArgs): String? =
        ops.tsCreate(key, args).awaitFirstOrNull()

    override suspend fun tsAlter(key: K, args: TsAlterArgs): String? =
        ops.tsAlter(key, args).awaitFirstOrNull()

    override suspend fun tsDel(key: K, range: Range<Long>): Long? =
        ops.tsDel(key, range).awaitFirstOrNull()

    override suspend fun tsInfo(key: K): TsInfoValue<K>? =
        ops.tsInfo(key).awaitFirstOrNull()

    override suspend fun tsInfo(key: K, debug: Boolean): TsInfoValue<K>? =
        ops.tsInfo(key, debug).awaitFirstOrNull()

}
