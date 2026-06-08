/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.reactive.RedisCuckooFilterReactiveCommands
import io.lettuce.core.cf.CfInfoValue
import io.lettuce.core.cf.CfScanDumpValue
import io.lettuce.core.cf.arguments.CfInsertArgs
import io.lettuce.core.cf.arguments.CfReserveArgs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for Cuckoo Filter commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Gyumin Hwang
 * @since 7.7
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisCuckooFilterCoroutinesCommandsImpl<K : Any, V : Any>(
    internal val ops: RedisCuckooFilterReactiveCommands<K, V>
) : RedisCuckooFilterCoroutinesCommands<K, V> {

    override suspend fun cfReserve(key: K, capacity: Long): String? =
        ops.cfReserve(key, capacity).awaitFirstOrNull()

    override suspend fun cfReserve(key: K, capacity: Long, args: CfReserveArgs): String? =
        ops.cfReserve(key, capacity, args).awaitFirstOrNull()

    override suspend fun cfAdd(key: K, value: V): Boolean? =
        ops.cfAdd(key, value).awaitFirstOrNull()

    override suspend fun cfAddNx(key: K, value: V): Boolean? =
        ops.cfAddNx(key, value).awaitFirstOrNull()

    override suspend fun cfInsert(key: K, vararg values: V): List<Boolean?> =
        ops.cfInsert(key, *values).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun cfInsert(key: K, args: CfInsertArgs, vararg values: V): List<Boolean?> =
        ops.cfInsert(key, args, *values).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun cfInsertNx(key: K, vararg values: V): List<Boolean?> =
        ops.cfInsertNx(key, *values).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun cfInsertNx(key: K, args: CfInsertArgs, vararg values: V): List<Boolean?> =
        ops.cfInsertNx(key, args, *values).asFlow().toList().map { it.getValueOrElse(null) }

    override suspend fun cfExists(key: K, value: V): Boolean? =
        ops.cfExists(key, value).awaitFirstOrNull()

    override suspend fun cfMExists(key: K, vararg values: V): List<Boolean> =
        ops.cfMExists(key, *values).asFlow().toList()

    override suspend fun cfDel(key: K, value: V): Boolean? =
        ops.cfDel(key, value).awaitFirstOrNull()

    override suspend fun cfCount(key: K, value: V): Long? =
        ops.cfCount(key, value).awaitFirstOrNull()

    override suspend fun cfScanDump(key: K, cursor: Long): CfScanDumpValue? =
        ops.cfScanDump(key, cursor).awaitFirstOrNull()

    override suspend fun cfLoadChunk(key: K, cursor: Long, data: ByteArray): String? =
        ops.cfLoadChunk(key, cursor, data).awaitFirstOrNull()

    override suspend fun cfInfo(key: K): CfInfoValue? =
        ops.cfInfo(key).awaitFirstOrNull()

}
