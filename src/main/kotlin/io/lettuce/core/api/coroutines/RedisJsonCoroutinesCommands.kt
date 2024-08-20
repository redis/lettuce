/*
 * Copyright 2020-2024, Redis Ltd. and Contributors
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

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import io.lettuce.core.json.JsonValue
import io.lettuce.core.json.arguments.JsonGetArgs
import io.lettuce.core.json.arguments.JsonMsetArgs
import io.lettuce.core.json.JsonPath
import io.lettuce.core.json.arguments.JsonRangeArgs
import io.lettuce.core.json.arguments.JsonSetArgs

/**
 * Coroutine executed commands for JSON documents
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @see <a href="https://redis.io/docs/latest/develop/data-types/json/">Redis JSON</a>
 * @since 6.5
 * @generated by io.lettuce.apigenerator.CreateKotlinCoroutinesApi
 */
@ExperimentalLettuceCoroutinesApi
interface RedisJsonCoroutinesCommands<K : Any, V : Any> {

    /**
     * Append the JSON values into the array at a given [JsonPath] after the last element in said array.
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the array inside the document.
     * @param values one or more [JsonValue] to be appended.
     * @return Long the resulting size of the arrays after the new data was appended, or null if the path does not exist.
     * @since 6.5
     */
    suspend fun jsonArrappend(key: K, jsonPath: JsonPath, vararg values: JsonValue<K, V>): List<Long>

    /**
     * Search for the first occurrence of a [JsonValue] in an array at a given [JsonPath] and return its index.
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the array inside the document.
     * @param value the [JsonValue] to search for.
     * @param range the [JsonRangeArgs] to search within.
     * @return Long the index hosting the searched element, -1 if not found or null if the specified path is not an array.
     * @since 6.5
     */
    suspend fun jsonArrindex(key: K, jsonPath: JsonPath, value: JsonValue<K, V>, range: JsonRangeArgs): List<Long>

    /**
     * Insert the [JsonValue]s into the array at a given [JsonPath] before the provided index, shifting the existing
     * elements to the right
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the array inside the document.
     * @param index the index before which the new elements will be inserted.
     * @param values one or more [JsonValue]s to be inserted.
     * @return Long the resulting size of the arrays after the new data was inserted, or null if the path does not exist.
     * @since 6.5
     */
    suspend fun jsonArrinsert(key: K, jsonPath: JsonPath, index: Int, vararg values: JsonValue<K, V>): List<Long>

    /**
     * Report the length of the JSON array at a given [JsonPath]
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the array inside the document.
     * @return the size of the arrays, or null if the path does not exist.
     * @since 6.5
     */
    suspend fun jsonArrlen(key: K, jsonPath: JsonPath): List<Long>

    /**
     * Remove and return [JsonValue] at a given index in the array at a given [JsonPath]
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the array inside the document.
     * @param index the index of the element to be removed. Default is -1, meaning the last element. Out-of-range indexes round
     *        to their respective array ends. Popping an empty array returns null.
     * @return List<JsonValue> the removed element, or null if the specified path is not an array.
     * @since 6.5
     */
    suspend fun jsonArrpop(key: K, jsonPath: JsonPath, index: Int): List<JsonValue<K, V>>

    /**
     * Trim an array at a given [JsonPath] so that it contains only the specified inclusive range of elements. All
     * elements with indexes smaller the start range and all elements with indexes bigger the end range are trimmed.
     * <p>
     * Behavior as of RedisJSON v2.0:
     * <ul>
     * <li>If start is larger than the array's size or start > stop, returns 0 and an empty array.</li>
     * <li>If start is < 0, then start from the end of the array.</li>
     * <li>If stop is larger than the end of the array, it is treated like the last element.</li>
     * </ul>
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the array inside the document.
     * @param range the [JsonRangeArgs] to trim by.
     * @return Long the resulting size of the arrays after the trimming, or null if the path does not exist.
     * @since 6.5
     */
    suspend fun jsonArrtrim(key: K, jsonPath: JsonPath, range: JsonRangeArgs): List<Long>

    /**
     * Clear container values (arrays/objects) and set numeric values to 0
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value to clear.
     * @return Long the number of values removed plus all the matching JSON numerical values that are zeroed.
     * @since 6.5
     */
    suspend fun jsonClear(key: K, jsonPath: JsonPath): Long?

    /**
     * Deletes a value inside the JSON document at a given [JsonPath]
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value to clear.
     * @return Long the number of values removed (0 or more).
     * @since 6.5
     */
    suspend fun jsonDel(key: K, jsonPath: JsonPath): Long?

    /**
     * Return the value at path in JSON serialized form.
     * <p>
     * When using a single JSONPath, the root of the matching values is a JSON string with a top-level array of serialized JSON
     * value. In contrast, a legacy path returns a single value.
     * <p>
     * When using multiple JSONPath arguments, the root of the matching values is a JSON string with a top-level object, with
     * each object value being a top-level array of serialized JSON value. In contrast, if all paths are legacy paths, each
     * object value is a single serialized JSON value. If there are multiple paths that include both legacy path and JSONPath,
     * the returned value conforms to the JSONPath version (an array of values).
     *
     * @param key the key holding the JSON document.
     * @param options the [JsonGetArgs] to use.
     * @param jsonPaths the [JsonPath]s to use to identify the values to get.
     * @return JsonValue the value at path in JSON serialized form, or null if the path does not exist.
     * @since 6.5
     */
    suspend fun jsonGet(key: K, options: JsonGetArgs, vararg jsonPaths: JsonPath): List<JsonValue<K, V>>

