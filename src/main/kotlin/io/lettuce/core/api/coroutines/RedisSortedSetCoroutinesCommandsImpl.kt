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

package io.lettuce.core.api.coroutines

import io.lettuce.core.*
import io.lettuce.core.api.reactive.RedisSortedSetReactiveCommands
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull


/**
 * Coroutine executed commands (based on reactive commands) for Sorted Sets.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 *
 * @generated by io.lettuce.apigenerator.CreateKotlinCoroutinesReactiveImplementation
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisSortedSetCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisSortedSetReactiveCommands<K, V>) : RedisSortedSetCoroutinesCommands<K, V> {

    override suspend fun bzpopmin(timeout: Long, vararg keys: K): KeyValue<K, ScoredValue<V>>? = ops.bzpopmin(timeout, *keys).awaitFirstOrNull()

    override suspend fun bzpopmax(timeout: Long, vararg keys: K): KeyValue<K, ScoredValue<V>>? = ops.bzpopmax(timeout, *keys).awaitFirstOrNull()

    override suspend fun zadd(key: K, score: Double, member: V): Long? = ops.zadd(key, score, member).awaitFirstOrNull()

    override suspend fun zadd(key: K, vararg scoresAndValues: Any): Long? = ops.zadd(key, *scoresAndValues).awaitFirstOrNull()

    override suspend fun zadd(key: K, vararg scoredValues: ScoredValue<V>): Long? = ops.zadd(key, *scoredValues).awaitFirstOrNull()

    override suspend fun zadd(key: K, zAddArgs: ZAddArgs, score: Double, member: V): Long? = ops.zadd(key, zAddArgs, score, member).awaitFirstOrNull()

    override suspend fun zadd(key: K, zAddArgs: ZAddArgs, vararg scoresAndValues: Any): Long? = ops.zadd(key, zAddArgs, *scoresAndValues).awaitFirstOrNull()

    override suspend fun zadd(key: K, zAddArgs: ZAddArgs, vararg scoredValues: ScoredValue<V>): Long? = ops.zadd(key, zAddArgs, *scoredValues).awaitFirstOrNull()

    override suspend fun zaddincr(key: K, score: Double, member: V): Double? = ops.zaddincr(key, score, member).awaitFirstOrNull()

    override suspend fun zaddincr(key: K, zAddArgs: ZAddArgs, score: Double, member: V): Double? = ops.zaddincr(key, zAddArgs, score, member).awaitFirstOrNull()

    override suspend fun zcard(key: K): Long? = ops.zcard(key).awaitFirstOrNull()

    override suspend fun zcount(key: K, range: Range<out Number>): Long? = ops.zcount(key, range).awaitFirstOrNull()

    override suspend fun zincrby(key: K, amount: Double, member: V): Double? = ops.zincrby(key, amount, member).awaitFirstOrNull()

    override suspend fun zinterstore(destination: K, vararg keys: K): Long? = ops.zinterstore(destination, *keys).awaitFirstOrNull()

    override suspend fun zinterstore(destination: K, storeArgs: ZStoreArgs, vararg keys: K): Long? = ops.zinterstore(destination, storeArgs, *keys).awaitFirstOrNull()

    override suspend fun zlexcount(key: K, range: Range<out V>): Long? = ops.zlexcount(key, range).awaitFirstOrNull()

    override suspend fun zpopmin(key: K): ScoredValue<V>? = ops.zpopmin(key).awaitFirstOrNull()

    override fun zpopmin(key: K, count: Long): Flow<ScoredValue<V>> = ops.zpopmin(key, count).asFlow()

    override suspend fun zpopmax(key: K): ScoredValue<V>? = ops.zpopmax(key).awaitFirstOrNull()

    override fun zpopmax(key: K, count: Long): Flow<ScoredValue<V>> = ops.zpopmax(key, count).asFlow()

    override fun zrange(key: K, start: Long, stop: Long): Flow<V> = ops.zrange(key, start, stop).asFlow()

    override fun zrangeWithScores(key: K, start: Long, stop: Long): Flow<ScoredValue<V>> = ops.zrangeWithScores(key, start, stop).asFlow()

    override fun zrangebylex(key: K, range: Range<out V>): Flow<V> = ops.zrangebylex(key, range).asFlow()

    override fun zrangebylex(key: K, range: Range<out V>, limit: Limit): Flow<V> = ops.zrangebylex(key, range, limit).asFlow()

    override fun zrangebyscore(key: K, range: Range<out Number>): Flow<V> = ops.zrangebyscore(key, range).asFlow()

    override fun zrangebyscore(key: K, range: Range<out Number>, limit: Limit): Flow<V> = ops.zrangebyscore(key, range, limit).asFlow()

    override fun zrangebyscoreWithScores(key: K, range: Range<out Number>): Flow<ScoredValue<V>> = ops.zrangebyscoreWithScores(key, range).asFlow()

    override fun zrangebyscoreWithScores(key: K, range: Range<out Number>, limit: Limit): Flow<ScoredValue<V>> = ops.zrangebyscoreWithScores(key, range, limit).asFlow()

    override suspend fun zrank(key: K, member: V): Long? = ops.zrank(key, member).awaitFirstOrNull()

    override suspend fun zrem(key: K, vararg members: V): Long? = ops.zrem(key, *members).awaitFirstOrNull()

    override suspend fun zremrangebylex(key: K, range: Range<out V>): Long? = ops.zremrangebylex(key, range).awaitFirstOrNull()

    override suspend fun zremrangebyrank(key: K, start: Long, stop: Long): Long? = ops.zremrangebyrank(key, start, stop).awaitFirstOrNull()

    override suspend fun zremrangebyscore(key: K, range: Range<out Number>): Long? = ops.zremrangebyscore(key, range).awaitFirstOrNull()

    override fun zrevrange(key: K, start: Long, stop: Long): Flow<V> = ops.zrevrange(key, start, stop).asFlow()

    override fun zrevrangeWithScores(key: K, start: Long, stop: Long): Flow<ScoredValue<V>> = ops.zrevrangeWithScores(key, start, stop).asFlow()

    override fun zrevrangebylex(key: K, range: Range<out V>): Flow<V> = ops.zrevrangebylex(key, range).asFlow()

    override fun zrevrangebylex(key: K, range: Range<out V>, limit: Limit): Flow<V> = ops.zrevrangebylex(key, range, limit).asFlow()

    override fun zrevrangebyscore(key: K, range: Range<out Number>): Flow<V> = ops.zrevrangebyscore(key, range).asFlow()

    override fun zrevrangebyscore(key: K, range: Range<out Number>, limit: Limit): Flow<V> = ops.zrevrangebyscore(key, range, limit).asFlow()

    override fun zrevrangebyscoreWithScores(key: K, range: Range<out Number>): Flow<ScoredValue<V>> = ops.zrevrangebyscoreWithScores(key, range).asFlow()

    override fun zrevrangebyscoreWithScores(key: K, range: Range<out Number>, limit: Limit): Flow<ScoredValue<V>> = ops.zrevrangebyscoreWithScores(key, range, limit).asFlow()

    override suspend fun zrevrank(key: K, member: V): Long? = ops.zrevrank(key, member).awaitFirstOrNull()

    override suspend fun zscan(key: K): ScoredValueScanCursor<V>? = ops.zscan(key).awaitFirstOrNull()

    override suspend fun zscan(key: K, scanArgs: ScanArgs): ScoredValueScanCursor<V>? = ops.zscan(key, scanArgs).awaitFirstOrNull()

    override suspend fun zscan(key: K, scanCursor: ScanCursor, scanArgs: ScanArgs): ScoredValueScanCursor<V>? = ops.zscan(key, scanCursor, scanArgs).awaitFirstOrNull()

    override suspend fun zscan(key: K, scanCursor: ScanCursor): ScoredValueScanCursor<V>? = ops.zscan(key, scanCursor).awaitFirstOrNull()

    override suspend fun zscore(key: K, member: V): Double? = ops.zscore(key, member).awaitFirstOrNull()

    override suspend fun zunionstore(destination: K, vararg keys: K): Long? = ops.zunionstore(destination, *keys).awaitFirstOrNull()

    override suspend fun zunionstore(destination: K, storeArgs: ZStoreArgs, vararg keys: K): Long? = ops.zunionstore(destination, storeArgs, *keys).awaitFirstOrNull()

}

