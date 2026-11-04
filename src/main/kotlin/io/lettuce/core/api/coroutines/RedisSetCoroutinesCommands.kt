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

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.ExpireArgs
import io.lettuce.core.SAddExArgs
import io.lettuce.core.SDiffCardArgs
import io.lettuce.core.SUnionCardArgs
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.ValueScanCursor
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.util.Date

/**
 * Coroutine executed commands for Sets.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mikhael Sokolov
 * @since 6.0
 */
@ExperimentalLettuceCoroutinesApi
interface RedisSetCoroutinesCommands<K : Any, V : Any> {

    /**
     * Add one or more members to a set.
     *
     * @param key the key.
     * @param members the member type: value.
     * @return Long integer-reply the number of elements that were added to the set, not including all the elements already
     *         present into the set.
     */
    suspend fun sadd(key: K, vararg members: V): Long?

    /**
     * Get the number of members in a set.
     *
     * @param key the key.
     * @return Long integer-reply the cardinality (number of elements) of the set, or `false` if `key` does not
     *         exist.
     */
    suspend fun scard(key: K): Long?

    /**
     * Subtract multiple sets.
     *
     * @param keys the key.
     * @return Set<V> array-reply list with members of the resulting set.
     */
    fun sdiff(vararg keys: K): Flow<V>

    /**
     * Compute the number of elements in the difference between the set stored at `key1` and the set stored at `key2`, without returning the difference itself. Non-existing keys are treated as empty sets; when `key1` does not exist the result is `0`.
     *
     * @param key1 the key of the set to subtract from, must not be `null`.
     * @param key2 the key of the set to subtract, must not be `null`.
     * @return the cardinality of the difference.
     * @since 7.7
     */
    suspend fun sdiffcard(key1: K, key2: K): Long?

    /**
     * Compute the number of elements in the difference between the set stored at the first key and the sets stored at all successive keys, without returning the difference itself. Non-existing keys are treated as empty sets; when the first key does not exist the result is `0`.
     *
     * @param keys the keys, must not be `null` or empty. The first key is the set to subtract from, all successive keys are subtracted.
     * @return the cardinality of the difference.
     * @since 7.7
     */
    suspend fun sdiffcard(keys: List<K>): Long?

    /**
     * Compute the number of elements in the difference between the set stored at `key1` and the set stored at `key2`, without returning the difference itself, applying [SDiffCardArgs]. Non-existing keys are treated as empty sets; when `key1` does not exist the result is `0`.
     *
     * @param key1 the key of the set to subtract from, must not be `null`.
     * @param key2 the key of the set to subtract, must not be `null`.
     * @param sdiffCardArgs the command arguments (`LIMIT`), must not be `null`.
     * @return the cardinality of the difference; when a limit is set, at most that limit.
     * @since 7.7
     */
    suspend fun sdiffcard(key1: K, key2: K, sdiffCardArgs: SDiffCardArgs): Long?

    /**
     * Compute the number of elements in the difference between the set stored at the first key and the sets stored at all successive keys, without returning the difference itself, applying [SDiffCardArgs]. Non-existing keys are treated as empty sets; when the first key does not exist the result is `0`.
     *
     * @param keys the keys, must not be `null` or empty. The first key is the set to subtract from, all successive keys are subtracted.
     * @param sdiffCardArgs the command arguments (`LIMIT`), must not be `null`.
     * @return the cardinality of the difference; when a limit is set, at most that limit.
     * @since 7.7
     */
    suspend fun sdiffcard(keys: List<K>, sdiffCardArgs: SDiffCardArgs): Long?

    /**
     * Subtract multiple sets and store the resulting set in a key.
     *
     * @param destination the destination type: key.
     * @param keys the key.
     * @return Long integer-reply the number of elements in the resulting set.
     */
    suspend fun sdiffstore(destination: K, vararg keys: K): Long?

    /**
     * Intersect multiple sets.
     *
     * @param keys the key.
     * @return Set<V> array-reply list with members of the resulting set.
     */
    fun sinter(vararg keys: K): Flow<V>

    /**
     * This command works exactly like {@link #sinter(Any[])} but instead of returning the result set, it returns
     * just the cardinality of the result.
     *
     * @param keys the keys.
     * @return The cardinality of the set which would result from the intersection of all the given sets.
     * @since 6.2
     */
    suspend fun sintercard(vararg keys: K): Long?

