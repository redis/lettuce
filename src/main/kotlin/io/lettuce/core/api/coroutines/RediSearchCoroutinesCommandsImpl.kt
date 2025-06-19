/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.reactive.RediSearchReactiveCommands
import io.lettuce.core.search.SearchReply
import io.lettuce.core.search.arguments.AggregateArgs
import io.lettuce.core.search.arguments.CreateArgs
import io.lettuce.core.search.arguments.FieldArgs
import io.lettuce.core.search.arguments.SearchArgs
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for RediSearch.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.8
 */
@ExperimentalLettuceCoroutinesApi
open class RediSearchCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RediSearchReactiveCommands<K, V>) :
    RediSearchCoroutinesCommands<K, V> {

    override suspend fun ftCreate(index: K, arguments: CreateArgs<K, V>, fieldArgs: List<FieldArgs<K>>): String? =
        ops.ftCreate(index, arguments, fieldArgs).awaitFirstOrNull()

    override suspend fun ftCreate(index: K, fieldArgs: List<FieldArgs<K>>): String? =
        ops.ftCreate(index, fieldArgs).awaitFirstOrNull()

    override suspend fun ftDropindex(index: K, deleteDocuments: Boolean): String? =
        ops.ftDropindex(index, deleteDocuments).awaitFirstOrNull()

    override suspend fun ftDropindex(index: K): String? =
        ops.ftDropindex(index).awaitFirstOrNull()

    override suspend fun ftSearch(index: K, query: V): SearchReply<K, V>? =
        ops.ftSearch(index, query).awaitFirstOrNull()

    override suspend fun ftSearch(index: K, query: V, args: SearchArgs<K, V>): SearchReply<K, V>? =
        ops.ftSearch(index, query, args).awaitFirstOrNull()

    override suspend fun ftAggregate(index: K, query: V, args: AggregateArgs<K, V>): SearchReply<K, V>? {
        return ops.ftAggregate(index, query, args).awaitFirstOrNull()
    }

    override suspend fun ftAggregate(index: K, query: V): SearchReply<K, V>? {
        return ops.ftAggregate(index, query).awaitFirstOrNull()
    }
}
