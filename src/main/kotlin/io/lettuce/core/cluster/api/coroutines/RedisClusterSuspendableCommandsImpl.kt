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
@file:Suppress("unused", "UsePropertyAccessSyntax")

package io.lettuce.core.cluster.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.*
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Implementation of [RedisClusterSuspendableCommands].
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.0
 **/
@ExperimentalLettuceCoroutinesApi
internal class RedisClusterSuspendableCommandsImpl<K : Any, V : Any>(
        private val ops: RedisClusterReactiveCommands<K, V>
) : RedisClusterSuspendableCommands<K, V>,
        BaseRedisSuspendableCommands<K, V> by BaseRedisSuspendableCommandsImpl(ops),
        RedisGeoSuspendableCommands<K, V> by RedisGeoSuspendableCommandsImpl(ops),
        RedisHashSuspendableCommands<K, V> by RedisHashSuspendableCommandsImpl(ops),
        RedisHLLSuspendableCommands<K, V> by RedisHLLSuspendableCommandsImpl(ops),
        RedisKeySuspendableCommands<K, V> by RedisKeySuspendableCommandsImpl(ops),
        RedisListSuspendableCommands<K, V> by RedisListSuspendableCommandsImpl(ops),
        RedisScriptingSuspendableCommands<K, V> by RedisScriptingSuspendableCommandsImpl(ops),
        RedisServerSuspendableCommands<K, V> by RedisServerSuspendableCommandsImpl(ops),
        RedisSetSuspendableCommands<K, V> by RedisSetSuspendableCommandsImpl(ops),
        RedisSortedSetSuspendableCommands<K, V> by RedisSortedSetSuspendableCommandsImpl(ops),
        RedisStreamSuspendableCommands<K, V> by RedisStreamSuspendableCommandsImpl(ops),
        RedisStringSuspendableCommands<K, V> by RedisStringSuspendableCommandsImpl(ops) {

    /**
     * Authenticate to the server.
     *
     * @param password the password
     * @return String simple-string-reply
     */
    override suspend fun auth(password: CharSequence): String? = ops.auth(password).awaitFirstOrNull()

    /**
     * Authenticate to the server with username and password. Requires Redis 6 or newer.
     *
     * @param username the username
     * @param password the password
     * @return String simple-string-reply
     * @since 6.0
     */
    override suspend fun auth(username: String, password: CharSequence): String? = ops.auth(username, password).awaitFirstOrNull()

}