    /**
     * This command works exactly like {@link #sinter(Any[])} but instead of returning the result set, it returns
     * just the cardinality of the result.
     *
     * @param limit If the intersection cardinality reaches limit partway through the computation, the algorithm will exit and
     *        yield limit as the cardinality.
     * @param keys the keys.
     * @return The cardinality of the set which would result from the intersection of all the given sets.
     * @since 6.2
     */
    suspend fun sintercard(limit: Long, vararg keys: K): Long?

    /**
     * Intersect multiple sets and store the resulting set in a key.
     *
     * @param destination the destination type: key.
     * @param keys the key.
     * @return Long integer-reply the number of elements in the resulting set.
     */
    suspend fun sinterstore(destination: K, vararg keys: K): Long?

    /**
     * Determine if a given value is a member of a set.
     *
     * @param key the key.
     * @param member the member type: value.
     * @return Boolean integer-reply specifically:
     *
     *         `true` if the element is a member of the set. `false` if the element is not a member of the set, or
     *         if `key` does not exist.
     */
    suspend fun sismember(key: K, member: V): Boolean?

    /**
     * Get all the members in a set.
     *
     * @param key the key.
     * @return Set<V> array-reply all elements of the set.
     */
    fun smembers(key: K): Flow<V>

    /**
     * Returns whether each member is a member of the set stored at key.
     *
     * @param key the key.
     * @param members the member type: value.
     * @return List<Boolean> array-reply list representing the membership of the given elements, in the same order as they
     *         are requested.
     * @since 6.1
     */
    fun smismember(key: K, vararg members: V): Flow<Boolean>

    /**
     * Move a member from one set to another.
     *
     * @param source the source key.
     * @param destination the destination type: key.
     * @param member the member type: value.
     * @return Boolean integer-reply specifically:
     *
     *         `true` if the element is moved. `false` if the element is not a member of `source` and no
     *         operation was performed.
     */
    suspend fun smove(source: K, destination: K, member: V): Boolean?

    /**
     * Remove and return a random member from a set.
     *
     * @param key the key.
     * @return V bulk-string-reply the removed element, or `null` when `key` does not exist.
     */
    suspend fun spop(key: K): V?

    /**
     * Remove and return one or multiple random members from a set.
     *
     * @param key the key.
     * @param count number of members to pop.
     * @return V bulk-string-reply the removed element, or `null` when `key` does not exist.
     */
    suspend fun spop(key: K, count: Long): Set<V>

    /**
     * Get one random member from a set.
     *
     * @param key the key.
     * @return V bulk-string-reply without the additional `count` argument the command returns a Bulk Reply with the
     *         randomly selected element, or `null` when `key` does not exist.
     */
    suspend fun srandmember(key: K): V?

    /**
     * Get one or multiple random members from a set.
     *
     * @param key the key.
     * @param count the count type: long.
     * @return Set<V> bulk-string-reply without the additional `count` argument the command returns a Bulk Reply
     *         with the randomly selected element, or `null` when `key` does not exist.
     */
    fun srandmember(key: K, count: Long): Flow<V>

    /**
     * Remove one or more members from a set.
     *
     * @param key the key.
     * @param members the member type: value.
     * @return Long integer-reply the number of members that were removed from the set, not including non existing members.
     */
    suspend fun srem(key: K, vararg members: V): Long?

    /**
     * Add multiple sets.
     *
     * @param keys the key.
     * @return Set<V> array-reply list with members of the resulting set.
     */
    fun sunion(vararg keys: K): Flow<V>

    /**
     * Compute the number of distinct elements in the union of the sets stored at `key1` and `key2` without returning the union itself. Non-existing keys are treated as empty sets.
     *
     * @param key1 the key of the first set, must not be `null`.
     * @param key2 the key of the second set, must not be `null`.
     * @return the cardinality of the union.
     * @since 7.7
     */
    suspend fun sunioncard(key1: K, key2: K): Long?

    /**
     * Compute the number of distinct elements in the union of the sets stored at the given keys without returning the union itself. Non-existing keys are treated as empty sets.
     *
     * @param keys the keys of the sets to union, must not be `null` or empty.
     * @return the cardinality of the union.
     * @since 7.7
     */
    suspend fun sunioncard(keys: List<K>): Long?

