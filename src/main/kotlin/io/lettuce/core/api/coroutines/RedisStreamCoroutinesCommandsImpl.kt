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
import io.lettuce.core.XReadArgs.StreamOffset
import io.lettuce.core.api.reactive.RedisStreamReactiveCommands
import io.lettuce.core.models.stream.PendingMessage
import io.lettuce.core.models.stream.PendingMessages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for Streams.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 5.1
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisStreamCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisStreamReactiveCommands<K, V>) : RedisStreamCoroutinesCommands<K, V> {

    override suspend fun xack(key: K, group: K, vararg messageIds: String): Long? = ops.xack(key, group, *messageIds).awaitFirstOrNull()

    override suspend fun xadd(key: K, body: Map<K, V>): String? = ops.xadd(key, body).awaitFirstOrNull()

    override suspend fun xadd(key: K, args: XAddArgs, body: Map<K, V>): String? = ops.xadd(key, args, body).awaitFirstOrNull()

    override suspend fun xadd(key: K, vararg keysAndValues: Any): String? = ops.xadd(key, *keysAndValues).awaitFirstOrNull()

    override suspend fun xadd(key: K, args: XAddArgs, vararg keysAndValues: Any): String? = ops.xadd(key, args, *keysAndValues).awaitFirstOrNull()

    override fun xclaim(key: K, consumer: Consumer<K>, minIdleTime: Long, vararg messageIds: String): Flow<StreamMessage<K, V>> = ops.xclaim(key, consumer, minIdleTime, *messageIds).asFlow()

    override fun xclaim(key: K, consumer: Consumer<K>, args: XClaimArgs, vararg messageIds: String): Flow<StreamMessage<K, V>> = ops.xclaim(key, consumer, args, *messageIds).asFlow()

    override suspend fun xdel(key: K, vararg messageIds: String): Long? = ops.xdel(key, *messageIds).awaitFirstOrNull()

    override suspend fun xgroupCreate(streamOffset: StreamOffset<K>, group: K): String? = ops.xgroupCreate(streamOffset, group).awaitFirstOrNull()

    override suspend fun xgroupCreate(streamOffset: StreamOffset<K>, group: K, args: XGroupCreateArgs): String? = ops.xgroupCreate(streamOffset, group, args).awaitFirstOrNull()

    override suspend fun xgroupDelconsumer(key: K, consumer: Consumer<K>): Long? = ops.xgroupDelconsumer(key, consumer).awaitFirstOrNull()

    override suspend fun xgroupDestroy(key: K, group: K): Boolean? = ops.xgroupDestroy(key, group).awaitFirstOrNull()

    override suspend fun xgroupSetid(streamOffset: StreamOffset<K>, group: K): String? = ops.xgroupSetid(streamOffset, group).awaitFirstOrNull()

    override suspend fun xinfoStream(key: K): List<Any> = ops.xinfoStream(key).asFlow().toList()

    override suspend fun xinfoGroups(key: K): List<Any> = ops.xinfoGroups(key).asFlow().toList()

    override suspend fun xinfoConsumers(key: K, group: K): List<Any> = ops.xinfoConsumers(key, group).asFlow().toList()

    override suspend fun xlen(key: K): Long? = ops.xlen(key).awaitFirstOrNull()

    override suspend fun xpending(key: K, group: K): PendingMessages? = ops.xpending(key, group).awaitFirstOrNull()

    override fun xpending(key: K, group: K, range: Range<String>, limit: Limit): Flow<PendingMessage> = ops.xpending(key, group, range, limit).asFlow()

    override fun xpending(key: K, consumer: Consumer<K>, range: Range<String>, limit: Limit): Flow<PendingMessage> = ops.xpending(key, consumer, range, limit).asFlow()

    override fun xrange(key: K, range: Range<String>): Flow<StreamMessage<K, V>> = ops.xrange(key, range).asFlow()

    override fun xrange(key: K, range: Range<String>, limit: Limit): Flow<StreamMessage<K, V>> = ops.xrange(key, range, limit).asFlow()

    override fun xread(vararg streams: StreamOffset<K>): Flow<StreamMessage<K, V>> = ops.xread(*streams).asFlow()

    override fun xread(args: XReadArgs, vararg streams: StreamOffset<K>): Flow<StreamMessage<K, V>> = ops.xread(args, *streams).asFlow()

    override fun xreadgroup(consumer: Consumer<K>, vararg streams: StreamOffset<K>): Flow<StreamMessage<K, V>> = ops.xreadgroup(consumer, *streams).asFlow()

    override fun xreadgroup(consumer: Consumer<K>, args: XReadArgs, vararg streams: StreamOffset<K>): Flow<StreamMessage<K, V>> = ops.xreadgroup(consumer, args, *streams).asFlow()

    override fun xrevrange(key: K, range: Range<String>): Flow<StreamMessage<K, V>> = ops.xrevrange(key, range).asFlow()

    override fun xrevrange(key: K, range: Range<String>, limit: Limit): Flow<StreamMessage<K, V>> = ops.xrevrange(key, range, limit).asFlow()

    override suspend fun xtrim(key: K, count: Long): Long? = ops.xtrim(key, count).awaitFirstOrNull()

    override suspend fun xtrim(key: K, approximateTrimming: Boolean, count: Long): Long? = ops.xtrim(key, approximateTrimming, count).awaitFirstOrNull()

}

