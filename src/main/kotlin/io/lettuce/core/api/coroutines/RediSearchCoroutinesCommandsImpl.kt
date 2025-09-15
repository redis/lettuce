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
import io.lettuce.core.search.AggregationReply.Cursor

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

    override suspend fun ftCreate(index: String, arguments: CreateArgs<K, V>, fieldArgs: List<FieldArgs<K>>): String? =
        ops.ftCreate(index, arguments, fieldArgs).awaitFirstOrNull()

    override suspend fun ftCreate(index: String, fieldArgs: List<FieldArgs<K>>): String? =
        ops.ftCreate(index, fieldArgs).awaitFirstOrNull()

    override suspend fun ftAliasadd(alias: String, index: String): String? =
        ops.ftAliasadd(alias, index).awaitFirstOrNull()

    override suspend fun ftAliasupdate(alias: String, index: String): String? =
        ops.ftAliasupdate(alias, index).awaitFirstOrNull()

    override suspend fun ftAliasdel(alias: String): String? =
        ops.ftAliasdel(alias).awaitFirstOrNull()

    override suspend fun ftAlter(index: String, skipInitialScan: Boolean, fieldArgs: List<FieldArgs<K>>): String? =
        ops.ftAlter(index, skipInitialScan, fieldArgs).awaitFirstOrNull()

     override suspend fun ftTagvals(index: String, fieldName: String): List<V> =
         ops.ftTagvals(index, fieldName).asFlow().toList()

    override suspend fun ftAlter(index: String, fieldArgs: List<FieldArgs<K>>): String? =
        ops.ftAlter(index, fieldArgs).awaitFirstOrNull()

    override suspend fun ftDropindex(index: String, deleteDocuments: Boolean): String? =
        ops.ftDropindex(index, deleteDocuments).awaitFirstOrNull()

    override suspend fun ftDropindex(index: String): String? =
        ops.ftDropindex(index).awaitFirstOrNull()

    override suspend fun ftSearch(index: String, query: V): SearchReply<K, V>? =
        ops.ftSearch(index, query).awaitFirstOrNull()

    override suspend fun ftSearch(index: String, query: V, args: SearchArgs<K, V>): SearchReply<K, V>? =
        ops.ftSearch(index, query, args).awaitFirstOrNull()

    override suspend fun ftAggregate(index: String, query: V, args: AggregateArgs<K, V>): AggregationReply<K, V>? =
        ops.ftAggregate(index, query, args).awaitFirstOrNull()

    override suspend fun ftAggregate(index: String, query: V): AggregationReply<K, V>? =
        ops.ftAggregate(index, query).awaitFirstOrNull()

    override suspend fun ftCursorread(index: String, cursor: Cursor, count: Int): AggregationReply<K, V>? =
        ops.ftCursorread(index, cursor, count).awaitFirstOrNull()

    override suspend fun ftCursorread(index: String, cursor: Cursor): AggregationReply<K, V>? =
        ops.ftCursorread(index, cursor).awaitFirstOrNull()

    override suspend fun ftCursordel(index: String, cursor: Cursor): String? =
        ops.ftCursordel(index, cursor).awaitFirstOrNull()

    override suspend fun ftDictadd(dict: String, vararg terms: V): Long? =
        ops.ftDictadd(dict, *terms).awaitFirstOrNull()

    override suspend fun ftDictdel(dict: String, vararg terms: V): Long? =
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

    override suspend fun ftSynupdate(index: String, synonymGroupId: V, vararg terms: V): String? =
        ops.ftSynupdate(index, synonymGroupId, *terms).awaitFirstOrNull()

    override suspend fun ftSynupdate(index: String, synonymGroupId: V, args: SynUpdateArgs<K, V>, vararg terms: V): String? =
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
