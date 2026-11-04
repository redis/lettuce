/*
 * Copyright 2017-Present, Redis Ltd. and Contributors
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
package io.lettuce.core.api.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import io.lettuce.core.ExpireArgs;
import io.lettuce.core.SAddExArgs;
import io.lettuce.core.SDiffCardArgs;
import io.lettuce.core.SUnionCardArgs;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.output.ValueStreamingChannel;

/**
 * Reactive executed commands for Sets.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface RedisSetReactiveCommands<K, V> {

    /**
     * Add one or more members to a set.
     *
     * @param key the key.
     * @param members the member type: value.
     * @return Long integer-reply the number of elements that were added to the set, not including all the elements already
     *         present into the set.
     */
    Mono<Long> sadd(K key, V... members);

    /**
     * Get the number of members in a set.
     *
     * @param key the key.
     * @return Long integer-reply the cardinality (number of elements) of the set, or {@code false} if {@code key} does not
     *         exist.
     */
    Mono<Long> scard(K key);

    /**
     * Subtract multiple sets.
     *
     * @param keys the key.
     * @return V array-reply list with members of the resulting set.
     */
    Flux<V> sdiff(K... keys);

    /**
     * Subtract multiple sets.
     *
     * @param channel the channel.
     * @param keys the keys.
     * @return Long count of members of the resulting set.
     * @deprecated since 6.0 in favor of consuming large results through the {@link org.reactivestreams.Publisher} returned by
     *             {@link #sdiff}.
     */
    @Deprecated
    Mono<Long> sdiff(ValueStreamingChannel<V> channel, K... keys);

    /**
     * Compute the number of elements in the difference between the set stored at {@code key1} and the set stored at
     * {@code key2}, without returning the difference itself. Non-existing keys are treated as empty sets; when {@code key1}
     * does not exist the result is {@code 0}.
     *
     * @param key1 the key of the set to subtract from, must not be {@code null}.
     * @param key2 the key of the set to subtract, must not be {@code null}.
     * @return the cardinality of the difference.
     * @throws IllegalArgumentException if {@code key1} or {@code key2} is {@code null}.
     * @since 7.7
     */
    Mono<Long> sdiffcard(K key1, K key2);

    /**
     * Compute the number of elements in the difference between the set stored at the first key and the sets stored at all
     * successive keys, without returning the difference itself. Non-existing keys are treated as empty sets; when the first key
     * does not exist the result is {@code 0}.
     *
     * @param keys the keys, must not be {@code null} or empty. The first key is the set to subtract from, all successive keys
     *        are subtracted.
     * @return the cardinality of the difference.
     * @throws IllegalArgumentException if {@code keys} is {@code null} or empty.
     * @since 7.7
     */
    Mono<Long> sdiffcard(List<K> keys);

    /**
     * Compute the number of elements in the difference between the set stored at {@code key1} and the set stored at
     * {@code key2}, without returning the difference itself, applying {@link SDiffCardArgs}. Non-existing keys are treated as
     * empty sets; when {@code key1} does not exist the result is {@code 0}.
     *
     * @param key1 the key of the set to subtract from, must not be {@code null}.
     * @param key2 the key of the set to subtract, must not be {@code null}.
     * @param sdiffCardArgs the command arguments ({@code LIMIT}), must not be {@code null}.
     * @return the cardinality of the difference; when a limit is set, at most that limit.
     * @throws IllegalArgumentException if {@code key1}, {@code key2} or {@code sdiffCardArgs} is {@code null}.
     * @since 7.7
     */
    Mono<Long> sdiffcard(K key1, K key2, SDiffCardArgs sdiffCardArgs);

    /**
     * Compute the number of elements in the difference between the set stored at the first key and the sets stored at all
     * successive keys, without returning the difference itself, applying {@link SDiffCardArgs}. Non-existing keys are treated
     * as empty sets; when the first key does not exist the result is {@code 0}.
     *
     * @param keys the keys, must not be {@code null} or empty. The first key is the set to subtract from, all successive keys
     *        are subtracted.
     * @param sdiffCardArgs the command arguments ({@code LIMIT}), must not be {@code null}.
     * @return the cardinality of the difference; when a limit is set, at most that limit.
     * @throws IllegalArgumentException if {@code keys} is {@code null} or empty, or {@code sdiffCardArgs} is {@code null}.
     * @since 7.7
     */
    Mono<Long> sdiffcard(List<K> keys, SDiffCardArgs sdiffCardArgs);

    /**
     * Subtract multiple sets and store the resulting set in a key.
     *
     * @param destination the destination type: key.
     * @param keys the key.
     * @return Long integer-reply the number of elements in the resulting set.
     */
    Mono<Long> sdiffstore(K destination, K... keys);

    /**
     * Intersect multiple sets.
     *
     * @param keys the key.
     * @return V array-reply list with members of the resulting set.
     */
    Flux<V> sinter(K... keys);

    /**
     * Intersect multiple sets.
     *
     * @param channel the channel.
     * @param keys the keys.
     * @return Long count of members of the resulting set.
     * @deprecated since 6.0 in favor of consuming large results through the {@link org.reactivestreams.Publisher} returned by
     *             {@link #sinter}.
     */
    @Deprecated
    Mono<Long> sinter(ValueStreamingChannel<V> channel, K... keys);

    /**
     * This command works exactly like {@link #sinter(java.lang.Object[])} but instead of returning the result set, it returns
     * just the cardinality of the result.
     *
     * @param keys the keys.
     * @return The cardinality of the set which would result from the intersection of all the given sets.
     * @since 6.2
     */
    Mono<Long> sintercard(K... keys);

    /**
     * This command works exactly like {@link #sinter(java.lang.Object[])} but instead of returning the result set, it returns
     * just the cardinality of the result.
     *
     * @param limit If the intersection cardinality reaches limit partway through the computation, the algorithm will exit and
     *        yield limit as the cardinality.
     * @param keys the keys.
     * @return The cardinality of the set which would result from the intersection of all the given sets.
     * @since 6.2
     */
    Mono<Long> sintercard(long limit, K... keys);

    /**
     * Intersect multiple sets and store the resulting set in a key.
     *
     * @param destination the destination type: key.
     * @param keys the key.
     * @return Long integer-reply the number of elements in the resulting set.
     */
    Mono<Long> sinterstore(K destination, K... keys);

    /**
     * Determine if a given value is a member of a set.
     *
     * @param key the key.
     * @param member the member type: value.
     * @return Boolean integer-reply specifically:
     *
     *         {@code true} if the element is a member of the set. {@code false} if the element is not a member of the set, or
     *         if {@code key} does not exist.
     */
    Mono<Boolean> sismember(K key, V member);

    /**
     * Get all the members in a set.
     *
     * @param key the key.
     * @return V array-reply all elements of the set.
     */
    Flux<V> smembers(K key);

    /**
     * Get all the members in a set.
     *
     * @param channel the channel.
     * @param key the keys.
     * @return Long count of members of the resulting set.
     * @deprecated since 6.0 in favor of consuming large results through the {@link org.reactivestreams.Publisher} returned by
     *             {@link #smembers}.
     */
    @Deprecated
    Mono<Long> smembers(ValueStreamingChannel<V> channel, K key);

    /**
     * Returns whether each member is a member of the set stored at key.
     *
     * @param key the key.
     * @param members the member type: value.
     * @return Boolean array-reply list representing the membership of the given elements, in the same order as they are
     *         requested.
     * @since 6.1
     */
    Flux<Boolean> smismember(K key, V... members);

    /**
     * Move a member from one set to another.
     *
     * @param source the source key.
     * @param destination the destination type: key.
     * @param member the member type: value.
     * @return Boolean integer-reply specifically:
     *
     *         {@code true} if the element is moved. {@code false} if the element is not a member of {@code source} and no
     *         operation was performed.
     */
    Mono<Boolean> smove(K source, K destination, V member);

    /**
     * Remove and return a random member from a set.
     *
     * @param key the key.
     * @return V bulk-string-reply the removed element, or {@code null} when {@code key} does not exist.
     */
    Mono<V> spop(K key);

    /**
     * Remove and return one or multiple random members from a set.
     *
     * @param key the key.
     * @param count number of members to pop.
     * @return V bulk-string-reply the removed element, or {@code null} when {@code key} does not exist.
     */
    Flux<V> spop(K key, long count);

    /**
     * Get one random member from a set.
     *
     * @param key the key.
     * @return V bulk-string-reply without the additional {@code count} argument the command returns a Bulk Reply with the
     *         randomly selected element, or {@code null} when {@code key} does not exist.
     */
    Mono<V> srandmember(K key);

    /**
     * Get one or multiple random members from a set.
     *
     * @param key the key.
     * @param count the count type: long.
     * @return V bulk-string-reply without the additional {@code count} argument the command returns a Bulk Reply with the
     *         randomly selected element, or {@code null} when {@code key} does not exist.
     */
    Flux<V> srandmember(K key, long count);

    /**
     * Get one or multiple random members from a set.
     *
     * @param channel streaming channel that receives a call for every value.
     * @param key the key.
     * @param count the count.
     * @return Long count of members of the resulting set.
     * @deprecated since 6.0 in favor of consuming large results through the {@link org.reactivestreams.Publisher} returned by
     *             {@link #srandmember}.
     */
    @Deprecated
    Mono<Long> srandmember(ValueStreamingChannel<V> channel, K key, long count);

    /**
     * Remove one or more members from a set.
     *
     * @param key the key.
     * @param members the member type: value.
     * @return Long integer-reply the number of members that were removed from the set, not including non existing members.
     */
    Mono<Long> srem(K key, V... members);

    /**
     * Add multiple sets.
     *
     * @param keys the key.
     * @return V array-reply list with members of the resulting set.
     */
    Flux<V> sunion(K... keys);

    /**
     * Add multiple sets.
     *
     * @param channel streaming channel that receives a call for every value.
     * @param keys the keys.
     * @return Long count of members of the resulting set.
     * @deprecated since 6.0 in favor of consuming large results through the {@link org.reactivestreams.Publisher} returned by
     *             {@link #sunion}.
     */
    @Deprecated
    Mono<Long> sunion(ValueStreamingChannel<V> channel, K... keys);

    /**
     * Compute the number of distinct elements in the union of the sets stored at {@code key1} and {@code key2} without
     * returning the union itself. Non-existing keys are treated as empty sets.
     *
     * @param key1 the key of the first set, must not be {@code null}.
     * @param key2 the key of the second set, must not be {@code null}.
     * @return the cardinality of the union.
     * @throws IllegalArgumentException if {@code key1} or {@code key2} is {@code null}.
     * @since 7.7
     */
    Mono<Long> sunioncard(K key1, K key2);

    /**
     * Compute the number of distinct elements in the union of the sets stored at the given keys without returning the union
     * itself. Non-existing keys are treated as empty sets.
     *
     * @param keys the keys of the sets to union, must not be {@code null} or empty.
     * @return the cardinality of the union.
     * @throws IllegalArgumentException if {@code keys} is {@code null} or empty.
     * @since 7.7
     */
    Mono<Long> sunioncard(List<K> keys);

    /**
     * Compute the number of distinct elements in the union of the sets stored at {@code key1} and {@code key2} without
     * returning the union itself, applying {@link SUnionCardArgs}. Non-existing keys are treated as empty sets.
     *
     * @param key1 the key of the first set, must not be {@code null}.
     * @param key2 the key of the second set, must not be {@code null}.
     * @param sunionCardArgs the command arguments ({@code APPROX}, {@code LIMIT}), must not be {@code null}.
     * @return the cardinality of the union; when a limit is set, at most that limit.
     * @throws IllegalArgumentException if {@code key1}, {@code key2} or {@code sunionCardArgs} is {@code null}.
     * @since 7.7
     */
    Mono<Long> sunioncard(K key1, K key2, SUnionCardArgs sunionCardArgs);

    /**
     * Compute the number of distinct elements in the union of the sets stored at the given keys without returning the union
     * itself, applying {@link SUnionCardArgs}. Non-existing keys are treated as empty sets.
     *
     * @param keys the keys of the sets to union, must not be {@code null} or empty.
     * @param sunionCardArgs the command arguments ({@code APPROX}, {@code LIMIT}), must not be {@code null}.
     * @return the cardinality of the union; when a limit is set, at most that limit.
     * @throws IllegalArgumentException if {@code keys} is {@code null} or empty, or {@code sunionCardArgs} is {@code null}.
     * @since 7.7
     */
    Mono<Long> sunioncard(List<K> keys, SUnionCardArgs sunionCardArgs);

    /**
     * Add multiple sets and store the resulting set in a key.
     *
     * @param destination the destination type: key.
     * @param keys the key.
     * @return Long integer-reply the number of elements in the resulting set.
     */
    Mono<Long> sunionstore(K destination, K... keys);

    /**
     * Incrementally iterate Set elements.
     *
     * @param key the key.
     * @return ValueScanCursor&lt;V&gt; scan cursor.
     */
    Mono<ValueScanCursor<V>> sscan(K key);

    /**
     * Incrementally iterate Set elements.
     *
     * @param key the key.
     * @param scanArgs scan arguments.
     * @return ValueScanCursor&lt;V&gt; scan cursor.
     */
    Mono<ValueScanCursor<V>> sscan(K key, ScanArgs scanArgs);

    /**
     * Incrementally iterate Set elements.
     *
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @param scanArgs scan arguments.
     * @return ValueScanCursor&lt;V&gt; scan cursor.
     */
    Mono<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate Set elements.
     *
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @return ValueScanCursor&lt;V&gt; scan cursor.
     */
    Mono<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor);

    /**
     * Incrementally iterate Set elements.
     *
     * @param channel streaming channel that receives a call for every value.
     * @param key the key.
     * @return StreamScanCursor scan cursor.
     * @deprecated since 6.0 in favor of consuming large results through the {@link org.reactivestreams.Publisher} returned by
     *             {@link #sscan}.
     */
    @Deprecated
    Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key);

    /**
     * Incrementally iterate Set elements.
     *
     * @param channel streaming channel that receives a call for every value.
     * @param key the key.
     * @param scanArgs scan arguments.
     * @return StreamScanCursor scan cursor.
     * @deprecated since 6.0 in favor of consuming large results through the {@link org.reactivestreams.Publisher} returned by
     *             {@link #sscan}.
     */
    @Deprecated
    Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanArgs scanArgs);

    /**
     * Incrementally iterate Set elements.
     *
     * @param channel streaming channel that receives a call for every value.
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @param scanArgs scan arguments.
     * @return StreamScanCursor scan cursor.
     * @deprecated since 6.0 in favor of consuming large results through the {@link org.reactivestreams.Publisher} returned by
     *             {@link #sscan}.
     */
    @Deprecated
    Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate Set elements.
     *
     * @param channel streaming channel that receives a call for every value.
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @return StreamScanCursor scan cursor.
     * @deprecated since 6.0 in favor of consuming large results through the {@link org.reactivestreams.Publisher} returned by
     *             {@link #sscan}.
     */
    @Deprecated
    Mono<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor);

    /**
     * Set the time to live, in seconds, of one or more members of the set stored at {@code key}. A time to live of {@code 0} is
     * already in the past and deletes the members.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param seconds the time to live in seconds, must not be negative.
     * @param members the members to set the time to live for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code members} is {@code null} or empty, or its only
     *         element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpire(K key, long seconds, V... members);

    /**
     * Set the time to live, in seconds, of a single member of the set stored at {@code key}. A time to live of {@code 0} is
     * already in the past and deletes the member.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param seconds the time to live in seconds, must not be negative.
     * @param member the member to set the time to live for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code member} is an argument object such as
     *         {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> sexpire(K key, long seconds, V member);

    /**
     * Set the time to live, in seconds, of one or more members of the set stored at {@code key}, applying the
     * {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A member without an
     * expiration is treated as having an infinite one: {@code LT} succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param seconds the time to live in seconds, must not be negative.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param members the members to set the time to live for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code expireArgs} is {@code null}, or {@code members} is {@code null}
     *         or empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpire(K key, long seconds, ExpireArgs expireArgs, V... members);

    /**
     * Set the time to live, in seconds, of a single member of the set stored at {@code key}, applying the {@link ExpireArgs}
     * condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A member without an expiration is treated as
     * having an infinite one: {@code LT} succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param seconds the time to live in seconds, must not be negative.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param member the member to set the time to live for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code expireArgs} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> sexpire(K key, long seconds, ExpireArgs expireArgs, V member);

    /**
     * Set the time to live of one or more members of the set stored at {@code key}. The {@link Duration} is truncated to whole
     * seconds; a duration under one second becomes {@code 0}, which is already in the past and deletes the members.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param seconds the time to live, must not be {@code null}; truncated to whole seconds.
     * @param members the members to set the time to live for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code seconds} is {@code null}, or {@code members} is {@code null} or
     *         empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpire(K key, Duration seconds, V... members);

    /**
     * Set the time to live of a single member of the set stored at {@code key}. The {@link Duration} is truncated to whole
     * seconds; a duration under one second becomes {@code 0}, which is already in the past and deletes the member.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param seconds the time to live, must not be {@code null}; truncated to whole seconds.
     * @param member the member to set the time to live for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code seconds} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> sexpire(K key, Duration seconds, V member);

    /**
     * Set the time to live of one or more members of the set stored at {@code key}, applying the {@link ExpireArgs} condition
     * ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. The {@link Duration} is truncated to whole seconds; a
     * duration under one second becomes {@code 0}, which is already in the past and deletes the members. A member without an
     * expiration is treated as having an infinite one: {@code LT} succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param seconds the time to live, must not be {@code null}; truncated to whole seconds.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param members the members to set the time to live for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code seconds} or {@code expireArgs} is {@code null}, or
     *         {@code members} is {@code null} or empty, or its only element is an argument object such as {@link ExpireArgs}
     *         passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpire(K key, Duration seconds, ExpireArgs expireArgs, V... members);

    /**
     * Set the time to live of a single member of the set stored at {@code key}, applying the {@link ExpireArgs} condition
     * ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. The {@link Duration} is truncated to whole seconds; a
     * duration under one second becomes {@code 0}, which is already in the past and deletes the member. A member without an
     * expiration is treated as having an infinite one: {@code LT} succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param seconds the time to live, must not be {@code null}; truncated to whole seconds.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param member the member to set the time to live for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code seconds} or {@code expireArgs} is {@code null}, or {@code member}
     *         is an argument object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the
     *         members).
     * @since 8.0
     */
    Flux<Long> sexpire(K key, Duration seconds, ExpireArgs expireArgs, V member);

    /**
     * Set the time to live, in milliseconds, of one or more members of the set stored at {@code key}. A time to live of
     * {@code 0} is already in the past and deletes the members.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param milliseconds the time to live in milliseconds, must not be negative.
     * @param members the members to set the time to live for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code members} is {@code null} or empty, or its only
     *         element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spexpire(K key, long milliseconds, V... members);

    /**
     * Set the time to live, in milliseconds, of a single member of the set stored at {@code key}. A time to live of {@code 0}
     * is already in the past and deletes the member.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param milliseconds the time to live in milliseconds, must not be negative.
     * @param member the member to set the time to live for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code member} is an argument object such as
     *         {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpire(K key, long milliseconds, V member);

    /**
     * Set the time to live, in milliseconds, of one or more members of the set stored at {@code key}, applying the
     * {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A member without an
     * expiration is treated as having an infinite one: {@code LT} succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param milliseconds the time to live in milliseconds, must not be negative.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param members the members to set the time to live for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code expireArgs} is {@code null}, or {@code members} is {@code null}
     *         or empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spexpire(K key, long milliseconds, ExpireArgs expireArgs, V... members);

    /**
     * Set the time to live, in milliseconds, of a single member of the set stored at {@code key}, applying the
     * {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A member without an
     * expiration is treated as having an infinite one: {@code LT} succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param milliseconds the time to live in milliseconds, must not be negative.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param member the member to set the time to live for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code expireArgs} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpire(K key, long milliseconds, ExpireArgs expireArgs, V member);

    /**
     * Set the time to live of one or more members of the set stored at {@code key}. The {@link Duration} is truncated to whole
     * milliseconds; a duration under one millisecond becomes {@code 0}, which is already in the past and deletes the members.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param milliseconds the time to live, must not be {@code null}; truncated to whole milliseconds.
     * @param members the members to set the time to live for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code milliseconds} is {@code null}, or {@code members} is
     *         {@code null} or empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of
     *         the members.
     * @since 8.0
     */
    Flux<Long> spexpire(K key, Duration milliseconds, V... members);

    /**
     * Set the time to live of a single member of the set stored at {@code key}. The {@link Duration} is truncated to whole
     * milliseconds; a duration under one millisecond becomes {@code 0}, which is already in the past and deletes the member.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param milliseconds the time to live, must not be {@code null}; truncated to whole milliseconds.
     * @param member the member to set the time to live for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code milliseconds} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpire(K key, Duration milliseconds, V member);

    /**
     * Set the time to live of one or more members of the set stored at {@code key}, applying the {@link ExpireArgs} condition
     * ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. The {@link Duration} is truncated to whole
     * milliseconds; a duration under one millisecond becomes {@code 0}, which is already in the past and deletes the members. A
     * member without an expiration is treated as having an infinite one: {@code LT} succeeds on it, {@code GT} and {@code XX}
     * fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param milliseconds the time to live, must not be {@code null}; truncated to whole milliseconds.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param members the members to set the time to live for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code milliseconds} or {@code expireArgs} is {@code null}, or
     *         {@code members} is {@code null} or empty, or its only element is an argument object such as {@link ExpireArgs}
     *         passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spexpire(K key, Duration milliseconds, ExpireArgs expireArgs, V... members);

    /**
     * Set the time to live of a single member of the set stored at {@code key}, applying the {@link ExpireArgs} condition
     * ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. The {@link Duration} is truncated to whole
     * milliseconds; a duration under one millisecond becomes {@code 0}, which is already in the past and deletes the member. A
     * member without an expiration is treated as having an infinite one: {@code LT} succeeds on it, {@code GT} and {@code XX}
     * fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param milliseconds the time to live, must not be {@code null}; truncated to whole milliseconds.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param member the member to set the time to live for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code milliseconds} or {@code expireArgs} is {@code null}, or
     *         {@code member} is an argument object such as {@link ExpireArgs} passed in place of the members (pass the
     *         arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpire(K key, Duration milliseconds, ExpireArgs expireArgs, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute Unix time in seconds. A time in
     * the past deletes the members.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the Unix time in seconds at which the members expire, must not be negative.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code members} is {@code null} or empty, or its only
     *         element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, long timestamp, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute Unix time in seconds. A time in the
     * past deletes the member.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the Unix time in seconds at which the members expire, must not be negative.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code member} is an argument object such as
     *         {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, long timestamp, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute Unix time in seconds, applying
     * the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A time in the past
     * deletes the members. A member without an expiration is treated as having an infinite one: {@code LT} succeeds on it,
     * {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the Unix time in seconds at which the members expire, must not be negative.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code expireArgs} is {@code null}, or {@code members} is {@code null}
     *         or empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, long timestamp, ExpireArgs expireArgs, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute Unix time in seconds, applying the
     * {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A time in the past
     * deletes the member. A member without an expiration is treated as having an infinite one: {@code LT} succeeds on it,
     * {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the Unix time in seconds at which the members expire, must not be negative.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code expireArgs} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, long timestamp, ExpireArgs expireArgs, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute point in time, with second
     * precision. A time in the past deletes the members.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}; truncated to whole seconds.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code timestamp} is {@code null}, or {@code members} is {@code null}
     *         or empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, Date timestamp, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute point in time, with second
     * precision. A time in the past deletes the member.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}; truncated to whole seconds.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code timestamp} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, Date timestamp, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute point in time, with second
     * precision, applying the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A
     * time in the past deletes the members. A member without an expiration is treated as having an infinite one: {@code LT}
     * succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}; truncated to whole seconds.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code timestamp} or {@code expireArgs} is {@code null}, or
     *         {@code members} is {@code null} or empty, or its only element is an argument object such as {@link ExpireArgs}
     *         passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, Date timestamp, ExpireArgs expireArgs, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute point in time, with second
     * precision, applying the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A
     * time in the past deletes the member. A member without an expiration is treated as having an infinite one: {@code LT}
     * succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}; truncated to whole seconds.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code timestamp} or {@code expireArgs} is {@code null}, or
     *         {@code member} is an argument object such as {@link ExpireArgs} passed in place of the members (pass the
     *         arguments before the members).
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, Date timestamp, ExpireArgs expireArgs, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute point in time, with second
     * precision. A time in the past deletes the members.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}; truncated to whole seconds.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code timestamp} is {@code null}, or {@code members} is {@code null}
     *         or empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, Instant timestamp, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute point in time, with second
     * precision. A time in the past deletes the member.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}; truncated to whole seconds.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code timestamp} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, Instant timestamp, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute point in time, with second
     * precision, applying the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A
     * time in the past deletes the members. A member without an expiration is treated as having an infinite one: {@code LT}
     * succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}; truncated to whole seconds.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code timestamp} or {@code expireArgs} is {@code null}, or
     *         {@code members} is {@code null} or empty, or its only element is an argument object such as {@link ExpireArgs}
     *         passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, Instant timestamp, ExpireArgs expireArgs, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute point in time, with second
     * precision, applying the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A
     * time in the past deletes the member. A member without an expiration is treated as having an infinite one: {@code LT}
     * succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}; truncated to whole seconds.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code timestamp} or {@code expireArgs} is {@code null}, or
     *         {@code member} is an argument object such as {@link ExpireArgs} passed in place of the members (pass the
     *         arguments before the members).
     * @since 8.0
     */
    Flux<Long> sexpireat(K key, Instant timestamp, ExpireArgs expireArgs, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute Unix time in milliseconds. A
     * time in the past deletes the members.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the Unix time in milliseconds at which the members expire, must not be negative.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code members} is {@code null} or empty, or its only
     *         element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, long timestamp, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute Unix time in milliseconds. A time
     * in the past deletes the member.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the Unix time in milliseconds at which the members expire, must not be negative.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code member} is an argument object such as
     *         {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, long timestamp, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute Unix time in milliseconds,
     * applying the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A time in
     * the past deletes the members. A member without an expiration is treated as having an infinite one: {@code LT} succeeds on
     * it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the Unix time in milliseconds at which the members expire, must not be negative.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code expireArgs} is {@code null}, or {@code members} is {@code null}
     *         or empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, long timestamp, ExpireArgs expireArgs, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute Unix time in milliseconds, applying
     * the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A time in the past
     * deletes the member. A member without an expiration is treated as having an infinite one: {@code LT} succeeds on it,
     * {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the Unix time in milliseconds at which the members expire, must not be negative.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code expireArgs} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, long timestamp, ExpireArgs expireArgs, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute point in time, with millisecond
     * precision. A time in the past deletes the members.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code timestamp} is {@code null}, or {@code members} is {@code null}
     *         or empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, Date timestamp, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute point in time, with millisecond
     * precision. A time in the past deletes the member.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code timestamp} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, Date timestamp, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute point in time, with millisecond
     * precision, applying the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A
     * time in the past deletes the members. A member without an expiration is treated as having an infinite one: {@code LT}
     * succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code timestamp} or {@code expireArgs} is {@code null}, or
     *         {@code members} is {@code null} or empty, or its only element is an argument object such as {@link ExpireArgs}
     *         passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, Date timestamp, ExpireArgs expireArgs, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute point in time, with millisecond
     * precision, applying the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A
     * time in the past deletes the member. A member without an expiration is treated as having an infinite one: {@code LT}
     * succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code timestamp} or {@code expireArgs} is {@code null}, or
     *         {@code member} is an argument object such as {@link ExpireArgs} passed in place of the members (pass the
     *         arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, Date timestamp, ExpireArgs expireArgs, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute point in time, with millisecond
     * precision. A time in the past deletes the members.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code timestamp} is {@code null}, or {@code members} is {@code null}
     *         or empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, Instant timestamp, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute point in time, with millisecond
     * precision. A time in the past deletes the member.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} or {@code timestamp} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, Instant timestamp, V member);

    /**
     * Set the expiration of one or more members of the set stored at {@code key} to an absolute point in time, with millisecond
     * precision, applying the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A
     * time in the past deletes the members. A member without an expiration is treated as having an infinite one: {@code LT}
     * succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param members the members to set the expiration for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 2} if the member was
     *         deleted because the expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a
     *         {@code NX}, {@code XX}, {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code timestamp} or {@code expireArgs} is {@code null}, or
     *         {@code members} is {@code null} or empty, or its only element is an argument object such as {@link ExpireArgs}
     *         passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, Instant timestamp, ExpireArgs expireArgs, V... members);

    /**
     * Set the expiration of a single member of the set stored at {@code key} to an absolute point in time, with millisecond
     * precision, applying the {@link ExpireArgs} condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}) to each member. A
     * time in the past deletes the member. A member without an expiration is treated as having an infinite one: {@code LT}
     * succeeds on it, {@code GT} and {@code XX} fail.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param timestamp the point in time at which the members expire, must not be {@code null}.
     * @param expireArgs the condition ({@code NX}, {@code XX}, {@code GT} or {@code LT}), must not be {@code null}.
     * @param member the member to set the expiration for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 2} if the member was deleted because the
     *         expiration is in the past; {@code 1} if the expiration was set or updated; {@code 0} if a {@code NX}, {@code XX},
     *         {@code GT} or {@code LT} condition was not met; {@code -2} if the member does not exist (a logically expired
     *         member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key}, {@code timestamp} or {@code expireArgs} is {@code null}, or
     *         {@code member} is an argument object such as {@link ExpireArgs} passed in place of the members (pass the
     *         arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpireat(K key, Instant timestamp, ExpireArgs expireArgs, V member);

    /**
     * Get the remaining time to live, in seconds, of one or more members of the set stored at {@code key}. The value is rounded
     * up to the next second, so a live member never returns {@code 0}.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param members the members to get the time to live for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: the remaining time to live
     *         in seconds, rounded up to the next second; {@code -1} if the member exists but has no expiration; {@code -2} if
     *         the member does not exist (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code members} is {@code null} or empty, or its only
     *         element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sttl(K key, V... members);

    /**
     * Get the remaining time to live, in seconds, of a single member of the set stored at {@code key}. The value is rounded up
     * to the next second, so a live member never returns {@code 0}.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param member the member to get the time to live for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: the remaining time to live in seconds, rounded up to
     *         the next second; {@code -1} if the member exists but has no expiration; {@code -2} if the member does not exist
     *         (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code member} is an argument object such as
     *         {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> sttl(K key, V member);

    /**
     * Get the remaining time to live, in milliseconds, of one or more members of the set stored at {@code key}.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param members the members to get the time to live for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: the remaining time to live
     *         in milliseconds, {@code 0} if the member is expiring now; {@code -1} if the member exists but has no expiration;
     *         {@code -2} if the member does not exist (a logically expired member does not exist) or {@code key} does not
     *         exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code members} is {@code null} or empty, or its only
     *         element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spttl(K key, V... members);

    /**
     * Get the remaining time to live, in milliseconds, of a single member of the set stored at {@code key}.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param member the member to get the time to live for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: the remaining time to live in milliseconds,
     *         {@code 0} if the member is expiring now; {@code -1} if the member exists but has no expiration; {@code -2} if the
     *         member does not exist (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code member} is an argument object such as
     *         {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> spttl(K key, V member);

    /**
     * Get the absolute Unix time, in seconds, at which one or more members of the set stored at {@code key} expire. The value
     * is rounded up to the next second.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param members the members to get the expiration time for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: the Unix time in seconds at
     *         which the member expires, rounded up to the next second; {@code -1} if the member exists but has no expiration;
     *         {@code -2} if the member does not exist (a logically expired member does not exist) or {@code key} does not
     *         exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code members} is {@code null} or empty, or its only
     *         element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> sexpiretime(K key, V... members);

    /**
     * Get the absolute Unix time, in seconds, at which a single member of the set stored at {@code key} expire. The value is
     * rounded up to the next second.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param member the member to get the expiration time for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: the Unix time in seconds at which the member
     *         expires, rounded up to the next second; {@code -1} if the member exists but has no expiration; {@code -2} if the
     *         member does not exist (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code member} is an argument object such as
     *         {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> sexpiretime(K key, V member);

    /**
     * Get the absolute Unix time, in milliseconds, at which one or more members of the set stored at {@code key} expire.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param members the members to get the expiration time for, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: the Unix time in
     *         milliseconds at which the member expires; {@code -1} if the member exists but has no expiration; {@code -2} if
     *         the member does not exist (a logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code members} is {@code null} or empty, or its only
     *         element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spexpiretime(K key, V... members);

    /**
     * Get the absolute Unix time, in milliseconds, at which a single member of the set stored at {@code key} expire.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param member the member to get the expiration time for.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: the Unix time in milliseconds at which the member
     *         expires; {@code -1} if the member exists but has no expiration; {@code -2} if the member does not exist (a
     *         logically expired member does not exist) or {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code member} is an argument object such as
     *         {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> spexpiretime(K key, V member);

    /**
     * Remove the expiration of one or more members of the set stored at {@code key}.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param members the members to remove the expiration from, must not be {@code null} or empty.
     * @return a {@link Flux} emitting one {@link Long} per member, in the order of {@code members}: {@code 1} if the expiration
     *         was removed; {@code -1} if the member exists but has no expiration; {@code -2} if the member does not exist or
     *         {@code key} does not exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code members} is {@code null} or empty, or its only
     *         element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Flux<Long> spersist(K key, V... members);

    /**
     * Remove the expiration of a single member of the set stored at {@code key}.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param member the member to remove the expiration from.
     * @return a {@link Flux} emitting one {@link Long} for {@code member}: {@code 1} if the expiration was removed; {@code -1}
     *         if the member exists but has no expiration; {@code -2} if the member does not exist or {@code key} does not
     *         exist.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code member} is an argument object such as
     *         {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Flux<Long> spersist(K key, V member);

    /**
     * Add one or more members to the set stored at {@code key}, creating it when it does not exist. Unlike
     * {@link #sadd(Object, Object[])}, a member that already exists is not skipped: its expiration is discarded. Without
     * {@link SAddExArgs} the command behaves like {@code SADD} for members without an expiration.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param members the members to add, must not be {@code null} or empty.
     * @return a {@link Mono} emitting {@code 1} if at least one member was added, updated or deleted; {@code 0} if nothing
     *         changed.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code members} is {@code null} or empty, or its only
     *         element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Mono<Long> saddex(K key, V... members);

    /**
     * Add a single member to the set stored at {@code key}, creating it when it does not exist. Unlike
     * {@link #sadd(Object, Object[])}, a member that already exists is not skipped: its expiration is discarded. Without
     * {@link SAddExArgs} the command behaves like {@code SADD} for members without an expiration.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param member the member to add.
     * @return a {@link Mono} emitting {@code 1} if at least one member was added, updated or deleted; {@code 0} if nothing
     *         changed.
     * @throws IllegalArgumentException if {@code key} is {@code null}, or {@code member} is an argument object such as
     *         {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Mono<Long> saddex(K key, V member);

    /**
     * Add one or more members to the set stored at {@code key} and set their expiration, applying {@link SAddExArgs}.
     * {@code MNX} adds only if none of the members exists, {@code MXX} only if all of them exist; both apply to the whole
     * command. {@code EX}, {@code PX}, {@code EXAT} or {@code PXAT} set the expiration of every member in the command,
     * {@code KEEPTTL} keeps the expiration a member already has; without any of them an existing member loses its expiration.
     * An expiration in the past deletes the members that exist and adds none.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param sAddExArgs the command arguments, must not be {@code null}.
     * @param members the members to add, must not be {@code null} or empty.
     * @return a {@link Mono} emitting {@code 1} if at least one member was added, updated or deleted; {@code 0} if the
     *         {@code MNX} or {@code MXX} condition was not met, or nothing changed.
     * @throws IllegalArgumentException if {@code key} or {@code sAddExArgs} is {@code null}, or {@code members} is {@code null}
     *         or empty, or its only element is an argument object such as {@link ExpireArgs} passed in place of the members.
     * @since 8.0
     */
    Mono<Long> saddex(K key, SAddExArgs sAddExArgs, V... members);

    /**
     * Add a single member to the set stored at {@code key} and set its expiration, applying {@link SAddExArgs}. {@code MNX}
     * adds only if none of the members exists, {@code MXX} only if all of them exist; both apply to the whole command.
     * {@code EX}, {@code PX}, {@code EXAT} or {@code PXAT} set the expiration of every member in the command, {@code KEEPTTL}
     * keeps the expiration a member already has; without any of them an existing member loses its expiration. An expiration in
     * the past deletes the member if it exists and adds nothing.
     *
     * @param key the key of the set, must not be {@code null}.
     * @param sAddExArgs the command arguments, must not be {@code null}.
     * @param member the member to add.
     * @return a {@link Mono} emitting {@code 1} if at least one member was added, updated or deleted; {@code 0} if the
     *         {@code MNX} or {@code MXX} condition was not met, or nothing changed.
     * @throws IllegalArgumentException if {@code key} or {@code sAddExArgs} is {@code null}, or {@code member} is an argument
     *         object such as {@link ExpireArgs} passed in place of the members (pass the arguments before the members).
     * @since 8.0
     */
    Mono<Long> saddex(K key, SAddExArgs sAddExArgs, V member);

}
