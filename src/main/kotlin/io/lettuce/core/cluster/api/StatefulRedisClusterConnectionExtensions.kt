package io.lettuce.core.cluster.api

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.cluster.api.coroutines.RedisClusterCoroutinesCommands
import io.lettuce.core.cluster.api.coroutines.RedisClusterCoroutinesCommandsImpl

/**
 * Extension for [StatefulRedisClusterConnection] to create [RedisClusterCoroutinesCommands]
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
fun <K : Any, V : Any> StatefulRedisClusterConnection<K, V>.coroutines(): RedisClusterCoroutinesCommands<K, V> = RedisClusterCoroutinesCommandsImpl(reactive())
