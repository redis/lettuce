/*
 * Copyright 2011-2020 the original author or authors.
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
import io.lettuce.core.cluster.api.coroutines.RedisClusterCoroutinesCommands

/**
 * A complete coroutine and thread-safe Redis API with 400+ Methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
interface RedisCoroutinesCommands<K : Any, V : Any> :
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
        RedisStringCoroutinesCommands<K, V>,
        RedisTransactionalCoroutinesCommands<K, V>,
        RedisClusterCoroutinesCommands<K, V> {

    /**
     * Authenticate to the server.
     *
     * @param password the password
     * @return String simple-string-reply
     */
    override suspend fun auth(password: CharSequence): String?

    /**
     * Authenticate to the server with username and password. Requires Redis 6 or newer.
     *
     * @param username the username
     * @param password the password
     * @return String simple-string-reply
     * @since 6.0
     */
    override suspend fun auth(username: String, password: CharSequence): String?

    /**
     * Change the selected database for the current connection.
     *
     * @param db the database number
     * @return String simple-string-reply
     */
    suspend fun select(db: Int): String?

    /**
     * Swap two Redis databases, so that immediately all the clients connected to a given DB will see the data of the other DB,
     * and the other way around
     *
     * @param db1 the first database number
     * @param db2 the second database number
     * @return String simple-string-reply
     */
    suspend fun swapdb(db1: Int, db2: Int): String?

}
