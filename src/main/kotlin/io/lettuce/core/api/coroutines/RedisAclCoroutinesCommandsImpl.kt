/*
 * Copyright 2021-2022 the original author or authors.
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

import io.lettuce.core.AclCategory
import io.lettuce.core.AclSetuserArgs
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.reactive.RedisAclReactiveCommands
import io.lettuce.core.protocol.CommandType
import io.lettuce.core.protocol.RedisCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for basic commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisAclCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisAclReactiveCommands<K, V>) : RedisAclCoroutinesCommands<K, V> {

    override suspend fun aclCat(): Set<AclCategory> =
        ops.aclCat().awaitFirstOrElse { emptySet<AclCategory>() }

    override suspend fun aclCat(category: AclCategory): Set<CommandType> =
        ops.aclCat(category).awaitFirstOrElse { emptySet<CommandType>() }

    override suspend fun aclDeluser(vararg usernames: String): Long? =
        ops.aclDeluser(*usernames).awaitFirstOrNull()

    override suspend fun aclDryRun(
        username: String,
        command: String,
        vararg args: String
    ): String? = ops.aclDryRun(username, command, *args).awaitFirstOrNull()

    override suspend fun aclDryRun(
        username: String,
        command: RedisCommand<K, V, *>
    ): String? = ops.aclDryRun(username, command).awaitFirstOrNull()

    override suspend fun aclGenpass(): String? = ops.aclGenpass().awaitFirstOrNull()

    override suspend fun aclGenpass(bits: Int): String? =
        ops.aclGenpass(bits).awaitFirstOrNull()

    override suspend fun aclGetuser(username: String): List<Any> =
        ops.aclGetuser(username).awaitFirst()

    override fun aclList(): Flow<String> = ops.aclList().asFlow()

    override suspend fun aclLoad(): String? = ops.aclLoad().awaitFirstOrNull()

    override fun aclLog(): Flow<Map<String, Any>> = ops.aclLog().asFlow()

    override fun aclLog(count: Int): Flow<Map<String, Any>> = ops.aclLog(count).asFlow()

    override suspend fun aclLogReset(): String? = ops.aclLogReset().awaitFirstOrNull()

    override suspend fun aclSave(): String? = ops.aclSave().awaitFirstOrNull()

    override suspend fun aclSetuser(username: String, setuserArgs: AclSetuserArgs): String? = ops.aclSetuser(username, setuserArgs).awaitFirstOrNull()

    override suspend fun aclUsers(): List<String> = ops.aclUsers().asFlow().toList()

    override suspend fun aclWhoami(): String? = ops.aclWhoami().awaitFirstOrNull()

}

