package io.lettuce.core.api.reactive

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisSuspendableCommands
import io.lettuce.core.api.coroutines.reactive.RedisSuspendableReactiveCommandsImpl

/**
 * Extension for [RedisReactiveCommands] to create [RedisSuspendableCommands]
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
fun <K, V> RedisReactiveCommands<K, V>.asSuspendable(): RedisSuspendableCommands<K, V> {
    return RedisSuspendableReactiveCommandsImpl(this)
}