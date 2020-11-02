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

package io.lettuce.core.sentinel.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.KillArgs
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import java.net.SocketAddress

/**
 * Coroutine executed commands (based on reactive commands) for Redis Sentinel.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisSentinelCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisSentinelReactiveCommands<K, V>) : RedisSentinelCoroutinesCommands<K, V> {

    override suspend fun getMasterAddrByName(key: K): SocketAddress = ops.getMasterAddrByName(key).awaitLast()

    override suspend fun masters(): List<Map<K, V>> = ops.masters().asFlow().toList()

    override suspend fun master(key: K): Map<K, V> = ops.master(key).awaitLast()

    override suspend fun slaves(key: K): List<Map<K, V>> = ops.slaves(key).asFlow().toList()

    override suspend fun reset(key: K): Long = ops.reset(key).awaitLast()

    override suspend fun failover(key: K): String = ops.failover(key).awaitLast()

    override suspend fun monitor(key: K, ip: String, port: Int, quorum: Int): String = ops.monitor(key, ip, port, quorum).awaitLast()

    override suspend fun set(key: K, option: String, value: V): String = ops.set(key, option, value).awaitLast()

    override suspend fun remove(key: K): String = ops.remove(key).awaitLast()

    override suspend fun clientGetname(): K? = ops.clientGetname().awaitFirstOrNull()

    override suspend fun clientSetname(name: K): String = ops.clientSetname(name).awaitLast()

    override suspend fun clientKill(addr: String): String = ops.clientKill(addr).awaitLast()

    override suspend fun clientKill(killArgs: KillArgs): Long = ops.clientKill(killArgs).awaitLast()

    override suspend fun clientPause(timeout: Long): String = ops.clientPause(timeout).awaitLast()

    override suspend fun clientList(): String = ops.clientList().awaitLast()

    override suspend fun info(): String = ops.info().awaitLast()

    override suspend fun info(section: String): String = ops.info(section).awaitLast()

    override suspend fun ping(): String = ops.ping().awaitLast()

    override fun isOpen(): Boolean = ops.isOpen

}

