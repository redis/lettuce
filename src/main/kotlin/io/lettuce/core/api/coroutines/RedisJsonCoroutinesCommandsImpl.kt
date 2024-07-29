/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import io.lettuce.core.*
import io.lettuce.core.api.reactive.RedisJsonReactiveCommands
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands
import io.lettuce.core.json.JsonPath
import io.lettuce.core.json.JsonValue
import io.lettuce.core.json.arguments.JsonGetArgs
import io.lettuce.core.json.arguments.JsonMsetArgs
import io.lettuce.core.json.arguments.JsonRangeArgs
import io.lettuce.core.json.arguments.JsonSetArgs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import java.time.Duration
import java.time.Instant
import java.util.*

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
    override suspend fun jsonArrappend(key: K, jsonPath: JsonPath, vararg values: JsonValue<V>): List<Long> =
        ops.jsonArrappend(key, jsonPath, *values).asFlow().toList()

    override suspend fun jsonArrindex(
        key: K,
        jsonPath: JsonPath,
        value: JsonValue<V>,
        range: JsonRangeArgs
    ): List<Long> = ops.jsonArrindex(key, jsonPath, value, range).asFlow().toList()

    override suspend fun jsonArrinsert(
        key: K,
        jsonPath: JsonPath,
        index: Int,
        vararg values: JsonValue<V>
    ): List<Long> = ops.jsonArrinsert(key, jsonPath, index, *values).asFlow().toList()

    override suspend fun jsonArrlen(key: K, jsonPath: JsonPath): List<Long> =
        ops.jsonArrlen(key, jsonPath).asFlow().toList()

    override suspend fun jsonArrpop(key: K, jsonPath: JsonPath, index: Int): List<JsonValue<V>> =
        ops.jsonArrpop(key, jsonPath, index).asFlow().toList()

    override suspend fun jsonArrtrim(key: K, jsonPath: JsonPath, range: JsonRangeArgs): List<Long> =
        ops.jsonArrtrim(key, jsonPath, range).asFlow().toList()

    override suspend fun jsonClear(key: K, jsonPath: JsonPath): Long? =
        ops.jsonClear(key, jsonPath).awaitFirstOrNull()

    override suspend fun jsonDel(key: K, jsonPath: JsonPath): Long? =
        ops.jsonDel(key, jsonPath).awaitFirstOrNull()

    override suspend fun jsonGet(key: K, options: JsonGetArgs, vararg jsonPaths: JsonPath): List<JsonValue<V>> =
        ops.jsonGet(key, options, *jsonPaths).asFlow().toList()

    override suspend fun jsonMerge(key: K, jsonPath: JsonPath, value: JsonValue<V>): Boolean? =
        ops.jsonMerge(key, jsonPath, value).awaitFirstOrNull()

    override suspend fun jsonMGet(jsonPath: JsonPath, vararg keys: K): List<JsonValue<V>> =
        ops.jsonMGet(jsonPath, *keys).asFlow().toList()

    override suspend fun jsonMSet(vararg arguments: JsonMsetArgs): Boolean? =
        ops.jsonMSet(*arguments).awaitFirstOrNull()

    override suspend fun jsonType(key: K, jsonPath: JsonPath): List<V> =
        ops.jsonType(key, jsonPath).asFlow().toList()

    override suspend fun jsonToggle(key: K, jsonPath: JsonPath): List<Boolean> =
        ops.jsonToggle(key, jsonPath).asFlow().toList()

    override suspend fun jsonStrlen(key: K, jsonPath: JsonPath): List<Long> =
        ops.jsonStrlen(key, jsonPath).asFlow().toList()

    override suspend fun jsonStrappend(key: K, jsonPath: JsonPath, value: JsonValue<V>): List<Long> =
        ops.jsonStrappend(key, jsonPath, value).asFlow().toList()

    override suspend fun jsonSet(key: K, jsonPath: JsonPath, value: JsonValue<V>, options: JsonSetArgs): Boolean? =
        ops.jsonSet(key, jsonPath, value, options).awaitFirstOrNull()

    override suspend fun jsonObjlen(key: K, jsonPath: JsonPath): List<Long> =
        ops.jsonObjlen(key, jsonPath).asFlow().toList()

    override suspend fun jsonObjkeys(key: K, jsonPath: JsonPath): List<List<V>> =
        ops.jsonObjkeys(key, jsonPath).asFlow().toList()

    override suspend fun jsonNumincrby(key: K, jsonPath: JsonPath, number: Number): List<JsonValue<V>> =
        ops.jsonNumincrby(key, jsonPath, number).asFlow().toList()

}

