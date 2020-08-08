package io.lettuce.core.api.async

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.TransactionResult
import io.lettuce.core.api.coroutines.RedisSuspendableCommands
import io.lettuce.core.api.coroutines.async.RedisSuspendableAsyncCommandsImpl
import kotlinx.coroutines.future.await


/**
 * Extension for [RedisAsyncCommands] to create [RedisSuspendableCommands]
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
fun <K, V> RedisAsyncCommands<K, V>.asSuspendable(): RedisSuspendableCommands<K, V> {
    return RedisSuspendableAsyncCommandsImpl(this)
}

/**
 * Allows to create transaction DSL block with [RedisAsyncCommands].
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
suspend inline fun <K, V> RedisAsyncCommands<K, V>.multi(action: RedisAsyncCommands<K, V>.() -> Unit): TransactionResult? {
    multi().await()
    runCatching {
        action.invoke(this)
    }.onFailure {
        discard()
    }
    return exec().await()
}