/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.reactive.RedisTimeSeriesReactiveCommands
import io.lettuce.core.timeseries.TsAggregationType
import io.lettuce.core.timeseries.TsInfoValue
import io.lettuce.core.timeseries.TsMGetValue
import io.lettuce.core.timeseries.TsSample
import io.lettuce.core.timeseries.arguments.TsAddArgs
import io.lettuce.core.timeseries.arguments.TsAlterArgs
import io.lettuce.core.timeseries.arguments.TsCreateArgs
import io.lettuce.core.timeseries.arguments.TsGetArgs
import io.lettuce.core.timeseries.arguments.TsIncrByArgs
import io.lettuce.core.timeseries.arguments.TsMGetArgs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for RedisTimeSeries commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Gyumin Hwang
 * @since 7.7
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisTimeSeriesCoroutinesCommandsImpl<K : Any, V : Any>(
    internal val ops: RedisTimeSeriesReactiveCommands<K, V>
) : RedisTimeSeriesCoroutinesCommands<K, V> {

    override suspend fun tsCreate(key: K): String? =
        ops.tsCreate(key).awaitFirstOrNull()

    override suspend fun tsCreate(key: K, createArgs: TsCreateArgs): String? =
        ops.tsCreate(key, createArgs).awaitFirstOrNull()

    override suspend fun tsAlter(key: K, alterArgs: TsAlterArgs): String? =
        ops.tsAlter(key, alterArgs).awaitFirstOrNull()

    override suspend fun tsCreateRule(sourceKey: K, destKey: K, aggregationType: TsAggregationType, bucketDuration: Long): String? =
        ops.tsCreateRule(sourceKey, destKey, aggregationType, bucketDuration).awaitFirstOrNull()

    override suspend fun tsCreateRule(
        sourceKey: K,
        destKey: K,
        aggregationType: TsAggregationType,
        bucketDuration: Long,
        alignTimestamp: Long
    ): String? =
        ops.tsCreateRule(sourceKey, destKey, aggregationType, bucketDuration, alignTimestamp).awaitFirstOrNull()

    override suspend fun tsDeleteRule(sourceKey: K, destKey: K): String? =
        ops.tsDeleteRule(sourceKey, destKey).awaitFirstOrNull()

    override suspend fun tsDel(key: K, fromTimestamp: Long, toTimestamp: Long): Long? =
        ops.tsDel(key, fromTimestamp, toTimestamp).awaitFirstOrNull()

    override suspend fun tsAdd(key: K, timestamp: Long, value: Double): Long? =
        ops.tsAdd(key, timestamp, value).awaitFirstOrNull()

    override suspend fun tsAdd(key: K, timestamp: Long, value: Double, addArgs: TsAddArgs): Long? =
        ops.tsAdd(key, timestamp, value, addArgs).awaitFirstOrNull()

    override suspend fun tsAdd(key: K, value: Double): Long? =
        ops.tsAdd(key, value).awaitFirstOrNull()

    override suspend fun tsMAdd(vararg entries: Map.Entry<K, TsSample>): List<Long> =
        ops.tsMAdd(*entries).asFlow().toList()

    override suspend fun tsMAdd(entry: Map.Entry<K, TsSample>): List<Long> =
        ops.tsMAdd(entry).asFlow().toList()

    override suspend fun tsIncrBy(key: K, value: Double): Long? =
        ops.tsIncrBy(key, value).awaitFirstOrNull()

    override suspend fun tsIncrBy(key: K, value: Double, incrByArgs: TsIncrByArgs): Long? =
        ops.tsIncrBy(key, value, incrByArgs).awaitFirstOrNull()

    override suspend fun tsDecrBy(key: K, value: Double): Long? =
        ops.tsDecrBy(key, value).awaitFirstOrNull()

    override suspend fun tsDecrBy(key: K, value: Double, decrByArgs: TsIncrByArgs): Long? =
        ops.tsDecrBy(key, value, decrByArgs).awaitFirstOrNull()

    override suspend fun tsGet(key: K): TsSample? =
        ops.tsGet(key).awaitFirstOrNull()

    override suspend fun tsGet(key: K, getArgs: TsGetArgs): TsSample? =
        ops.tsGet(key, getArgs).awaitFirstOrNull()

    override suspend fun tsMGet(vararg filters: V): List<TsMGetValue<K>> =
        ops.tsMGet(*filters).asFlow().toList()

    override suspend fun tsMGet(filter: V): List<TsMGetValue<K>> =
        ops.tsMGet(filter).asFlow().toList()

    override suspend fun tsMGet(mGetArgs: TsMGetArgs, vararg filters: V): List<TsMGetValue<K>> =
        ops.tsMGet(mGetArgs, *filters).asFlow().toList()

    override suspend fun tsMGet(mGetArgs: TsMGetArgs, filter: V): List<TsMGetValue<K>> =
        ops.tsMGet(mGetArgs, filter).asFlow().toList()

    override suspend fun tsInfo(key: K): TsInfoValue<K>? =
        ops.tsInfo(key).awaitFirstOrNull()

    override suspend fun tsInfoDebug(key: K): TsInfoValue<K>? =
        ops.tsInfoDebug(key).awaitFirstOrNull()

    override suspend fun tsQueryIndex(vararg filters: V): List<K> =
        ops.tsQueryIndex(*filters).asFlow().toList()

    override suspend fun tsQueryIndex(filter: V): List<K> =
        ops.tsQueryIndex(filter).asFlow().toList()

}