    /**
     * Compute the number of distinct elements in the union of the sets stored at `key1` and `key2` without returning the union itself, applying [SUnionCardArgs]. Non-existing keys are treated as empty sets.
     *
     * @param key1 the key of the first set, must not be `null`.
     * @param key2 the key of the second set, must not be `null`.
     * @param sunionCardArgs the command arguments (`APPROX`, `LIMIT`), must not be `null`.
     * @return the cardinality of the union; when a limit is set, at most that limit.
     * @since 7.7
     */
    suspend fun sunioncard(key1: K, key2: K, sunionCardArgs: SUnionCardArgs): Long?

    /**
     * Compute the number of distinct elements in the union of the sets stored at the given keys without returning the union itself, applying [SUnionCardArgs]. Non-existing keys are treated as empty sets.
     *
     * @param keys the keys of the sets to union, must not be `null` or empty.
     * @param sunionCardArgs the command arguments (`APPROX`, `LIMIT`), must not be `null`.
     * @return the cardinality of the union; when a limit is set, at most that limit.
     * @since 7.7
     */
    suspend fun sunioncard(keys: List<K>, sunionCardArgs: SUnionCardArgs): Long?

    /**
     * Add multiple sets and store the resulting set in a key.
     *
     * @param destination the destination type: key.
     * @param keys the key.
     * @return Long integer-reply the number of elements in the resulting set.
     */
    suspend fun sunionstore(destination: K, vararg keys: K): Long?

    /**
     * Incrementally iterate Set elements.
     *
     * @param key the key.
     * @return ValueScanCursor<V> scan cursor.
     */
    suspend fun sscan(key: K): ValueScanCursor<V>?

    /**
     * Incrementally iterate Set elements.
     *
     * @param key the key.
     * @param scanArgs scan arguments.
     * @return ValueScanCursor<V> scan cursor.
     */
    suspend fun sscan(key: K, scanArgs: ScanArgs): ValueScanCursor<V>?

    /**
     * Incrementally iterate Set elements.
     *
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be `null`.
     * @param scanArgs scan arguments.
     * @return ValueScanCursor<V> scan cursor.
     */
    suspend fun sscan(key: K, scanCursor: ScanCursor, scanArgs: ScanArgs): ValueScanCursor<V>?

    /**
     * Incrementally iterate Set elements.
     *
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be `null`.
     * @return ValueScanCursor<V> scan cursor.
     */
    suspend fun sscan(key: K, scanCursor: ScanCursor): ValueScanCursor<V>?

