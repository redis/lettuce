/*
 * Copyright 2020-2022 the original author or authors.
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

import io.lettuce.core.*
import io.lettuce.core.api.reactive.RedisGeoReactiveCommands
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull


/**
 * Coroutine executed commands (based on reactive commands) for the Geo-API.
 *
 * @author Mikhael Sokolov
 * @author Mark Paluch
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
internal class RedisGeoCoroutinesCommandsImpl<K : Any, V : Any>(internal val ops: RedisGeoReactiveCommands<K, V>) : RedisGeoCoroutinesCommands<K, V> {

    override suspend fun geoadd(
        key: K,
        longitude: Double,
        latitude: Double,
        member: V
    ): Long? = ops.geoadd(key, longitude, latitude, member).awaitFirstOrNull()

    override suspend fun geoadd(
        key: K,
        longitude: Double,
        latitude: Double,
        member: V,
        args: GeoAddArgs
    ): Long? = ops.geoadd(key, longitude, latitude, member, args).awaitFirstOrNull()

    override suspend fun geoadd(key: K, vararg lngLatMember: Any): Long? =
        ops.geoadd(key, *lngLatMember).awaitFirstOrNull()

    override suspend fun geoadd(key: K, vararg values: GeoValue<V>): Long? =
        ops.geoadd(key, *values).awaitFirstOrNull()

    override suspend fun geoadd(
        key: K,
        args: GeoAddArgs,
        vararg lngLatMember: Any
    ): Long? = ops.geoadd(key, args, *lngLatMember).awaitFirstOrNull()

    override suspend fun geoadd(
        key: K,
        args: GeoAddArgs,
        vararg values: GeoValue<V>
    ): Long? = ops.geoadd(key, args, *values).awaitFirstOrNull()

    override suspend fun geopos(key: K, vararg members: V): List<GeoCoordinates> =
        ops.geopos(key, *members).map { it.value }.asFlow().toList()

    override suspend fun geodist(key: K, from: V, to: V, unit: GeoArgs.Unit): Double? =
        ops.geodist(key, from, to, unit).awaitFirstOrNull()

    override fun geohash(key: K, vararg members: V): Flow<Value<String>> =
        ops.geohash(key, *members).asFlow()

    override fun georadius(
        key: K,
        longitude: Double,
        latitude: Double,
        distance: Double,
        unit: GeoArgs.Unit
    ): Flow<V> = ops.georadius(key, longitude, latitude, distance, unit).asFlow()

    override fun georadius(
        key: K,
        longitude: Double,
        latitude: Double,
        distance: Double,
        unit: GeoArgs.Unit,
        geoArgs: GeoArgs
    ): Flow<GeoWithin<V>> =
        ops.georadius(key, longitude, latitude, distance, unit, geoArgs).asFlow()

    override suspend fun georadius(key: K, longitude: Double, latitude: Double, distance: Double, unit: GeoArgs.Unit, geoRadiusStoreArgs: GeoRadiusStoreArgs<K>): Long? = ops.georadius(key, longitude, latitude, distance, unit, geoRadiusStoreArgs).awaitFirstOrNull()

    override fun georadiusbymember(key: K, member: V, distance: Double, unit: GeoArgs.Unit): Flow<V> = ops.georadiusbymember(key, member, distance, unit).asFlow()

    override fun georadiusbymember(key: K, member: V, distance: Double, unit: GeoArgs.Unit, geoArgs: GeoArgs): Flow<GeoWithin<V>> = ops.georadiusbymember(key, member, distance, unit, geoArgs).asFlow()

    override suspend fun georadiusbymember(key: K, member: V, distance: Double, unit: GeoArgs.Unit, geoRadiusStoreArgs: GeoRadiusStoreArgs<K>): Long? = ops.georadiusbymember(key, member, distance, unit, geoRadiusStoreArgs).awaitFirstOrNull()

    override fun geosearch(key: K, reference: GeoSearch.GeoRef<K>, predicate: GeoSearch.GeoPredicate): Flow<V> = ops.geosearch(key, reference, predicate).asFlow()

    override fun geosearch(key: K, reference: GeoSearch.GeoRef<K>, predicate: GeoSearch.GeoPredicate, geoArgs: GeoArgs): Flow<GeoWithin<V>>  = ops.geosearch(key, reference, predicate, geoArgs).asFlow()

    override suspend fun geosearchstore(destination: K, key: K, reference: GeoSearch.GeoRef<K>, predicate: GeoSearch.GeoPredicate, geoArgs: GeoArgs, storeDist: Boolean): Long? = ops.geosearchstore(destination, key, reference, predicate, geoArgs, storeDist).awaitFirstOrNull()
}