    /**
     * Merge a given [JsonValue] with the value matching [JsonPath]. Consequently, JSON values at matching paths are
     * updated, deleted, or expanded with new children.
     * <p>
     * Merging is done according to the following rules per JSON value in the value argument while considering the corresponding
     * original value if it exists:
     * <ul>
     * <li>merging an existing object key with a null value deletes the key</li>
     * <li>merging an existing object key with non-null value updates the value</li>
     * <li>merging a non-existing object key adds the key and value</li>
     * <li>merging an existing array with any merged value, replaces the entire array with the value</li>
     * </ul>
     * <p>
     * This command complies with RFC7396 "Json Merge Patch"
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value to merge.
     * @param value the [JsonValue] to merge.
     * @return String "OK" if the set was successful, error if the operation failed.
     * @since 6.5
     * @see <A href="https://tools.ietf.org/html/rfc7396">RFC7396</a>
     */
    suspend fun jsonMerge(key: K, jsonPath: JsonPath, value: JsonValue<K, V>): String?

    /**
     * Return the values at path from multiple key arguments.
     *
     * @param jsonPath the [JsonPath] pointing to the value to fetch.
     * @param keys the keys holding the [JsonValue]s to fetch.
     * @return List<JsonValue> the values at path, or null if the path does not exist.
     * @since 6.5
     */
    suspend fun jsonMGet(jsonPath: JsonPath, vararg keys: K): List<JsonValue<K, V>>

    /**
     * Set or update one or more JSON values according to the specified [JsonMsetArgs]
     * <p>
     * JSON.MSET is atomic, hence, all given additions or updates are either applied or not. It is not possible for clients to
     * see that some of the keys were updated while others are unchanged.
     * <p>
     * A JSON value is a hierarchical structure. If you change a value in a specific path - nested values are affected.
     *
     * @param arguments the [JsonMsetArgs] specifying the values to change.
     * @return "OK" if the operation was successful, error otherwise
     * @since 6.5
     */
    suspend fun jsonMSet(vararg arguments: JsonMsetArgs): String?

    /**
     * Increment the number value stored at the specified [JsonPath] in the JSON document by the provided increment.
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value to increment.
     * @param number the increment value.
     * @return a [List] of the new values after the increment.
     * @since 6.5
     */
    suspend fun jsonNumincrby(key: K, jsonPath: JsonPath, number: Number): List<Number>

    /**
     * Return the keys in the JSON document that are referenced by the given [JsonPath]
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value(s) whose key(s) we want.
     * @return List<K> the keys in the JSON document that are referenced by the given [JsonPath].
     * @since 6.5
     */
    suspend fun jsonObjkeys(key: K, jsonPath: JsonPath): List<K>

    /**
     * Report the number of keys in the JSON object at path in key
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value(s) whose key(s) we want to count
     * @return Long the number of keys in the JSON object at the specified path, or null if the path does not exist.
     * @since 6.5
     */
    suspend fun jsonObjlen(key: K, jsonPath: JsonPath): List<Long>

    /**
     * Sets the JSON value at a given [JsonPath] in the JSON document.
     * <p>
     * For new Redis keys the path must be the root. For existing keys, when the entire path exists, the value that it contains
     * is replaced with the JSON value. For existing keys, when the path exists, except for the last element, a new child is
     * added with the JSON value.
     * <p>
     * Adds a key (with its respective value) to a JSON Any (in a RedisJSON data type key) only if it is the last child in
     * the path, or it is the parent of a new child being added in the path. Optional arguments NX and XX modify this behavior
     * for both new RedisJSON data type keys and the JSON Any keys in them.
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value(s) where we want to set the value.
     * @param value the [JsonValue] to set.
     * @param options the [JsonSetArgs] the options for setting the value.
     * @return String "OK" if the set was successful, null if the [JsonSetArgs] conditions are not met.
     * @since 6.5
     */
    suspend fun jsonSet(key: K, jsonPath: JsonPath, value: JsonValue<K, V>, options: JsonSetArgs): String?

    /**
     * Append the json-string values to the string at the provided [JsonPath] in the JSON document.
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value(s) where we want to append the value.
     * @param value the [JsonValue] to append.
     * @return Long the new length of the string, or null if the matching JSON value is not a string.
     * @since 6.5
     */
    suspend fun jsonStrappend(key: K, jsonPath: JsonPath, value: JsonValue<K, V>): List<Long>

    /**
     * Report the length of the JSON String at the provided [JsonPath] in the JSON document.
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value(s).
     * @return Long (in recursive descent) the length of the JSON String at the provided [JsonPath], or null if the value
     *         ath the desired path is not a string.
     * @since 6.5
     */
    suspend fun jsonStrlen(key: K, jsonPath: JsonPath): List<Long>

    /**
     * Toggle a Boolean value stored at the provided [JsonPath] in the JSON document.
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value(s).
     * @return List<Long> the new value after the toggle, 0 for false, 1 for true or null if the path does not exist.
     * @since 6.5
     */
    suspend fun jsonToggle(key: K, jsonPath: JsonPath): List<Long>

    /**
     * Report the type of JSON value at the provided [JsonPath] in the JSON document.
     *
     * @param key the key holding the JSON document.
     * @param jsonPath the [JsonPath] pointing to the value(s).
     * @return List<JsonType> the type of JSON value at the provided [JsonPath]
     * @since 6.5
     */
    suspend fun jsonType(key: K, jsonPath: JsonPath): List<V>

}

