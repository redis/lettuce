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
package io.lettuce.core.cluster.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.*

/**
 * A complete coroutine and thread-safe Redis API with 400+ Methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
interface RedisClusterCoroutinesCommands<K : Any, V : Any> :
        BaseRedisCoroutinesCommands<K, V>,
        RedisGeoCoroutinesCommands<K, V>,
        RedisHashCoroutinesCommands<K, V>,
        RedisHLLCoroutinesCommands<K, V>,
        RedisKeyCoroutinesCommands<K, V>,
        RedisListCoroutinesCommands<K, V>,
        RedisScriptingCoroutinesCommands<K, V>,
        RedisServerCoroutinesCommands<K, V>,
        RedisSetCoroutinesCommands<K, V>,
        RedisSortedSetCoroutinesCommands<K, V>,
        RedisStreamCoroutinesCommands<K, V>,
        RedisStringCoroutinesCommands<K, V> {

    /**
     * Authenticate to the server.
     *
     * @param password the password
     * @return String simple-string-reply
     */
    suspend fun auth(password: CharSequence): String?

    /**
     * Authenticate to the server with username and password. Requires Redis 6 or newer.
     *
     * @param username the username
     * @param password the password
     * @return String simple-string-reply
     * @since 6.0
     */
    suspend fun auth(username: String, password: CharSequence): String?

}
