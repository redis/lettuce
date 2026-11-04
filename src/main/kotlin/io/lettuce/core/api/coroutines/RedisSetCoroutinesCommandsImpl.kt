/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
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
import io.lettuce.core.ExpireArgs
import io.lettuce.core.SAddExArgs
import io.lettuce.core.SDiffCardArgs
import io.lettuce.core.SUnionCardArgs
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.ValueScanCursor
import io.lettuce.core.api.reactive.RedisSetReactiveCommands
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.time.Duration
import java.time.Instant
import java.util.Date


/**
 * Coroutine executed commands (based on reactive commands) for Sets.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 *
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisSetCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisSetReactiveCommands<K, V>) : RedisSetCoroutinesCommands<K, V> {

    override suspend fun sadd(key: K, vararg members: V): Long? = ops.sadd(key, *members).awaitFirstOrNull()

    override suspend fun scard(key: K): Long? = ops.scard(key).awaitFirstOrNull()

    override fun sdiff(vararg keys: K): Flow<V> = ops.sdiff(*keys).asFlow()

    override suspend fun sdiffcard(key1: K, key2: K): Long? = ops.sdiffcard(key1, key2).awaitFirstOrNull()

    override suspend fun sdiffcard(keys: List<K>): Long? = ops.sdiffcard(keys).awaitFirstOrNull()

    override suspend fun sdiffcard(key1: K, key2: K, sdiffCardArgs: SDiffCardArgs): Long? =
        ops.sdiffcard(key1, key2, sdiffCardArgs).awaitFirstOrNull()

    override suspend fun sdiffcard(keys: List<K>, sdiffCardArgs: SDiffCardArgs): Long? =
        ops.sdiffcard(keys, sdiffCardArgs).awaitFirstOrNull()

    override suspend fun sdiffstore(destination: K, vararg keys: K): Long? = ops.sdiffstore(destination, *keys).awaitFirstOrNull()

    override fun sinter(vararg keys: K): Flow<V> = ops.sinter(*keys).asFlow()

    override suspend fun sintercard(vararg keys: K): Long? = ops.sintercard(*keys).awaitFirstOrNull()

    override suspend fun sintercard(limit: Long, vararg keys: K): Long? =
        ops.sintercard(limit, *keys).awaitFirstOrNull()

    override suspend fun sinterstore(destination: K, vararg keys: K): Long? = ops.sinterstore(destination, *keys).awaitFirstOrNull()

    override suspend fun sismember(key: K, member: V): Boolean? = ops.sismember(key, member).awaitFirstOrNull()

    override fun smembers(key: K): Flow<V> = ops.smembers(key).asFlow()

    override fun smismember(key: K, vararg members: V): Flow<Boolean> = ops.smismember(key, *members).asFlow()

    override suspend fun smove(source: K, destination: K, member: V): Boolean? = ops.smove(source, destination, member).awaitFirstOrNull()

    override suspend fun spop(key: K): V? = ops.spop(key).awaitFirstOrNull()

    override suspend fun spop(key: K, count: Long): Set<V> = ops.spop(key, count).asFlow().toSet()

    override suspend fun srandmember(key: K): V? = ops.srandmember(key).awaitFirstOrNull()

    override fun srandmember(key: K, count: Long): Flow<V> = ops.srandmember(key, count).asFlow()

    override suspend fun srem(key: K, vararg members: V): Long? = ops.srem(key, *members).awaitFirstOrNull()

    override fun sunion(vararg keys: K): Flow<V> = ops.sunion(*keys).asFlow()

    override suspend fun sunioncard(key1: K, key2: K): Long? = ops.sunioncard(key1, key2).awaitFirstOrNull()

    override suspend fun sunioncard(keys: List<K>): Long? = ops.sunioncard(keys).awaitFirstOrNull()

    override suspend fun sunioncard(key1: K, key2: K, sunionCardArgs: SUnionCardArgs): Long? =
        ops.sunioncard(key1, key2, sunionCardArgs).awaitFirstOrNull()

    override suspend fun sunioncard(keys: List<K>, sunionCardArgs: SUnionCardArgs): Long? =
        ops.sunioncard(keys, sunionCardArgs).awaitFirstOrNull()

    override suspend fun sunionstore(destination: K, vararg keys: K): Long? = ops.sunionstore(destination, *keys).awaitFirstOrNull()

    override suspend fun sscan(key: K): ValueScanCursor<V>? = ops.sscan(key).awaitFirstOrNull()

    override suspend fun sscan(key: K, scanArgs: ScanArgs): ValueScanCursor<V>? = ops.sscan(key, scanArgs).awaitFirstOrNull()

    override suspend fun sscan(key: K, scanCursor: ScanCursor, scanArgs: ScanArgs): ValueScanCursor<V>? = ops.sscan(key, scanCursor, scanArgs).awaitFirstOrNull()

    override suspend fun sscan(key: K, scanCursor: ScanCursor): ValueScanCursor<V>? = ops.sscan(key, scanCursor).awaitFirstOrNull()

    override suspend fun sexpire(key: K, seconds: Long, vararg members: V): List<Long> =
        ops.sexpire(key, seconds, *members).asFlow().toList()

    override suspend fun sexpire(key: K, seconds: Long, member: V): List<Long> =
        ops.sexpire(key, seconds, member).asFlow().toList()

    override suspend fun sexpire(key: K, seconds: Long, expireArgs: ExpireArgs, vararg members: V): List<Long> =
        ops.sexpire(key, seconds, expireArgs, *members).asFlow().toList()

    override suspend fun sexpire(key: K, seconds: Long, expireArgs: ExpireArgs, member: V): List<Long> =
        ops.sexpire(key, seconds, expireArgs, member).asFlow().toList()

    override suspend fun sexpire(key: K, seconds: Duration, vararg members: V): List<Long> =
        ops.sexpire(key, seconds, *members).asFlow().toList()

    override suspend fun sexpire(key: K, seconds: Duration, member: V): List<Long> =
        ops.sexpire(key, seconds, member).asFlow().toList()

    override suspend fun sexpire(key: K, seconds: Duration, expireArgs: ExpireArgs, vararg members: V): List<Long> =
        ops.sexpire(key, seconds, expireArgs, *members).asFlow().toList()

    override suspend fun sexpire(key: K, seconds: Duration, expireArgs: ExpireArgs, member: V): List<Long> =
        ops.sexpire(key, seconds, expireArgs, member).asFlow().toList()

    override suspend fun spexpire(key: K, milliseconds: Long, vararg members: V): List<Long> =
        ops.spexpire(key, milliseconds, *members).asFlow().toList()

    override suspend fun spexpire(key: K, milliseconds: Long, member: V): List<Long> =
        ops.spexpire(key, milliseconds, member).asFlow().toList()

    override suspend fun spexpire(key: K, milliseconds: Long, expireArgs: ExpireArgs, vararg members: V): List<Long> =
        ops.spexpire(key, milliseconds, expireArgs, *members).asFlow().toList()

    override suspend fun spexpire(key: K, milliseconds: Long, expireArgs: ExpireArgs, member: V): List<Long> =
        ops.spexpire(key, milliseconds, expireArgs, member).asFlow().toList()

    override suspend fun spexpire(key: K, milliseconds: Duration, vararg members: V): List<Long> =
        ops.spexpire(key, milliseconds, *members).asFlow().toList()

    override suspend fun spexpire(key: K, milliseconds: Duration, member: V): List<Long> =
        ops.spexpire(key, milliseconds, member).asFlow().toList()

    override suspend fun spexpire(key: K, milliseconds: Duration, expireArgs: ExpireArgs, vararg members: V): List<Long> =
        ops.spexpire(key, milliseconds, expireArgs, *members).asFlow().toList()

    override suspend fun spexpire(key: K, milliseconds: Duration, expireArgs: ExpireArgs, member: V): List<Long> =
        ops.spexpire(key, milliseconds, expireArgs, member).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Long, vararg members: V): List<Long> =
        ops.sexpireat(key, timestamp, *members).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Long, member: V): List<Long> =
        ops.sexpireat(key, timestamp, member).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Long, expireArgs: ExpireArgs, vararg members: V): List<Long> =
        ops.sexpireat(key, timestamp, expireArgs, *members).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Long, expireArgs: ExpireArgs, member: V): List<Long> =
        ops.sexpireat(key, timestamp, expireArgs, member).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Date, vararg members: V): List<Long> =
        ops.sexpireat(key, timestamp, *members).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Date, member: V): List<Long> =
        ops.sexpireat(key, timestamp, member).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Date, expireArgs: ExpireArgs, vararg members: V): List<Long> =
        ops.sexpireat(key, timestamp, expireArgs, *members).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Date, expireArgs: ExpireArgs, member: V): List<Long> =
        ops.sexpireat(key, timestamp, expireArgs, member).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Instant, vararg members: V): List<Long> =
        ops.sexpireat(key, timestamp, *members).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Instant, member: V): List<Long> =
        ops.sexpireat(key, timestamp, member).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Instant, expireArgs: ExpireArgs, vararg members: V): List<Long> =
        ops.sexpireat(key, timestamp, expireArgs, *members).asFlow().toList()

    override suspend fun sexpireat(key: K, timestamp: Instant, expireArgs: ExpireArgs, member: V): List<Long> =
        ops.sexpireat(key, timestamp, expireArgs, member).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Long, vararg members: V): List<Long> =
        ops.spexpireat(key, timestamp, *members).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Long, member: V): List<Long> =
        ops.spexpireat(key, timestamp, member).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Long, expireArgs: ExpireArgs, vararg members: V): List<Long> =
        ops.spexpireat(key, timestamp, expireArgs, *members).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Long, expireArgs: ExpireArgs, member: V): List<Long> =
        ops.spexpireat(key, timestamp, expireArgs, member).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Date, vararg members: V): List<Long> =
        ops.spexpireat(key, timestamp, *members).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Date, member: V): List<Long> =
        ops.spexpireat(key, timestamp, member).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Date, expireArgs: ExpireArgs, vararg members: V): List<Long> =
        ops.spexpireat(key, timestamp, expireArgs, *members).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Date, expireArgs: ExpireArgs, member: V): List<Long> =
        ops.spexpireat(key, timestamp, expireArgs, member).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Instant, vararg members: V): List<Long> =
        ops.spexpireat(key, timestamp, *members).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Instant, member: V): List<Long> =
        ops.spexpireat(key, timestamp, member).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Instant, expireArgs: ExpireArgs, vararg members: V): List<Long> =
        ops.spexpireat(key, timestamp, expireArgs, *members).asFlow().toList()

    override suspend fun spexpireat(key: K, timestamp: Instant, expireArgs: ExpireArgs, member: V): List<Long> =
        ops.spexpireat(key, timestamp, expireArgs, member).asFlow().toList()

    override suspend fun sttl(key: K, vararg members: V): List<Long> =
        ops.sttl(key, *members).asFlow().toList()

    override suspend fun sttl(key: K, member: V): List<Long> =
        ops.sttl(key, member).asFlow().toList()

    override suspend fun spttl(key: K, vararg members: V): List<Long> =
        ops.spttl(key, *members).asFlow().toList()

    override suspend fun spttl(key: K, member: V): List<Long> =
        ops.spttl(key, member).asFlow().toList()

    override suspend fun sexpiretime(key: K, vararg members: V): List<Long> =
        ops.sexpiretime(key, *members).asFlow().toList()

    override suspend fun sexpiretime(key: K, member: V): List<Long> =
        ops.sexpiretime(key, member).asFlow().toList()

    override suspend fun spexpiretime(key: K, vararg members: V): List<Long> =
        ops.spexpiretime(key, *members).asFlow().toList()

    override suspend fun spexpiretime(key: K, member: V): List<Long> =
        ops.spexpiretime(key, member).asFlow().toList()

    override suspend fun spersist(key: K, vararg members: V): List<Long> =
        ops.spersist(key, *members).asFlow().toList()

    override suspend fun spersist(key: K, member: V): List<Long> =
        ops.spersist(key, member).asFlow().toList()

    override suspend fun saddex(key: K, vararg members: V): Long? =
        ops.saddex(key, *members).awaitFirstOrNull()

    override suspend fun saddex(key: K, member: V): Long? =
        ops.saddex(key, member).awaitFirstOrNull()

    override suspend fun saddex(key: K, sAddExArgs: SAddExArgs, vararg members: V): Long? =
        ops.saddex(key, sAddExArgs, *members).awaitFirstOrNull()

    override suspend fun saddex(key: K, sAddExArgs: SAddExArgs, member: V): Long? =
        ops.saddex(key, sAddExArgs, member).awaitFirstOrNull()

}
