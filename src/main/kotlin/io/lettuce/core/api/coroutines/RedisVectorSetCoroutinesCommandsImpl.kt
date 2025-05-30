/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.api.coroutines

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.VAddArgs
import io.lettuce.core.VSimArgs
import io.lettuce.core.api.reactive.RedisVectorSetReactiveCommands
import io.lettuce.core.json.JsonValue
import io.lettuce.core.vector.RawVector
import io.lettuce.core.vector.VectorMetadata
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.asFlow

/**
 * Coroutine executed commands (based on reactive commands) for Vector sets.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Tihomir Mateev
 * @since 6.7
 */
@ExperimentalLettuceCoroutinesApi

internal class RedisVectorSetCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisVectorSetReactiveCommands<K, V>) :
    RedisVectorSetCoroutinesCommands<K, V> {

    override suspend fun vadd(key: K, element: V, vararg vectors: Double): Boolean? =
        ops.vadd(key, element, *vectors.asSequence().toList().toTypedArray()).awaitFirstOrNull()

    override suspend fun vadd(key: K, dimensionality: Int, element: V, vararg vectors: Double): Boolean? =
        ops.vadd(key, dimensionality, element, *vectors.asSequence().toList().toTypedArray()).awaitFirstOrNull()

    override suspend fun vadd(key: K, element: V, args: VAddArgs, vararg vectors: Double): Boolean? =
        ops.vadd(key, element, args, *vectors.asSequence().toList().toTypedArray()).awaitFirstOrNull()

    override suspend fun vadd(key: K, dimensionality: Int, element: V, args: VAddArgs, vararg vectors: Double): Boolean? =
        ops.vadd(key, dimensionality, element, args, *vectors.asSequence().toList().toTypedArray()).awaitFirstOrNull()

    override suspend fun vcard(key: K): Long? = ops.vcard(key).awaitFirstOrNull()

    override suspend fun vClearAttributes(key: K, element: V): Boolean? = ops.vsetattr(key, element, "").awaitFirstOrNull()

    override suspend fun vdim(key: K): Long? = ops.vdim(key).awaitFirstOrNull()

    override suspend fun vemb(key: K, element: V): List<Double> = ops.vemb(key, element).asFlow().toList()

    override suspend fun vembRaw(key: K, element: V): RawVector? = ops.vembRaw(key, element).awaitFirstOrNull()

    override suspend fun vgetattr(key: K, element: V): String? = ops.vgetattr(key, element).awaitFirstOrNull()

    override suspend fun vgetattrAsJsonValue(key: K, element: V): List<JsonValue> = ops.vgetattrAsJsonValue(key, element).asFlow().toList()

    override suspend fun vinfo(key: K): VectorMetadata? = ops.vinfo(key).awaitFirstOrNull()

    override suspend fun vlinks(key: K, element: V): List<V> = ops.vlinks(key, element).asFlow().toList()

    override suspend fun vlinksWithScores(key: K, element: V): Map<V, Double>? = ops.vlinksWithScores(key, element).awaitFirstOrNull()

    override suspend fun vrandmember(key: K): V? = ops.vrandmember(key).awaitFirstOrNull()

    override suspend fun vrandmember(key: K, count: Int): List<V> = ops.vrandmember(key, count).asFlow().toList()

    override suspend fun vrem(key: K, element: V): Boolean? = ops.vrem(key, element).awaitFirstOrNull()

    override suspend fun vsetattr(key: K, element: V, json: String): Boolean? = ops.vsetattr(key, element, json).awaitFirstOrNull()

    override suspend fun vsetattr(key: K, element: V, json: JsonValue): Boolean? = ops.vsetattr(key, element, json).awaitFirstOrNull()

    override suspend fun vsim(key: K, vararg vectors: Double): List<V> = ops.vsim(key, *vectors.asSequence().toList().toTypedArray()).asFlow().toList()

    override suspend fun vsim(key: K, element: V): List<V> = ops.vsim(key, element).asFlow().toList()

    override suspend fun vsim(key: K, args: VSimArgs, vararg vectors: Double): List<V> = ops.vsim(key, args, *vectors.asSequence().toList().toTypedArray()).asFlow().toList()

    override suspend fun vsim(key: K, args: VSimArgs, element: V): List<V> = ops.vsim(key, args, element).asFlow().toList()

    override suspend fun vsimWithScore(key: K, vararg vectors: Double): Map<V, Double>? = ops.vsimWithScore(key, *vectors.asSequence().toList().toTypedArray()).awaitFirstOrNull()

    override suspend fun vsimWithScore(key: K, element: V): Map<V, Double>? = ops.vsimWithScore(key, element).awaitFirstOrNull()

    override suspend fun vsimWithScore(key: K, args: VSimArgs, vararg vectors: Double): Map<V, Double>? = ops.vsimWithScore(key, args, *vectors.asSequence().toList().toTypedArray()).awaitFirstOrNull()

    override suspend fun vsimWithScore(key: K, args: VSimArgs, element: V): Map<V, Double>? = ops.vsimWithScore(key, args, element).awaitFirstOrNull()
}
