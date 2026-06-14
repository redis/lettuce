/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.reactive.RedisBloomFilterReactiveCommands
import io.lettuce.core.probabilistic.BfInfoValue
import io.lettuce.core.probabilistic.ScanDumpValue
import io.lettuce.core.probabilistic.arguments.BfInsertArgs
import io.lettuce.core.probabilistic.arguments.BfReserveArgs
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
internal class RedisBloomFilterCoroutinesCommandsImpl<K : Any, V : Any>(
    internal val ops: RedisBloomFilterReactiveCommands<K, V>
) : RedisBloomFilterCoroutinesCommands<K, V> {

    override suspend fun bfAdd(key: K, value: V): Boolean? =
        ops.bfAdd(key, value).awaitFirstOrNull()

    override suspend fun bfCard(key: K): Long? =
        ops.bfCard(key).awaitFirstOrNull()

    override suspend fun bfExists(key: K, value: V): Boolean? =
        ops.bfExists(key, value).awaitFirstOrNull()

    override suspend fun bfInfo(key: K): BfInfoValue? =
        ops.bfInfo(key).awaitFirstOrNull()

    override suspend fun bfInsert(key: K, value: V): List<Boolean> =
        ops.bfInsert(key, value).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun bfInsert(key: K, insertArgs: BfInsertArgs, value: V): List<Boolean> =
        ops.bfInsert(key, insertArgs, value).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun bfInsert(key: K, vararg values: V): List<Boolean> =
        ops.bfInsert(key, *values).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun bfInsert(
        key: K,
        insertArgs: BfInsertArgs,
        vararg values: V
    ): List<Boolean> =
        ops.bfInsert(key, insertArgs, *values).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun bfLoadChunk(key: K, iterator: Long, data: ByteArray): String? =
        ops.bfLoadChunk(key, iterator, data).awaitFirstOrNull()

    override suspend fun bfMAdd(key: K, vararg values: V): List<Boolean> =
        ops.bfMAdd(key, *values).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun bfMExists(key: K, vararg values: V): List<Boolean> =
        ops.bfMExists(key, *values).asFlow().toList()

    override suspend fun bfReserve(key: K, errorRate: Double, capacity: Long): String? =
        ops.bfReserve(key, errorRate, capacity).awaitFirstOrNull()

    override suspend fun bfReserve(
        key: K,
        errorRate: Double,
        capacity: Long,
        reserveArgs: BfReserveArgs
    ): String? =
        ops.bfReserve(key, errorRate, capacity, reserveArgs).awaitFirstOrNull()

    override suspend fun bfScanDump(key: K, iterator: Long): ScanDumpValue? =
        ops.bfScanDump(key, iterator).awaitFirstOrNull()

}