    /**
     * Set the time to live, in seconds, of one or more members of the set stored at `key`. A time to live of `0` is already
     * in the past and deletes the members.
     *
     * @param key the key of the set, must not be `null`.
     * @param seconds the time to live in seconds, must not be negative.
     * @param members the members to set the time to live for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun sexpire(key: K, seconds: Long, vararg members: V): List<Long>

    /**
     * Set the time to live, in seconds, of a single member of the set stored at `key`. A time to live of `0` is already in
     * the past and deletes the member.
     *
     * @param key the key of the set, must not be `null`.
     * @param seconds the time to live in seconds, must not be negative.
     * @param member the member to set the time to live for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpire(key: K, seconds: Long, member: V): List<Long>

    /**
     * Set the time to live, in seconds, of one or more members of the set stored at `key`, applying the [ExpireArgs]
     * condition (`NX`, `XX`, `GT` or `LT`) to each member. A member without an expiration is treated as having an infinite
     * one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param seconds the time to live in seconds, must not be negative.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param members the members to set the time to live for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun sexpire(key: K, seconds: Long, expireArgs: ExpireArgs, vararg members: V): List<Long>

    /**
     * Set the time to live, in seconds, of a single member of the set stored at `key`, applying the [ExpireArgs] condition
     * (`NX`, `XX`, `GT` or `LT`) to each member. A member without an expiration is treated as having an infinite one: `LT`
     * succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param seconds the time to live in seconds, must not be negative.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param member the member to set the time to live for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpire(key: K, seconds: Long, expireArgs: ExpireArgs, member: V): List<Long>

    /**
     * Set the time to live of one or more members of the set stored at `key`. The [Duration] is truncated to whole seconds;
     * a duration under one second becomes `0`, which is already in the past and deletes the members.
     *
     * @param key the key of the set, must not be `null`.
     * @param seconds the time to live, must not be `null`; truncated to whole seconds.
     * @param members the members to set the time to live for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun sexpire(key: K, seconds: Duration, vararg members: V): List<Long>

    /**
     * Set the time to live of a single member of the set stored at `key`. The [Duration] is truncated to whole seconds; a
     * duration under one second becomes `0`, which is already in the past and deletes the member.
     *
     * @param key the key of the set, must not be `null`.
     * @param seconds the time to live, must not be `null`; truncated to whole seconds.
     * @param member the member to set the time to live for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpire(key: K, seconds: Duration, member: V): List<Long>

    /**
     * Set the time to live of one or more members of the set stored at `key`, applying the [ExpireArgs] condition (`NX`,
     * `XX`, `GT` or `LT`) to each member. The [Duration] is truncated to whole seconds; a duration under one second becomes
     * `0`, which is already in the past and deletes the members. A member without an expiration is treated as having an
     * infinite one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param seconds the time to live, must not be `null`; truncated to whole seconds.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param members the members to set the time to live for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun sexpire(key: K, seconds: Duration, expireArgs: ExpireArgs, vararg members: V): List<Long>

    /**
     * Set the time to live of a single member of the set stored at `key`, applying the [ExpireArgs] condition (`NX`, `XX`,
     * `GT` or `LT`) to each member. The [Duration] is truncated to whole seconds; a duration under one second becomes `0`,
     * which is already in the past and deletes the member. A member without an expiration is treated as having an infinite
     * one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param seconds the time to live, must not be `null`; truncated to whole seconds.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param member the member to set the time to live for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpire(key: K, seconds: Duration, expireArgs: ExpireArgs, member: V): List<Long>

    /**
     * Set the time to live, in milliseconds, of one or more members of the set stored at `key`. A time to live of `0` is
     * already in the past and deletes the members.
     *
     * @param key the key of the set, must not be `null`.
     * @param milliseconds the time to live in milliseconds, must not be negative.
     * @param members the members to set the time to live for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun spexpire(key: K, milliseconds: Long, vararg members: V): List<Long>

    /**
     * Set the time to live, in milliseconds, of a single member of the set stored at `key`. A time to live of `0` is already
     * in the past and deletes the member.
     *
     * @param key the key of the set, must not be `null`.
     * @param milliseconds the time to live in milliseconds, must not be negative.
     * @param member the member to set the time to live for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpire(key: K, milliseconds: Long, member: V): List<Long>

    /**
     * Set the time to live, in milliseconds, of one or more members of the set stored at `key`, applying the [ExpireArgs]
     * condition (`NX`, `XX`, `GT` or `LT`) to each member. A member without an expiration is treated as having an infinite
     * one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param milliseconds the time to live in milliseconds, must not be negative.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param members the members to set the time to live for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun spexpire(key: K, milliseconds: Long, expireArgs: ExpireArgs, vararg members: V): List<Long>

    /**
     * Set the time to live, in milliseconds, of a single member of the set stored at `key`, applying the [ExpireArgs]
     * condition (`NX`, `XX`, `GT` or `LT`) to each member. A member without an expiration is treated as having an infinite
     * one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param milliseconds the time to live in milliseconds, must not be negative.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param member the member to set the time to live for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpire(key: K, milliseconds: Long, expireArgs: ExpireArgs, member: V): List<Long>

    /**
     * Set the time to live of one or more members of the set stored at `key`. The [Duration] is truncated to whole
     * milliseconds; a duration under one millisecond becomes `0`, which is already in the past and deletes the members.
     *
     * @param key the key of the set, must not be `null`.
     * @param milliseconds the time to live, must not be `null`; truncated to whole milliseconds.
     * @param members the members to set the time to live for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun spexpire(key: K, milliseconds: Duration, vararg members: V): List<Long>

    /**
     * Set the time to live of a single member of the set stored at `key`. The [Duration] is truncated to whole milliseconds;
     * a duration under one millisecond becomes `0`, which is already in the past and deletes the member.
     *
     * @param key the key of the set, must not be `null`.
     * @param milliseconds the time to live, must not be `null`; truncated to whole milliseconds.
     * @param member the member to set the time to live for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpire(key: K, milliseconds: Duration, member: V): List<Long>

    /**
     * Set the time to live of one or more members of the set stored at `key`, applying the [ExpireArgs] condition (`NX`,
     * `XX`, `GT` or `LT`) to each member. The [Duration] is truncated to whole milliseconds; a duration under one
     * millisecond becomes `0`, which is already in the past and deletes the members. A member without an expiration is
     * treated as having an infinite one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param milliseconds the time to live, must not be `null`; truncated to whole milliseconds.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param members the members to set the time to live for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun spexpire(key: K, milliseconds: Duration, expireArgs: ExpireArgs, vararg members: V): List<Long>

    /**
     * Set the time to live of a single member of the set stored at `key`, applying the [ExpireArgs] condition (`NX`, `XX`,
     * `GT` or `LT`) to each member. The [Duration] is truncated to whole milliseconds; a duration under one millisecond
     * becomes `0`, which is already in the past and deletes the member. A member without an expiration is treated as having
     * an infinite one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param milliseconds the time to live, must not be `null`; truncated to whole milliseconds.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param member the member to set the time to live for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpire(key: K, milliseconds: Duration, expireArgs: ExpireArgs, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute Unix time in seconds. A time in
     * the past deletes the members.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the Unix time in seconds at which the members expire, must not be negative.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Long, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute Unix time in seconds. A time in the
     * past deletes the member.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the Unix time in seconds at which the members expire, must not be negative.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Long, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute Unix time in seconds, applying the
     * [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes the members. A member
     * without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the Unix time in seconds at which the members expire, must not be negative.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Long, expireArgs: ExpireArgs, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute Unix time in seconds, applying the
     * [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes the member. A member
     * without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the Unix time in seconds at which the members expire, must not be negative.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Long, expireArgs: ExpireArgs, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute point in time, with second
     * precision. A time in the past deletes the members.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`; truncated to whole seconds.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Date, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute point in time, with second precision.
     * A time in the past deletes the member.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`; truncated to whole seconds.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Date, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute point in time, with second
     * precision, applying the [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes
     * the members. A member without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX`
     * fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`; truncated to whole seconds.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Date, expireArgs: ExpireArgs, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute point in time, with second precision,
     * applying the [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes the member.
     * A member without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`; truncated to whole seconds.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Date, expireArgs: ExpireArgs, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute point in time, with second
     * precision. A time in the past deletes the members.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`; truncated to whole seconds.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Instant, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute point in time, with second precision.
     * A time in the past deletes the member.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`; truncated to whole seconds.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Instant, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute point in time, with second
     * precision, applying the [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes
     * the members. A member without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX`
     * fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`; truncated to whole seconds.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Instant, expireArgs: ExpireArgs, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute point in time, with second precision,
     * applying the [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes the member.
     * A member without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`; truncated to whole seconds.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpireat(key: K, timestamp: Instant, expireArgs: ExpireArgs, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute Unix time in milliseconds. A time
     * in the past deletes the members.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the Unix time in milliseconds at which the members expire, must not be negative.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Long, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute Unix time in milliseconds. A time in
     * the past deletes the member.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the Unix time in milliseconds at which the members expire, must not be negative.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Long, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute Unix time in milliseconds,
     * applying the [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes the members.
     * A member without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the Unix time in milliseconds at which the members expire, must not be negative.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Long, expireArgs: ExpireArgs, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute Unix time in milliseconds, applying
     * the [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes the member. A member
     * without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX` fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the Unix time in milliseconds at which the members expire, must not be negative.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Long, expireArgs: ExpireArgs, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute point in time, with millisecond
     * precision. A time in the past deletes the members.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Date, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute point in time, with millisecond
     * precision. A time in the past deletes the member.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Date, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute point in time, with millisecond
     * precision, applying the [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes
     * the members. A member without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX`
     * fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Date, expireArgs: ExpireArgs, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute point in time, with millisecond
     * precision, applying the [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes
     * the member. A member without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX`
     * fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Date, expireArgs: ExpireArgs, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute point in time, with millisecond
     * precision. A time in the past deletes the members.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Instant, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute point in time, with millisecond
     * precision. A time in the past deletes the member.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Instant, member: V): List<Long>

    /**
     * Set the expiration of one or more members of the set stored at `key` to an absolute point in time, with millisecond
     * precision, applying the [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes
     * the members. A member without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX`
     * fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param members the members to set the expiration for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `2` if the member was deleted because
     *         the expiration is in the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT`
     *         condition was not met; `-2` if the member does not exist (a logically expired member does not exist) or `key`
     *         does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Instant, expireArgs: ExpireArgs, vararg members: V): List<Long>

    /**
     * Set the expiration of a single member of the set stored at `key` to an absolute point in time, with millisecond
     * precision, applying the [ExpireArgs] condition (`NX`, `XX`, `GT` or `LT`) to each member. A time in the past deletes
     * the member. A member without an expiration is treated as having an infinite one: `LT` succeeds on it, `GT` and `XX`
     * fail.
     *
     * @param key the key of the set, must not be `null`.
     * @param timestamp the point in time at which the members expire, must not be `null`.
     * @param expireArgs the condition (`NX`, `XX`, `GT` or `LT`), must not be `null`.
     * @param member the member to set the expiration for.
     * @return a list with a single [Long] for `member`: `2` if the member was deleted because the expiration is in
     *         the past; `1` if the expiration was set or updated; `0` if a `NX`, `XX`, `GT` or `LT` condition was not met;
     *         `-2` if the member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpireat(key: K, timestamp: Instant, expireArgs: ExpireArgs, member: V): List<Long>

    /**
     * Get the remaining time to live, in seconds, of one or more members of the set stored at `key`. The value is rounded up
     * to the next second, so a live member never returns `0`.
     *
     * @param key the key of the set, must not be `null`.
     * @param members the members to get the time to live for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: the remaining time to live in seconds,
     *         rounded up to the next second; `-1` if the member exists but has no expiration; `-2` if the member does not
     *         exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sttl(key: K, vararg members: V): List<Long>

    /**
     * Get the remaining time to live, in seconds, of a single member of the set stored at `key`. The value is rounded up to
     * the next second, so a live member never returns `0`.
     *
     * @param key the key of the set, must not be `null`.
     * @param member the member to get the time to live for.
     * @return a list with a single [Long] for `member`: the remaining time to live in seconds, rounded up to the
     *         next second; `-1` if the member exists but has no expiration; `-2` if the member does not exist (a logically
     *         expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sttl(key: K, member: V): List<Long>

    /**
     * Get the remaining time to live, in milliseconds, of one or more members of the set stored at `key`.
     *
     * @param key the key of the set, must not be `null`.
     * @param members the members to get the time to live for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: the remaining time to live in
     *         milliseconds, `0` if the member is expiring now; `-1` if the member exists but has no expiration; `-2` if the
     *         member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spttl(key: K, vararg members: V): List<Long>

    /**
     * Get the remaining time to live, in milliseconds, of a single member of the set stored at `key`.
     *
     * @param key the key of the set, must not be `null`.
     * @param member the member to get the time to live for.
     * @return a list with a single [Long] for `member`: the remaining time to live in milliseconds, `0` if the
     *         member is expiring now; `-1` if the member exists but has no expiration; `-2` if the member does not exist (a
     *         logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spttl(key: K, member: V): List<Long>

    /**
     * Get the absolute Unix time, in seconds, at which one or more members of the set stored at `key` expire. The value is
     * rounded up to the next second.
     *
     * @param key the key of the set, must not be `null`.
     * @param members the members to get the expiration time for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: the Unix time in seconds at which the
     *         member expires, rounded up to the next second; `-1` if the member exists but has no expiration; `-2` if the
     *         member does not exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpiretime(key: K, vararg members: V): List<Long>

    /**
     * Get the absolute Unix time, in seconds, at which a single member of the set stored at `key` expire. The value is
     * rounded up to the next second.
     *
     * @param key the key of the set, must not be `null`.
     * @param member the member to get the expiration time for.
     * @return a list with a single [Long] for `member`: the Unix time in seconds at which the member expires,
     *         rounded up to the next second; `-1` if the member exists but has no expiration; `-2` if the member does not
     *         exist (a logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun sexpiretime(key: K, member: V): List<Long>

    /**
     * Get the absolute Unix time, in milliseconds, at which one or more members of the set stored at `key` expire.
     *
     * @param key the key of the set, must not be `null`.
     * @param members the members to get the expiration time for, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: the Unix time in milliseconds at which
     *         the member expires; `-1` if the member exists but has no expiration; `-2` if the member does not exist (a
     *         logically expired member does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpiretime(key: K, vararg members: V): List<Long>

    /**
     * Get the absolute Unix time, in milliseconds, at which a single member of the set stored at `key` expire.
     *
     * @param key the key of the set, must not be `null`.
     * @param member the member to get the expiration time for.
     * @return a list with a single [Long] for `member`: the Unix time in milliseconds at which the member expires;
     *         `-1` if the member exists but has no expiration; `-2` if the member does not exist (a logically expired member
     *         does not exist) or `key` does not exist.
     * @since 8.0
     */
    suspend fun spexpiretime(key: K, member: V): List<Long>

