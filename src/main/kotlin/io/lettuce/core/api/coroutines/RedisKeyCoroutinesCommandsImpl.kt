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
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.util.*

/**
 * Coroutine executed commands (based on reactive commands) for Keys (Key manipulation/querying).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisKeyCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisKeyReactiveCommands<K, V>) : RedisKeyCoroutinesCommands<K, V> {

    override suspend fun del(vararg keys: K): Long? = ops.del(*keys).awaitFirstOrNull()

    override suspend fun unlink(vararg keys: K): Long? = ops.unlink(*keys).awaitFirstOrNull()

    override suspend fun dump(key: K): ByteArray? = ops.dump(key).awaitFirstOrNull()

    override suspend fun exists(vararg keys: K): Long? = ops.exists(*keys).awaitFirstOrNull()

    override suspend fun expire(key: K, seconds: Long): Boolean? = ops.expire(key, seconds).awaitFirstOrNull()

    override suspend fun expireat(key: K, timestamp: Date): Boolean? = ops.expireat(key, timestamp).awaitFirstOrNull()

    override suspend fun expireat(key: K, timestamp: Long): Boolean? = ops.expireat(key, timestamp).awaitFirstOrNull()

    override fun keys(pattern: K): Flow<K> = ops.keys(pattern).asFlow()

    override suspend fun migrate(host: String, port: Int, key: K, db: Int, timeout: Long): String? = ops.migrate(host, port, key, db, timeout).awaitFirstOrNull()

    override suspend fun migrate(host: String, port: Int, db: Int, timeout: Long, migrateArgs: MigrateArgs<K>): String? = ops.migrate(host, port, db, timeout, migrateArgs).awaitFirstOrNull()

    override suspend fun move(key: K, db: Int): Boolean? = ops.move(key, db).awaitFirstOrNull()

    override suspend fun objectEncoding(key: K): String? = ops.objectEncoding(key).awaitFirstOrNull()

    override suspend fun objectIdletime(key: K): Long? = ops.objectIdletime(key).awaitFirstOrNull()

    override suspend fun objectRefcount(key: K): Long? = ops.objectRefcount(key).awaitFirstOrNull()

    override suspend fun persist(key: K): Boolean? = ops.persist(key).awaitFirstOrNull()

    override suspend fun pexpire(key: K, milliseconds: Long): Boolean? = ops.pexpire(key, milliseconds).awaitFirstOrNull()

    override suspend fun pexpireat(key: K, timestamp: Date): Boolean? = ops.pexpireat(key, timestamp).awaitFirstOrNull()

    override suspend fun pexpireat(key: K, timestamp: Long): Boolean? = ops.pexpireat(key, timestamp).awaitFirstOrNull()

    override suspend fun pttl(key: K): Long? = ops.pttl(key).awaitFirstOrNull()

    override suspend fun randomkey(): K? = ops.randomkey().awaitFirstOrNull()

    override suspend fun rename(key: K, newKey: K): String? = ops.rename(key, newKey).awaitFirstOrNull()

    override suspend fun renamenx(key: K, newKey: K): Boolean? = ops.renamenx(key, newKey).awaitFirstOrNull()

    override suspend fun restore(key: K, ttl: Long, value: ByteArray): String? = ops.restore(key, ttl, value).awaitFirstOrNull()

    override suspend fun restore(key: K, value: ByteArray, args: RestoreArgs): String? = ops.restore(key, value, args).awaitFirstOrNull()

    override fun sort(key: K): Flow<V> = ops.sort(key).asFlow()

    override fun sort(key: K, sortArgs: SortArgs): Flow<V> = ops.sort(key, sortArgs).asFlow()

    override suspend fun sortStore(key: K, sortArgs: SortArgs, destination: K): Long? = ops.sortStore(key, sortArgs, destination).awaitFirstOrNull()

    override suspend fun touch(vararg keys: K): Long? = ops.touch(*keys).awaitFirstOrNull()

    override suspend fun ttl(key: K): Long? = ops.ttl(key).awaitFirstOrNull()

    override suspend fun type(key: K): String? = ops.type(key).awaitFirstOrNull()

    override suspend fun scan(): KeyScanCursor<K>? = ops.scan().awaitFirstOrNull()

    override suspend fun scan(scanArgs: ScanArgs): KeyScanCursor<K>? = ops.scan(scanArgs).awaitFirstOrNull()

    override suspend fun scan(scanCursor: ScanCursor, scanArgs: ScanArgs): KeyScanCursor<K>? = ops.scan(scanCursor, scanArgs).awaitFirstOrNull()

    override suspend fun scan(scanCursor: ScanCursor): KeyScanCursor<K>? = ops.scan(scanCursor).awaitFirstOrNull()

}

