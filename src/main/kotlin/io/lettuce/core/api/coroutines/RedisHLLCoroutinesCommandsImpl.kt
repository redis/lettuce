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
import io.lettuce.core.api.reactive.RedisHLLReactiveCommands
import kotlinx.coroutines.reactive.awaitFirstOrNull


/**
 * Coroutine executed commands (based on reactive commands) for HyperLogLog (PF* commands).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisHLLCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisHLLReactiveCommands<K, V>) : RedisHLLCoroutinesCommands<K, V> {

    override suspend fun pfadd(key: K, vararg values: V): Long? = ops.pfadd(key, *values).awaitFirstOrNull()

    override suspend fun pfmerge(destkey: K, vararg sourcekeys: K): String? = ops.pfmerge(destkey, *sourcekeys).awaitFirstOrNull()

    override suspend fun pfcount(vararg keys: K): Long? = ops.pfcount(*keys).awaitFirstOrNull()

}

