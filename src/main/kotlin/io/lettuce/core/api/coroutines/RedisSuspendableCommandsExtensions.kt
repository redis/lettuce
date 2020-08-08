package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.TransactionResult

/**
 * Allows to create transaction DSL block with [RedisSuspendableCommands].
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
suspend inline fun <K, V> RedisSuspendableCommands<K, V>.multi(action: RedisSuspendableCommands<K, V>.() -> Unit): TransactionResult? {
    multi()
    runCatching {
        action.invoke(this)
    }.onFailure {
        discard()
    }
    return exec()
}