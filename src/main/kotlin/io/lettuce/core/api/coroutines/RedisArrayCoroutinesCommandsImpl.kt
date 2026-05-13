/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.Range
import io.lettuce.core.api.reactive.RedisArrayReactiveCommands
import io.lettuce.core.array.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.asFlow

/**
 * Coroutine executed commands (based on reactive commands) for Redis Arrays.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.6
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisArrayCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisArrayReactiveCommands<K, V>) :
    RedisArrayCoroutinesCommands<K, V> {

    override suspend fun arset(key: K, index: Long, value: V): Long? =
        ops.arset(key, index, value).awaitFirstOrNull()

    override suspend fun armset(key: K, indexValueMap: Map<Long, V>): Long? =
        ops.armset(key, indexValueMap).awaitFirstOrNull()

    override suspend fun arget(key: K, index: Long): V? =
        ops.arget(key, index).awaitFirstOrNull()

    override suspend fun armget(key: K, vararg indices: Long): List<V> =
        ops.armget(key, *indices).asFlow().toList()

    override suspend fun ardel(key: K, index: Long): Long? =
        ops.ardel(key, index).awaitFirstOrNull()

    override suspend fun ardel(key: K, vararg indices: Long): Long? =
        ops.ardel(key, *indices).awaitFirstOrNull()

    override suspend fun ardelrange(key: K, vararg ranges: Range<Long>): Long? =
        ops.ardelrange(key, *ranges).awaitFirstOrNull()

    override suspend fun arlen(key: K): Long? = ops.arlen(key).awaitFirstOrNull()

    override suspend fun arcount(key: K): Long? = ops.arcount(key).awaitFirstOrNull()

    override suspend fun argetrange(key: K, range: Range<Long>): List<V> =
        ops.argetrange(key, range).asFlow().toList()

    override suspend fun arnext(key: K): Long? = ops.arnext(key).awaitFirstOrNull()

    override suspend fun arlastitems(key: K, count: Long): List<V> =
        ops.arlastitems(key, count).asFlow().toList()

    override suspend fun arlastitemsRev(key: K, count: Long): List<V> =
        ops.arlastitemsRev(key, count).asFlow().toList()

    override suspend fun arscan(key: K, scanArgs: ArScanArgs): List<IndexedValue<V>> =
        ops.arscan(key, scanArgs).asFlow().toList()

    override suspend fun argrep(key: K, grepArgs: ArGrepArgs): List<Long> =
        ops.argrep(key, grepArgs).asFlow().toList()

    override suspend fun argrepWithValues(key: K, grepArgs: ArGrepArgs): List<IndexedValue<V>> =
        ops.argrepWithValues(key, grepArgs).asFlow().toList()

    override suspend fun aropAggregate(key: K, range: Range<Long>, operation: ArAggregateType): V? =
        ops.aropAggregate(key, range, operation).awaitFirstOrNull()

    override suspend fun aropCount(key: K, range: Range<Long>, operation: ArCountType): Long? =
        ops.aropCount(key, range, operation).awaitFirstOrNull()

    override suspend fun aropMatch(key: K, range: Range<Long>, matchValue: V): Long? =
        ops.aropMatch(key, range, matchValue).awaitFirstOrNull()

    override suspend fun arinsert(key: K, vararg values: V): Long? =
        ops.arinsert(key, *values).awaitFirstOrNull()

    override suspend fun arring(key: K, size: Long, vararg values: V): Long? =
        ops.arring(key, size, *values).awaitFirstOrNull()

    override suspend fun arseek(key: K, index: Long): Long? =
        ops.arseek(key, index).awaitFirstOrNull()

    override suspend fun arinfo(key: K): ArrayMetadata? =
        ops.arinfo(key).awaitFirstOrNull()

    override suspend fun arinfoFull(key: K): ArrayFullMetadata? =
        ops.arinfoFull(key).awaitFirstOrNull()
}

