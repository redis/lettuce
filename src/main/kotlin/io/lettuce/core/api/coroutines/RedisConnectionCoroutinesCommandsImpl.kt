/*
 * Copyright 2020-2022 the original author or authors.
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

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.KillArgs
import io.lettuce.core.TrackingArgs
import io.lettuce.core.UnblockType
import io.lettuce.core.api.reactive.RedisConnectionReactiveCommands
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle


/**
 * Coroutine executed commands (based on reactive commands) for basic commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisConnectionCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisConnectionReactiveCommands<K, V>) : RedisConnectionCoroutinesCommands<K, V> {

    override suspend fun auth(password: CharSequence): String? = ops.auth(password).awaitFirstOrNull()

    override suspend fun auth(username: String, password: CharSequence): String? = ops.auth(username, password).awaitFirstOrNull()

    override suspend fun clientCaching(enabled: Boolean): String? = ops.clientCaching(enabled).awaitFirstOrNull()

    override suspend fun clientGetname(): K? = ops.clientGetname().awaitFirstOrNull()

    override suspend fun clientGetredir(): Long? = ops.clientGetredir().awaitFirstOrNull()

    override suspend fun clientId(): Long? = ops.clientId().awaitFirstOrNull()

    override suspend fun clientKill(addr: String): String? = ops.clientKill(addr).awaitFirstOrNull()

    override suspend fun clientKill(killArgs: KillArgs): Long? = ops.clientKill(killArgs).awaitFirstOrNull()

    override suspend fun clientList(): String? = ops.clientList().awaitFirstOrNull()

    override suspend fun clientPause(timeout: Long): String? = ops.clientPause(timeout).awaitFirstOrNull()

    override suspend fun clientSetname(name: K): String? = ops.clientSetname(name).awaitFirstOrNull()

    override suspend fun clientTracking(args: TrackingArgs): String? = ops.clientTracking(args).awaitFirstOrNull()

    override suspend fun clientUnblock(id: Long, type: UnblockType): Long? = ops.clientUnblock(id, type).awaitFirstOrNull()

    override suspend fun echo(msg: V): V = ops.echo(msg).awaitSingle()

    override suspend fun role(): List<Any> = ops.role().asFlow().toList()

    override suspend fun ping(): String = ops.ping().awaitSingle()

    override suspend fun readOnly(): String = ops.readOnly().awaitSingle()

    override suspend fun readWrite(): String = ops.readWrite().awaitSingle()

    override suspend fun quit(): String? = ops.quit().awaitFirstOrNull()

}
