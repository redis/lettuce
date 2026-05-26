/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.reactive.RedisBloomFilterReactiveCommands
import io.lettuce.core.bf.BfInfoValue
import io.lettuce.core.bf.BfScanDumpValue
import io.lettuce.core.bf.arguments.BfInsertArgs
import io.lettuce.core.bf.arguments.BfReserveArgs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for basic commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Yordan Tsintsov
 * @since 7.6
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

    override suspend fun bfInsert(key: K, vararg values: V): List<Boolean> =
        ops.bfInsert(key, *values).asFlow().toList()

    override suspend fun bfInsert(
        key: K,
        insertArgs: BfInsertArgs,
        vararg values: V
    ): List<Boolean> =
        ops.bfInsert(key, insertArgs, *values).asFlow().toList()

    override suspend fun bfLoadChunk(key: K, iterator: Long, data: ByteArray): String? =
        ops.bfLoadChunk(key, iterator, data).awaitFirstOrNull()

    override suspend fun bfMAdd(key: K, vararg values: V): List<Boolean> =
        ops.bfMAdd(key, *values).asFlow().toList()

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

    override suspend fun bfScanDump(key: K, iterator: Long): BfScanDumpValue? =
        ops.bfScanDump(key, iterator).awaitFirstOrNull()

}
