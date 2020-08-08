package io.lettuce.core.api.sync

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.TransactionResult

/**
 * Allows to create transaction DSL block with [RedisCommands].
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
inline fun <K, V> RedisCommands<K, V>.multi(action: RedisCommands<K, V>.() -> Unit): TransactionResult? {
    multi()
    runCatching {
        action.invoke(this)
    }.onFailure {
        discard()
    }
    return exec()
}