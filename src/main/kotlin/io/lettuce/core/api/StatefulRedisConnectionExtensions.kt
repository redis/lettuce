package io.lettuce.core.api

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.api.coroutines.RedisCoroutinesCommandsImpl

/**
 * Extension for [StatefulRedisConnection] to create [RedisCoroutinesCommands]
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
fun <K : Any, V : Any> StatefulRedisConnection<K, V>.coroutines(): RedisCoroutinesCommands<K, V> = RedisCoroutinesCommandsImpl(reactive())
