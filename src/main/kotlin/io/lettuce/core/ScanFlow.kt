package io.lettuce.core

import io.lettuce.core.api.coroutines.*
import io.lettuce.core.cluster.api.coroutines.RedisClusterCoroutinesCommandsImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow

object ScanFlow {
    /**
     * Sequentially iterate the keys space.
     *
     * @param commands coroutines commands
     * @param scanArgs scan arguments.
     * @return `Flow<K>` flow of keys.
     */
    @JvmOverloads
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
    @JvmOverloads
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
    @JvmOverloads
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
}