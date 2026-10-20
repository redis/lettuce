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
import io.lettuce.core.timeseries.TsAggregation
import io.lettuce.core.timeseries.TsInfoValue
import io.lettuce.core.timeseries.TsMAddValue
import io.lettuce.core.timeseries.TsSample
import io.lettuce.core.timeseries.arguments.TsAddArgs
import io.lettuce.core.timeseries.arguments.TsAlterArgs
import io.lettuce.core.timeseries.arguments.TsCreateArgs
import io.lettuce.core.timeseries.arguments.TsIncrByArgs
import io.lettuce.core.timeseries.arguments.TsRangeArgs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
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

    override suspend fun tsCreateRule(sourceKey: K, destKey: K, aggregation: TsAggregation, bucketDuration: Long): String? =
        ops.tsCreateRule(sourceKey, destKey, aggregation, bucketDuration).awaitFirstOrNull()

    override suspend fun tsCreateRule(sourceKey: K, destKey: K, aggregation: TsAggregation, bucketDuration: Long, alignTimestamp: Long): String? =
        ops.tsCreateRule(sourceKey, destKey, aggregation, bucketDuration, alignTimestamp).awaitFirstOrNull()

    override suspend fun tsDeleteRule(sourceKey: K, destKey: K): String? =
        ops.tsDeleteRule(sourceKey, destKey).awaitFirstOrNull()

    override suspend fun tsAdd(key: K, value: Double): Long? =
        ops.tsAdd(key, value).awaitFirstOrNull()

    override suspend fun tsAdd(key: K, value: Double, args: TsAddArgs): Long? =
        ops.tsAdd(key, value, args).awaitFirstOrNull()

    override suspend fun tsAdd(key: K, timestamp: Long, value: Double): Long? =
        ops.tsAdd(key, timestamp, value).awaitFirstOrNull()

    override suspend fun tsAdd(key: K, timestamp: Long, value: Double, args: TsAddArgs): Long? =
        ops.tsAdd(key, timestamp, value, args).awaitFirstOrNull()

    override suspend fun tsMAdd(samples: List<TsMAddValue<K>>): List<Long?> =
        ops.tsMAdd(samples).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun tsIncrBy(key: K, addend: Double): Long? =
        ops.tsIncrBy(key, addend).awaitFirstOrNull()

    override suspend fun tsIncrBy(key: K, addend: Double, args: TsIncrByArgs): Long? =
        ops.tsIncrBy(key, addend, args).awaitFirstOrNull()

    override suspend fun tsDecrBy(key: K, subtrahend: Double): Long? =
        ops.tsDecrBy(key, subtrahend).awaitFirstOrNull()

    override suspend fun tsDecrBy(key: K, subtrahend: Double, args: TsIncrByArgs): Long? =
        ops.tsDecrBy(key, subtrahend, args).awaitFirstOrNull()

    override suspend fun tsGet(key: K): TsSample? =
        ops.tsGet(key).awaitFirstOrNull()

    override suspend fun tsGet(key: K, latest: Boolean): TsSample? =
        ops.tsGet(key, latest).awaitFirstOrNull()

    override suspend fun tsRange(key: K, range: Range<Long>): List<TsSample> =
        ops.tsRange(key, range).asFlow().toList()

    override suspend fun tsRange(key: K, range: Range<Long>, args: TsRangeArgs): List<TsSample> =
        ops.tsRange(key, range, args).asFlow().toList()

    override suspend fun tsRevRange(key: K, range: Range<Long>): List<TsSample> =
        ops.tsRevRange(key, range).asFlow().toList()

    override suspend fun tsRevRange(key: K, range: Range<Long>, args: TsRangeArgs): List<TsSample> =
        ops.tsRevRange(key, range, args).asFlow().toList()

}