    /**
     * Remove the expiration of one or more members of the set stored at `key`.
     *
     * @param key the key of the set, must not be `null`.
     * @param members the members to remove the expiration from, must not be `null` or empty.
     * @return a list with one [Long] per member, in the order of `members`: `1` if the expiration was removed; `-1`
     *         if the member exists but has no expiration; `-2` if the member does not exist or `key` does not exist.
     * @since 8.0
     */
    suspend fun spersist(key: K, vararg members: V): List<Long>

    /**
     * Remove the expiration of a single member of the set stored at `key`.
     *
     * @param key the key of the set, must not be `null`.
     * @param member the member to remove the expiration from.
     * @return a list with a single [Long] for `member`: `1` if the expiration was removed; `-1` if the member exists
     *         but has no expiration; `-2` if the member does not exist or `key` does not exist.
     * @since 8.0
     */
    suspend fun spersist(key: K, member: V): List<Long>

    /**
     * Add one or more members to the set stored at `key`, creating it when it does not exist. Unlike [sadd], a member that
     * already exists is not skipped: its expiration is discarded. Without [SAddExArgs] the command behaves like `SADD` for
     * members without an expiration.
     *
     * @param key the key of the set, must not be `null`.
     * @param members the members to add, must not be `null` or empty.
     * @return `1` if at least one member was added, updated or deleted; `0` if nothing changed.
     * @since 8.0
     */
    suspend fun saddex(key: K, vararg members: V): Long?

