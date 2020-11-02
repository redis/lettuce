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

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.reactive.BaseRedisReactiveCommands
import io.lettuce.core.output.CommandOutput
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.protocol.ProtocolKeyword
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
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
internal class BaseRedisCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: BaseRedisReactiveCommands<K, V>) : BaseRedisCoroutinesCommands<K, V> {

    override suspend fun publish(channel: K, message: V): Long? = ops.publish(channel, message).awaitFirstOrNull()

    override suspend fun pubsubChannels(): List<K> = ops.pubsubChannels().asFlow().toList()

    override suspend fun pubsubChannels(channel: K): List<K> = ops.pubsubChannels(channel).asFlow().toList()

    override suspend fun pubsubNumsub(vararg channels: K): Map<K, Long> = ops.pubsubNumsub(*channels).awaitSingle()

    override suspend fun pubsubNumpat(): Long = ops.pubsubNumpat().awaitSingle()

    override suspend fun echo(msg: V): V = ops.echo(msg).awaitSingle()

    override suspend fun role(): List<Any> = ops.role().asFlow().toList()

    override suspend fun ping(): String = ops.ping().awaitSingle()

    override suspend fun readOnly(): String = ops.readOnly().awaitSingle()

    override suspend fun readWrite(): String = ops.readWrite().awaitSingle()

    override suspend fun quit(): String? = ops.quit().awaitFirstOrNull()

    override suspend fun waitForReplication(replicas: Int, timeout: Long): Long? = ops.waitForReplication(replicas, timeout).awaitFirstOrNull()

    override suspend fun <T> dispatch(type: ProtocolKeyword, output: CommandOutput<K, V, T>): T? = ops.dispatch<T>(type, output).awaitFirstOrNull()

    override suspend fun <T> dispatch(type: ProtocolKeyword, output: CommandOutput<K, V, T>, args: CommandArgs<K, V>): T? = ops.dispatch<T>(type, output, args).awaitFirstOrNull()

    override fun isOpen(): Boolean = ops.isOpen

    override fun setAutoFlushCommands(autoFlush: Boolean) = ops.setAutoFlushCommands(autoFlush)

    override fun flushCommands() = ops.flushCommands()

}

