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
@file:Suppress("unused", "UsePropertyAccessSyntax")

package io.lettuce.core.api.coroutines.async

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines.*
import kotlinx.coroutines.future.await

/**
 * A complete reactive and thread-safe Redis API with 400+ Methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 5.0
 **/
@ExperimentalLettuceCoroutinesApi
open class RedisSuspendableAsyncCommandsImpl<K, V>(
        private val ops: RedisAsyncCommands<K, V>
) : RedisSuspendableCommands<K, V>,
        BaseRedisSuspendableCommands<K, V> by BaseRedisSuspendableAsyncCommands(ops),
        RedisGeoSuspendableCommands<K, V> by RedisGeoSuspendableAsyncCommands(ops),
        RedisHashSuspendableCommands<K, V> by RedisHashSuspendableAsyncCommands(ops),
        RedisHLLSuspendableCommands<K, V> by RedisHLLSuspendableAsyncCommands(ops),
        RedisKeySuspendableCommands<K, V> by RedisKeySuspendableAsyncCommands(ops),
        RedisListSuspendableCommands<K, V> by RedisListSuspendableAsyncCommands(ops),
        RedisScriptingSuspendableCommands<K, V> by RedisScriptingSuspendableAsyncCommands(ops),
        RedisServerSuspendableCommands<K, V> by RedisServerSuspendableAsyncCommands(ops),
        RedisSetSuspendableCommands<K, V> by RedisSetSuspendableAsyncCommands(ops),
        RedisSortedSetSuspendableCommands<K, V> by RedisSortedSetSuspendableAsyncCommands(ops),
        RedisStreamSuspendableCommands<K, V> by RedisStreamSuspendableAsyncCommands(ops),
        RedisStringSuspendableCommands<K, V> by RedisStringSuspendableAsyncCommands(ops),
        RedisTransactionalSuspendableCommands<K, V> by RedisTransactionalSuspendableAsyncCommands(ops) {

    /**
     * Authenticate to the server.
     *
     * @param password the password
     * @return String simple-string-reply
     */
    override suspend fun auth(password: CharSequence?): String? = ops.auth(password).await()

    /**
     * Authenticate to the server with username and password. Requires Redis 6 or newer.
     *
     * @param username the username
     * @param password the password
     * @return String simple-string-reply
     * @since 6.0
     */
    override suspend fun auth(username: String?, password: CharSequence?): String? = ops.auth(username, password).await()

    /**
     * Change the selected database for the current connection.
     *
     * @param db the database number
     * @return String simple-string-reply
     */
    override suspend fun select(db: Int): String? = ops.select(db).await()

    /**
     * Swap two Redis databases, so that immediately all the clients connected to a given DB will see the data of the other DB,
     * and the other way around
     *
     * @param db1 the first database number
     * @param db2 the second database number
     * @return String simple-string-reply
     */
    override suspend fun swapdb(db1: Int, db2: Int): String? = ops.swapdb(db1, db2).await()

    /**
     * @return the underlying connection.
     */
    override val statefulConnection: StatefulRedisConnection<K, V> = ops.statefulConnection
}