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
import io.lettuce.core.TransactionResult
import io.lettuce.core.api.reactive.RedisTransactionalReactiveCommands
import kotlinx.coroutines.reactive.awaitLast


/**
 * Coroutine executed commands (based on reactive commands) for Transactions.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisTransactionalCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisTransactionalReactiveCommands<K, V>) : RedisTransactionalCoroutinesCommands<K, V> {

    override suspend fun discard(): String = ops.discard().awaitLast()

    override suspend fun exec(): TransactionResult = ops.exec().awaitLast()

    override suspend fun multi(): String = ops.multi().awaitLast()

    override suspend fun watch(vararg keys: K): String = ops.watch(*keys).awaitLast()

    override suspend fun unwatch(): String = ops.unwatch().awaitLast()

}

