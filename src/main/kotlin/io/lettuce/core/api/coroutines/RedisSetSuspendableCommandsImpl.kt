/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused")

package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.ValueScanCursor
import io.lettuce.core.api.reactive.RedisSetReactiveCommands
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull


/**
 * Coroutine executed commands (based on reactive commands) for Sets.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 *
 * @generated by io.lettuce.apigenerator.CreateKotlinCoroutinesReactiveImplementation
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisSetSuspendableCommandsImpl<K : Any, V : Any>(private val ops: RedisSetReactiveCommands<K, V>) : RedisSetSuspendableCommands<K, V> {

    override suspend fun sadd(key: K, vararg members: V): Long? = ops.sadd(key, *members).awaitFirstOrNull()

    override suspend fun scard(key: K): Long? = ops.scard(key).awaitFirstOrNull()

    override suspend fun sdiff(vararg keys: K): Set<V> = ops.sdiff(*keys).asFlow().toSet()

    override suspend fun sdiffstore(destination: K, vararg keys: K): Long? = ops.sdiffstore(destination, *keys).awaitFirstOrNull()

    override suspend fun sinter(vararg keys: K): Set<V> = ops.sinter(*keys).asFlow().toSet()

    override suspend fun sinterstore(destination: K, vararg keys: K): Long? = ops.sinterstore(destination, *keys).awaitFirstOrNull()

    override suspend fun sismember(key: K, member: V): Boolean? = ops.sismember(key, member).awaitFirstOrNull()

    override suspend fun smove(source: K, destination: K, member: V): Boolean? = ops.smove(source, destination, member).awaitFirstOrNull()

    override suspend fun smembers(key: K): Set<V> = ops.smembers(key).asFlow().toSet()

    override suspend fun spop(key: K): V? = ops.spop(key).awaitFirstOrNull()

    override suspend fun spop(key: K, count: Long): Set<V> = ops.spop(key, count).asFlow().toSet()

    override suspend fun srandmember(key: K): V? = ops.srandmember(key).awaitFirstOrNull()

    override suspend fun srandmember(key: K, count: Long): List<V> = ops.srandmember(key, count).asFlow().toList()

    override suspend fun srem(key: K, vararg members: V): Long? = ops.srem(key, *members).awaitFirstOrNull()

    override suspend fun sunion(vararg keys: K): Set<V> = ops.sunion(*keys).asFlow().toSet()

    override suspend fun sunionstore(destination: K, vararg keys: K): Long? = ops.sunionstore(destination, *keys).awaitFirstOrNull()

    override suspend fun sscan(key: K): ValueScanCursor<V>? = ops.sscan(key).awaitFirstOrNull()

    override suspend fun sscan(key: K, scanArgs: ScanArgs): ValueScanCursor<V>? = ops.sscan(key, scanArgs).awaitFirstOrNull()

    override suspend fun sscan(key: K, scanCursor: ScanCursor, scanArgs: ScanArgs): ValueScanCursor<V>? = ops.sscan(key, scanCursor, scanArgs).awaitFirstOrNull()

    override suspend fun sscan(key: K, scanCursor: ScanCursor): ValueScanCursor<V>? = ops.sscan(key, scanCursor).awaitFirstOrNull()

}

