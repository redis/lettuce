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

/**
 * Allows to create transaction DSL block with [RedisCoroutinesCommands].
 *
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
suspend inline fun <K : Any, V : Any> RedisCoroutinesCommands<K, V>.multi(action: RedisCoroutinesCommands<K, V>.() -> Unit): TransactionResult? = try {
    multi()
    action.invoke(this)
    exec()
} catch (thr: Throwable) {
    discard()
    throw thr
}