    /**
     * Add a single member to the set stored at `key`, creating it when it does not exist. Unlike [sadd], a member that
     * already exists is not skipped: its expiration is discarded. Without [SAddExArgs] the command behaves like `SADD` for
     * members without an expiration.
     *
     * @param key the key of the set, must not be `null`.
     * @param member the member to add.
     * @return `1` if at least one member was added, updated or deleted; `0` if nothing changed.
     * @since 8.0
     */
    suspend fun saddex(key: K, member: V): Long?

    /**
     * Add one or more members to the set stored at `key` and set their expiration, applying [SAddExArgs]. `MNX` adds only if
     * none of the members exists, `MXX` only if all of them exist; both apply to the whole command. `EX`, `PX`, `EXAT` or
     * `PXAT` set the expiration of every member in the command, `KEEPTTL` keeps the expiration a member already has; without
     * any of them an existing member loses its expiration. An expiration in the past deletes the members that exist and adds
     * none.
     *
     * @param key the key of the set, must not be `null`.
     * @param sAddExArgs the command arguments, must not be `null`.
     * @param members the members to add, must not be `null` or empty.
     * @return `1` if at least one member was added, updated or deleted; `0` if the `MNX` or `MXX` condition was not
     *         met, or nothing changed.
     * @since 8.0
     */
    suspend fun saddex(key: K, sAddExArgs: SAddExArgs, vararg members: V): Long?

    /**
     * Add a single member to the set stored at `key` and set its expiration, applying [SAddExArgs]. `MNX` adds only if none
     * of the members exists, `MXX` only if all of them exist; both apply to the whole command. `EX`, `PX`, `EXAT` or `PXAT`
     * set the expiration of every member in the command, `KEEPTTL` keeps the expiration a member already has; without any of
     * them an existing member loses its expiration. An expiration in the past deletes the member if it exists and adds
     * nothing.
     *
     * @param key the key of the set, must not be `null`.
     * @param sAddExArgs the command arguments, must not be `null`.
     * @param member the member to add.
     * @return `1` if at least one member was added, updated or deleted; `0` if the `MNX` or `MXX` condition was not
     *         met, or nothing changed.
     * @since 8.0
     */
    suspend fun saddex(key: K, sAddExArgs: SAddExArgs, member: V): Long?

}
