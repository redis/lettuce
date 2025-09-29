/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.reactive.RediSearchReactiveCommands
import io.lettuce.core.search.AggregationReply
import io.lettuce.core.search.SearchReply
import io.lettuce.core.search.SpellCheckResult
import io.lettuce.core.search.Suggestion
import io.lettuce.core.search.arguments.AggregateArgs
import io.lettuce.core.search.arguments.CreateArgs
import io.lettuce.core.search.arguments.ExplainArgs
import io.lettuce.core.search.arguments.FieldArgs
import io.lettuce.core.search.arguments.SearchArgs
import io.lettuce.core.search.arguments.SpellCheckArgs
import io.lettuce.core.search.arguments.SugAddArgs
import io.lettuce.core.search.arguments.SugGetArgs
import io.lettuce.core.search.arguments.SynUpdateArgs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
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

    override suspend fun ftAliasadd(alias: K, index: K): String? =
        ops.ftAliasadd(alias, index).awaitFirstOrNull()

    override suspend fun ftAliasupdate(alias: K, index: K): String? =
        ops.ftAliasupdate(alias, index).awaitFirstOrNull()

    override suspend fun ftAliasdel(alias: K): String? =
        ops.ftAliasdel(alias).awaitFirstOrNull()

    override suspend fun ftAlter(index: K, skipInitialScan: Boolean, fieldArgs: List<FieldArgs<K>>): String? =
        ops.ftAlter(index, skipInitialScan, fieldArgs).awaitFirstOrNull()

     override suspend fun ftTagvals(index: String, fieldName: K): List<V> =
         ops.ftTagvals(index, fieldName).asFlow().toList()

    override suspend fun ftAlter(index: K, fieldArgs: List<FieldArgs<K>>): String? =
        ops.ftAlter(index, fieldArgs).awaitFirstOrNull()

    override suspend fun ftDropindex(index: K, deleteDocuments: Boolean): String? =
        ops.ftDropindex(index, deleteDocuments).awaitFirstOrNull()

    override suspend fun ftDropindex(index: K): String? =
        ops.ftDropindex(index).awaitFirstOrNull()

    override suspend fun ftSearch(index: String, query: V): SearchReply<K, V>? =
        ops.ftSearch(index, query).awaitFirstOrNull()

    override suspend fun ftSearch(index: String, query: V, args: SearchArgs<K, V>): SearchReply<K, V>? =
        ops.ftSearch(index, query, args).awaitFirstOrNull()

    override suspend fun ftAggregate(index: String, query: V, args: AggregateArgs<K, V>): AggregationReply<K, V>? =
        ops.ftAggregate(index, query, args).awaitFirstOrNull()

    override suspend fun ftAggregate(index: String, query: V): AggregationReply<K, V>? =
        ops.ftAggregate(index, query).awaitFirstOrNull()

    override suspend fun ftCursorread(index: String, cursorId: Long): AggregationReply<K, V>? =
        ops.ftCursorread(index, cursorId).awaitFirstOrNull()

    override suspend fun ftCursorread(index: String, cursorId: Long, count: Int): AggregationReply<K, V>? =
        ops.ftCursorread(index, cursorId, count).awaitFirstOrNull()

    override suspend fun ftCursordel(index: String, cursorId: Long): String? {
        return ops.ftCursordel(index, cursorId).awaitFirstOrNull()
    }

    override suspend fun ftDictadd(dict: K, vararg terms: V): Long? =
        ops.ftDictadd(dict, *terms).awaitFirstOrNull()

    override suspend fun ftDictdel(dict: K, vararg terms: V): Long? =
        ops.ftDictdel(dict, *terms).awaitFirstOrNull()

    override suspend fun ftDictdump(dict: String): List<V> =
        ops.ftDictdump(dict).asFlow().toList()

    override suspend fun ftSpellcheck(index: String, query: V): SpellCheckResult<V>? =
        ops.ftSpellcheck(index, query).awaitFirstOrNull()

    override suspend fun ftSpellcheck(index: String, query: V, args: SpellCheckArgs<K, V>): SpellCheckResult<V>? =
        ops.ftSpellcheck(index, query, args).awaitFirstOrNull()

    override suspend fun ftSugadd(key: K, suggestion: V, score: Double): Long? =
        ops.ftSugadd(key, suggestion, score).awaitFirstOrNull()

    override suspend fun ftSugadd(key: K, suggestion: V, score: Double, args: SugAddArgs<K, V>): Long? =
        ops.ftSugadd(key, suggestion, score, args).awaitFirstOrNull()

    override suspend fun ftSugdel(key: K, suggestion: V): Boolean? =
        ops.ftSugdel(key, suggestion).awaitFirstOrNull()

    override suspend fun ftSugget(key: K, prefix: V): List<Suggestion<V>> =
        ops.ftSugget(key, prefix).asFlow().toList()

    override suspend fun ftSugget(key: K, prefix: V, args: SugGetArgs<K, V>): List<Suggestion<V>> =
        ops.ftSugget(key, prefix, args).asFlow().toList()

    override suspend fun ftSuglen(key: K): Long? =
        ops.ftSuglen(key).awaitFirstOrNull()

    override suspend fun ftSynupdate(index: K, synonymGroupId: V, vararg terms: V): String? =
        ops.ftSynupdate(index, synonymGroupId, *terms).awaitFirstOrNull()

    override suspend fun ftSynupdate(index: K, synonymGroupId: V, args: SynUpdateArgs<K, V>, vararg terms: V): String? =
        ops.ftSynupdate(index, synonymGroupId, args, *terms).awaitFirstOrNull()

    override suspend fun ftSyndump(index: String): Map<V, List<V>>? =
        ops.ftSyndump(index).awaitFirstOrNull()

    override suspend fun ftExplain(index: String, query: V): String? =
        ops.ftExplain(index, query).awaitFirstOrNull()

    override suspend fun ftExplain(index: String, query: V, args: ExplainArgs<K, V>): String? =
        ops.ftExplain(index, query, args).awaitFirstOrNull()

    override suspend fun ftList(): List<V> =
        ops.ftList().asFlow().toList()



}
