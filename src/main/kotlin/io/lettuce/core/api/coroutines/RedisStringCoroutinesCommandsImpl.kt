/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import io.lettuce.core.*
import io.lettuce.core.api.reactive.RedisStringReactiveCommands
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull


/**
 * Coroutine executed commands (based on reactive commands) for Strings.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisStringCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisStringReactiveCommands<K, V>) : RedisStringCoroutinesCommands<K, V> {

    override suspend fun append(key: K, value: V): Long? = ops.append(key, value).awaitFirstOrNull()

    override suspend fun bitcount(key: K): Long? = ops.bitcount(key).awaitFirstOrNull()

    override suspend fun bitcount(key: K, start: Long, end: Long): Long? = ops.bitcount(key, start, end).awaitFirstOrNull()

    override suspend fun bitfield(key: K, bitFieldArgs: BitFieldArgs): List<Long> = ops.bitfield(key, bitFieldArgs).map { it.value }.asFlow().toList()

    override suspend fun bitpos(key: K, state: Boolean): Long? = ops.bitpos(key, state).awaitFirstOrNull()

    override suspend fun bitpos(key: K, state: Boolean, start: Long): Long? = ops.bitpos(key, state, start).awaitFirstOrNull()

    override suspend fun bitpos(key: K, state: Boolean, start: Long, end: Long): Long? = ops.bitpos(key, state, start, end).awaitFirstOrNull()

    override suspend fun bitopAnd(destination: K, vararg keys: K): Long? = ops.bitopAnd(destination, *keys).awaitFirstOrNull()

    override suspend fun bitopNot(destination: K, source: K): Long? = ops.bitopNot(destination, source).awaitFirstOrNull()

    override suspend fun bitopOr(destination: K, vararg keys: K): Long? = ops.bitopOr(destination, *keys).awaitFirstOrNull()

    override suspend fun bitopXor(destination: K, vararg keys: K): Long? = ops.bitopXor(destination, *keys).awaitFirstOrNull()

    override suspend fun decr(key: K): Long? = ops.decr(key).awaitFirstOrNull()

    override suspend fun decrby(key: K, amount: Long): Long? = ops.decrby(key, amount).awaitFirstOrNull()

    override suspend fun get(key: K): V? = ops.get(key).awaitFirstOrNull()

    override suspend fun getbit(key: K, offset: Long): Long? = ops.getbit(key, offset).awaitFirstOrNull()

    override suspend fun getrange(key: K, start: Long, end: Long): V? = ops.getrange(key, start, end).awaitFirstOrNull()

    override suspend fun getset(key: K, value: V): V? = ops.getset(key, value).awaitFirstOrNull()

    override suspend fun incr(key: K): Long? = ops.incr(key).awaitFirstOrNull()

    override suspend fun incrby(key: K, amount: Long): Long? = ops.incrby(key, amount).awaitFirstOrNull()

    override suspend fun incrbyfloat(key: K, amount: Double): Double? = ops.incrbyfloat(key, amount).awaitFirstOrNull()

    override fun mget(vararg keys: K): Flow<KeyValue<K, V>> = ops.mget(*keys).asFlow()

    override suspend fun mset(map: Map<K, V>): String? = ops.mset(map).awaitFirstOrNull()

    override suspend fun msetnx(map: Map<K, V>): Boolean? = ops.msetnx(map).awaitFirstOrNull()

    override suspend fun set(key: K, value: V): String? = ops.set(key, value).awaitFirstOrNull()

    override suspend fun set(key: K, value: V, setArgs: SetArgs): String? = ops.set(key, value, setArgs).awaitFirstOrNull()

    override suspend fun setbit(key: K, offset: Long, value: Int): Long? = ops.setbit(key, offset, value).awaitFirstOrNull()

    override suspend fun setex(key: K, seconds: Long, value: V): String? = ops.setex(key, seconds, value).awaitFirstOrNull()

    override suspend fun psetex(key: K, milliseconds: Long, value: V): String? = ops.psetex(key, milliseconds, value).awaitFirstOrNull()

    override suspend fun setnx(key: K, value: V): Boolean? = ops.setnx(key, value).awaitFirstOrNull()

    override suspend fun setrange(key: K, offset: Long, value: V): Long? = ops.setrange(key, offset, value).awaitFirstOrNull()

    override suspend fun stralgoLcs(strAlgoArgs: StrAlgoArgs): StringMatchResult? = ops.stralgoLcs(strAlgoArgs).awaitFirstOrNull()

    override suspend fun strlen(key: K): Long? = ops.strlen(key).awaitFirstOrNull()

}

