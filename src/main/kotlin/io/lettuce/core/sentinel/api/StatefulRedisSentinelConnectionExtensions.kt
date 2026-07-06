package io.lettuce.core.sentinel.api

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.sentinel.api.coroutines.RedisSentinelCoroutinesCommands
import io.lettuce.core.sentinel.api.coroutines.RedisSentinelCoroutinesCommandsImpl
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands

/**
 * Extension for [StatefulRedisSentinelConnection] to create [RedisSentinelCoroutinesCommands]
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
fun <K : Any, V : Any> StatefulRedisSentinelConnection<K, V>.coroutines(): RedisSentinelCoroutinesCommands<K, V> = RedisSentinelCoroutinesCommandsImpl(commands(RedisSentinelReactiveCommands.factory<K, V>()))
