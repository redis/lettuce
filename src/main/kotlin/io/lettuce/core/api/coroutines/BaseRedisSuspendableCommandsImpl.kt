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
import io.lettuce.core.api.reactive.BaseRedisReactiveCommands
import io.lettuce.core.output.CommandOutput
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.protocol.ProtocolKeyword
import kotlinx.coroutines.reactive.awaitFirstOrNull


/**
 * Coroutine executed commands (based on reactive commands) for basic commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
internal class BaseRedisSuspendableCommandsImpl<K, V>(private val ops: BaseRedisReactiveCommands<K, V>) : BaseRedisSuspendableCommands<K, V> {

    override suspend fun publish(channel: K, message: V): Long? = ops.publish(channel, message).awaitFirstOrNull()

    override suspend fun pubsubChannels(): List<K>? = ops.pubsubChannels().collectList().awaitFirstOrNull()

    override suspend fun pubsubChannels(channel: K): List<K>? = ops.pubsubChannels(channel).collectList().awaitFirstOrNull()

    override suspend fun pubsubNumsub(vararg channels: K): Map<K, Long>? = ops.pubsubNumsub(*channels).awaitFirstOrNull()

    override suspend fun pubsubNumpat(): Long? = ops.pubsubNumpat().awaitFirstOrNull()

    override suspend fun echo(msg: V): V? = ops.echo(msg).awaitFirstOrNull()

    override suspend fun role(): List<Any>? = ops.role().collectList().awaitFirstOrNull()

    override suspend fun ping(): String? = ops.ping().awaitFirstOrNull()

    override suspend fun readOnly(): String? = ops.readOnly().awaitFirstOrNull()

    override suspend fun readWrite(): String? = ops.readWrite().awaitFirstOrNull()

    override suspend fun quit(): String? = ops.quit().awaitFirstOrNull()

    override suspend fun waitForReplication(replicas: Int, timeout: Long): Long? = ops.waitForReplication(replicas, timeout).awaitFirstOrNull()

    override suspend fun <T> dispatch(type: ProtocolKeyword, output: CommandOutput<K, V, T>): T? = ops.dispatch<T>(type, output).awaitFirstOrNull()

    override suspend fun <T> dispatch(type: ProtocolKeyword, output: CommandOutput<K, V, T>, args: CommandArgs<K, V>): T? = ops.dispatch<T>(type, output, args).awaitFirstOrNull()

    override fun isOpen(): Boolean = ops.isOpen()

    override fun setAutoFlushCommands(autoFlush: Boolean): Unit = ops.setAutoFlushCommands(autoFlush)

    override fun flushCommands(): Unit = ops.flushCommands()

}

