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

package io.lettuce.core.api.coroutines.reactive

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines.*
import io.lettuce.core.api.reactive.RedisReactiveCommands
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * A complete reactive and thread-safe Redis API with 400+ Methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 **/
@ExperimentalLettuceCoroutinesApi
open class RedisSuspendableReactiveCommandsImpl<K, V>(
        private val ops: RedisReactiveCommands<K, V>,
) : RedisSuspendableCommands<K, V>,
        BaseRedisSuspendableCommands<K, V> by BaseRedisSuspendableReactiveCommands(ops),
        RedisGeoSuspendableCommands<K, V> by RedisGeoSuspendableReactiveCommands(ops),
        RedisHashSuspendableCommands<K, V> by RedisHashSuspendableReactiveCommands(ops),
        RedisHLLSuspendableCommands<K, V> by RedisHLLSuspendableReactiveCommands(ops),
        RedisKeySuspendableCommands<K, V> by RedisKeySuspendableReactiveCommands(ops),
        RedisListSuspendableCommands<K, V> by RedisListSuspendableReactiveCommands(ops),
        RedisScriptingSuspendableCommands<K, V> by RedisScriptingSuspendableReactiveCommands(ops),
        RedisServerSuspendableCommands<K, V> by RedisServerSuspendableReactiveCommands(ops),
        RedisSetSuspendableCommands<K, V> by RedisSetSuspendableReactiveCommands(ops),
        RedisSortedSetSuspendableCommands<K, V> by RedisSortedSetSuspendableReactiveCommands(ops),
        RedisStreamSuspendableCommands<K, V> by RedisStreamSuspendableReactiveCommands(ops),
        RedisStringSuspendableCommands<K, V> by RedisStringSuspendableReactiveCommands(ops),
        RedisTransactionalSuspendableCommands<K, V> by RedisTransactionalSuspendableReactiveCommands(ops) {

    /**
     * Authenticate to the server.
     *
     * @param password the password
     * @return String simple-string-reply
     */
    override suspend fun auth(password: CharSequence?): String? = ops.auth(password).awaitFirstOrNull()

    /**
     * Authenticate to the server with username and password. Requires Redis 6 or newer.
     *
     * @param username the username
     * @param password the password
     * @return String simple-string-reply
     * @since 6.0
     */
    override suspend fun auth(username: String?, password: CharSequence?): String? = ops.auth(username, password).awaitFirstOrNull()

    /**
     * Change the selected database for the current connection.
     *
     * @param db the database number
     * @return String simple-string-reply
     */
    override suspend fun select(db: Int): String? = ops.select(db).awaitFirstOrNull()

    /**
     * Swap two Redis databases, so that immediately all the clients connected to a given DB will see the data of the other DB,
     * and the other way around
     *
     * @param db1 the first database number
     * @param db2 the second database number
     * @return String simple-string-reply
     */
    override suspend fun swapdb(db1: Int, db2: Int): String? = ops.swapdb(db1, db2).awaitFirstOrNull()

    /**
     * @return the underlying connection.
     */
    override val statefulConnection: StatefulRedisConnection<K, V> = ops.statefulConnection
}