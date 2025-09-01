/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.api.coroutines

import io.lettuce.core.*
import io.lettuce.core.api.reactive.RedisJsonReactiveCommands
import io.lettuce.core.json.JsonPath
import io.lettuce.core.json.JsonType
import io.lettuce.core.json.JsonValue
import io.lettuce.core.json.arguments.JsonGetArgs
import io.lettuce.core.json.arguments.JsonMsetArgs
import io.lettuce.core.json.arguments.JsonRangeArgs
import io.lettuce.core.json.arguments.JsonSetArgs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutine executed commands (based on reactive commands) for Keys (Key manipulation/querying).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.5
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisJsonCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisJsonReactiveCommands<K, V>) :
    RedisJsonCoroutinesCommands<K, V> {
    override suspend fun jsonArrappend(key: K, jsonPath: JsonPath, vararg values: JsonValue): List<Long> =
        ops.jsonArrappend(key, jsonPath, *values).asFlow().toList()


    override suspend fun jsonArrappend(key: K, jsonPath: JsonPath, vararg jsonStrings: String): List<Long> =
        ops.jsonArrappend(key, jsonPath, *jsonStrings).asFlow().toList()

    override suspend fun jsonArrappend(key: K, vararg values: JsonValue): List<Long> =
        ops.jsonArrappend(key, *values).asFlow().toList()


    override suspend fun jsonArrappend(key: K, vararg jsonStrings: String): List<Long> =
        ops.jsonArrappend(key, *jsonStrings).asFlow().toList()

    override suspend fun jsonArrindex(
        key: K,
        jsonPath: JsonPath,
        value: JsonValue,
        range: JsonRangeArgs
    ): List<Long> = ops.jsonArrindex(key, jsonPath, value, range).asFlow().toList()

    override suspend fun jsonArrindex(key: K, jsonPath: JsonPath, value: JsonValue): List<Long> =
        ops.jsonArrindex(key, jsonPath, value).asFlow().toList()


    override suspend fun jsonArrindex(key: K, jsonPath: JsonPath, jsonString: String): List<Long> =
        ops.jsonArrindex(key, jsonPath, jsonString).asFlow().toList()


    override suspend fun jsonArrindex(
        key: K,
        jsonPath: JsonPath,
        jsonString: String,
        range: JsonRangeArgs
    ): List<Long> = ops.jsonArrindex(key, jsonPath, jsonString, range).asFlow().toList()

    override suspend fun jsonArrinsert(
        key: K,
        jsonPath: JsonPath,
        index: Int,
        vararg values: JsonValue
    ): List<Long> = ops.jsonArrinsert(key, jsonPath, index, *values).asFlow().toList()


    override suspend fun jsonArrinsert(
        key: K,
        jsonPath: JsonPath,
        index: Int,
        vararg jsonStrings: String
    ): List<Long> = ops.jsonArrinsert(key, jsonPath, index, *jsonStrings).asFlow().toList()

    override suspend fun jsonArrlen(key: K, jsonPath: JsonPath): List<Long> =
        ops.jsonArrlen(key, jsonPath).asFlow().toList()

    override suspend fun jsonArrlen(key: K): List<Long> = ops.jsonArrlen(key).asFlow().toList()

    override suspend fun jsonArrpop(key: K, jsonPath: JsonPath, index: Int): List<JsonValue> =
        ops.jsonArrpop(key, jsonPath, index).asFlow().toList()

    override suspend fun jsonArrpop(key: K, jsonPath: JsonPath): List<JsonValue> =
        ops.jsonArrpop(key, jsonPath).asFlow().toList()

    override suspend fun jsonArrpop(key: K): List<JsonValue> = ops.jsonArrpop(key).asFlow().toList()

    override suspend fun jsonArrtrim(key: K, jsonPath: JsonPath, range: JsonRangeArgs): List<Long> =
        ops.jsonArrtrim(key, jsonPath, range).asFlow().toList()

    override suspend fun jsonClear(key: K, jsonPath: JsonPath): Long? =
        ops.jsonClear(key, jsonPath).awaitFirstOrNull()

    override suspend fun jsonClear(key: K): Long? = ops.jsonClear(key).awaitFirstOrNull()

    override suspend fun jsonDel(key: K, jsonPath: JsonPath): Long? =
        ops.jsonDel(key, jsonPath).awaitFirstOrNull()

    override suspend fun jsonDel(key: K): Long? = ops.jsonDel(key).awaitFirstOrNull()

    override suspend fun jsonGet(key: K, options: JsonGetArgs, vararg jsonPaths: JsonPath): List<JsonValue> =
        ops.jsonGet(key, options, *jsonPaths).asFlow().toList()

    override suspend fun jsonGet(key: K, vararg jsonPaths: JsonPath): List<JsonValue> =
        ops.jsonGet(key, *jsonPaths).asFlow().toList()

    override suspend fun jsonMerge(key: K, jsonPath: JsonPath, value: JsonValue): String? =
        ops.jsonMerge(key, jsonPath, value).awaitFirstOrNull()

    override suspend fun jsonMerge(key: K, jsonPath: JsonPath, jsonString: String): String? =
        ops.jsonMerge(key, jsonPath, jsonString).awaitFirstOrNull()


    override suspend fun jsonMGet(jsonPath: JsonPath, vararg keys: K): List<JsonValue> =
        ops.jsonMGet(jsonPath, *keys).asFlow().toList()

    override suspend fun jsonMSet(arguments: List<JsonMsetArgs<K, V>>): String? =
        ops.jsonMSet(arguments).awaitFirstOrNull()

    override suspend fun jsonType(key: K, jsonPath: JsonPath): List<JsonType> =
        ops.jsonType(key, jsonPath).asFlow().toList()

    override suspend fun jsonType(key: K): List<JsonType> = ops.jsonType(key).asFlow().toList()

    override suspend fun jsonToggle(key: K, jsonPath: JsonPath): List<Long> =
        ops.jsonToggle(key, jsonPath).asFlow().toList()

    override suspend fun jsonStrlen(key: K, jsonPath: JsonPath): List<Long> =
        ops.jsonStrlen(key, jsonPath).asFlow().toList()

    override suspend fun jsonStrlen(key: K): List<Long> = ops.jsonStrlen(key).asFlow().toList()

    override suspend fun jsonStrappend(key: K, jsonPath: JsonPath, value: JsonValue): List<Long> =
        ops.jsonStrappend(key, jsonPath, value).asFlow().toList()


    override suspend fun jsonStrappend(key: K, jsonPath: JsonPath, jsonString: String): List<Long> =
        ops.jsonStrappend(key, jsonPath, jsonString).asFlow().toList()

    override suspend fun jsonStrappend(key: K, value: JsonValue): List<Long> =
        ops.jsonStrappend(key, value).asFlow().toList()

    override suspend fun jsonStrappend(key: K, jsonString: String): List<Long> =
        ops.jsonStrappend(key, jsonString).asFlow().toList()



    override suspend fun jsonSet(key: K, jsonPath: JsonPath, jsonString: String): String? =
        ops.jsonSet(key, jsonPath, jsonString).awaitFirstOrNull()

    override suspend fun jsonSet(key: K, jsonPath: JsonPath, value: JsonValue, options: JsonSetArgs): String? =
        ops.jsonSet(key, jsonPath, value, options).awaitFirstOrNull()

    override suspend fun jsonSet(key: K, jsonPath: JsonPath, value: JsonValue): String? =
        ops.jsonSet(key, jsonPath, value).awaitFirstOrNull()

    override suspend fun jsonSet(key: K, jsonPath: JsonPath, jsonString: String, options: JsonSetArgs): String? =
        ops.jsonSet(key, jsonPath, jsonString, options).awaitFirstOrNull()


    override suspend fun jsonObjlen(key: K, jsonPath: JsonPath): List<Long> =
        ops.jsonObjlen(key, jsonPath).asFlow().toList()

    override suspend fun jsonObjlen(key: K): List<Long> = ops.jsonObjlen(key).asFlow().toList()

    override suspend fun jsonObjkeys(key: K, jsonPath: JsonPath): List<V> =
        ops.jsonObjkeys(key, jsonPath).asFlow().toList()

    override suspend fun jsonObjkeys(key: K): List<V> = ops.jsonObjkeys(key).asFlow().toList()

    override suspend fun jsonNumincrby(key: K, jsonPath: JsonPath, number: Number): List<Number> =
        ops.jsonNumincrby(key, jsonPath, number).asFlow().toList()
}

