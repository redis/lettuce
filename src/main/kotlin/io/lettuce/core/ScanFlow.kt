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
package io.lettuce.core

import io.lettuce.core.api.coroutines.*
import io.lettuce.core.cluster.api.coroutines.RedisClusterCoroutinesCommandsImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow

/**
 * Coroutines adapter for [ScanStream].
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.1
 */
@ExperimentalLettuceCoroutinesApi
object ScanFlow {

    /**
     * Sequentially iterate the keys space.
     *
     * @param commands coroutines commands
     * @param scanArgs scan arguments.
     * @return `Flow<K>` flow of keys.
     */
    fun <K : Any, V : Any> scan(commands: RedisKeyCoroutinesCommands<K, V>, scanArgs: ScanArgs? = null): Flow<K> {
        val ops = when (commands) {
            is RedisCoroutinesCommandsImpl -> commands.ops
            is RedisClusterCoroutinesCommandsImpl -> commands.ops
            is RedisKeyCoroutinesCommandsImpl -> commands.ops
            else -> throw IllegalArgumentException("Cannot access underlying reactive API")
        }
        return when (scanArgs) {
            null -> ScanStream.scan(ops)
            else -> ScanStream.scan(ops, scanArgs)
        }.asFlow()
    }

    /**
     * Sequentially iterate hash fields and associated values.
     *
     * @param commands coroutines commands
     * @param key the key.
     * @param scanArgs scan arguments.
     * @return `Flow<KeyValue<K, V>>` flow of key-values.
     */
    fun <K : Any, V : Any> hscan(commands: RedisHashCoroutinesCommands<K, V>, key: K, scanArgs: ScanArgs? = null): Flow<KeyValue<K, V>> {
        val ops = when (commands) {
            is RedisCoroutinesCommandsImpl -> commands.ops
            is RedisClusterCoroutinesCommandsImpl -> commands.ops
            is RedisHashCoroutinesCommandsImpl -> commands.ops
            else -> throw IllegalArgumentException("Cannot access underlying reactive API")
        }
        return when (scanArgs) {
            null -> ScanStream.hscan(ops, key)
            else -> ScanStream.hscan(ops, key, scanArgs)
        }.asFlow()
    }

    /**
     * Sequentially iterate Set elements.
     *
     * @param commands coroutines commands
     * @param key the key.
     * @param scanArgs scan arguments.
     * @return `Flow<V>` flow of value.
     */
    fun <K : Any, V : Any> sscan(commands: RedisSetCoroutinesCommands<K, V>, key: K, scanArgs: ScanArgs? = null): Flow<V> {
        val ops = when (commands) {
            is RedisCoroutinesCommandsImpl -> commands.ops
            is RedisClusterCoroutinesCommandsImpl -> commands.ops
            is RedisSetCoroutinesCommandsImpl -> commands.ops
            else -> throw IllegalArgumentException("Cannot access underlying reactive API")
        }
        return when (scanArgs) {
            null -> ScanStream.sscan(ops, key)
            else -> ScanStream.sscan(ops, key, scanArgs)
        }.asFlow()
    }

    /**
     * Sequentially iterate Sorted Set elements.
     *
     * @param commands coroutines commands
     * @param key the key.
     * @param scanArgs scan arguments.
     * @return `Flow<V>` flow of [ScoredValue].
     */
    fun <K : Any, V : Any> zscan(commands: RedisSortedSetCoroutinesCommands<K, V>, key: K, scanArgs: ScanArgs? = null): Flow<ScoredValue<V>> {
        val ops = when (commands) {
            is RedisCoroutinesCommandsImpl -> commands.ops
            is RedisClusterCoroutinesCommandsImpl -> commands.ops
            is RedisSortedSetCoroutinesCommandsImpl -> commands.ops
            else -> throw IllegalArgumentException("Cannot access underlying reactive API")
        }
        return when (scanArgs) {
            null -> ScanStream.zscan(ops, key)
            else -> ScanStream.zscan(ops, key, scanArgs)
        }.asFlow()
    }
}
