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
package io.lettuce.core.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.lettuce.core.ExpireArgs;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.HGetExArgs;
import io.lettuce.core.HSetExArgs;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.KeyValueStreamingChannel;
import io.lettuce.core.output.ValueStreamingChannel;

/**
 * ${intent} for Hashes (Key-Value pairs).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface RedisHashCommands<K, V> {

    /**
     * Delete one or more hash fields.
     *
     * @param key the key.
     * @param fields the field type: key.
     * @return Long integer-reply the number of fields that were removed from the hash, not including specified but non existing
     *         fields.
     */
    Long hdel(K key, K... fields);

    /**
     * Determine if a hash field exists.
     *
     * @param key the key.
     * @param field the field type: key.
     * @return Boolean integer-reply specifically:
     *
     *         {@code true} if the hash contains {@code field}. {@code false} if the hash does not contain {@code field}, or
     *         {@code key} does not exist.
     */
    Boolean hexists(K key, K field);

    /**
     * Get the value of a hash field.
     *
     * @param key the key.
     * @param field the field type: key.
     * @return V bulk-string-reply the value associated with {@code field}, or {@code null} when {@code field} is not present in
     *         the hash or {@code key} does not exist.
     */
    V hget(K key, K field);

    /**
     * Increment the integer value of a hash field by the given number.
     *
     * @param key the key.
     * @param field the field type: key.
     * @param amount the increment type: long.
     * @return Long integer-reply the value at {@code field} after the increment operation.
     */
    Long hincrby(K key, K field, long amount);

    /**
     * Increment the float value of a hash field by the given amount.
     *
     * @param key the key.
     * @param field the field type: key.
     * @param amount the increment type: double.
     * @return Double bulk-string-reply the value of {@code field} after the increment.
     */
    Double hincrbyfloat(K key, K field, double amount);

    /**
     * Get all the fields and values in a hash.
     *
     * @param key the key.
     * @return Map&lt;K,V&gt; array-reply list of fields and their values stored in the hash, or an empty list when {@code key}
     *         does not exist.
     */
    Map<K, V> hgetall(K key);

    /**
     * Stream over all the fields and values in a hash.
     *
     * @param channel the channel.
     * @param key the key.
     * @return Long count of the keys.
     */
    Long hgetall(KeyValueStreamingChannel<K, V> channel, K key);

    /**
     * Get all the fields in a hash.
     *
     * @param key the key.
     * @return List&lt;K&gt; array-reply list of fields in the hash, or an empty list when {@code key} does not exist.
     */
    List<K> hkeys(K key);

    /**
     * Stream over all the fields in a hash.
     *
     * @param channel the channel.
     * @param key the key.
     * @return Long count of the keys.
     */
    Long hkeys(KeyStreamingChannel<K> channel, K key);

    /**
     * Get the number of fields in a hash.
     *
     * @param key the key.
     * @return Long integer-reply number of fields in the hash, or {@code 0} when {@code key} does not exist.
     */
    Long hlen(K key);

    /**
     * Get the values of all the given hash fields.
     *
     * @param key the key.
     * @param fields the field type: key.
     * @return List&lt;V&gt; array-reply list of values associated with the given fields, in the same.
     */
    List<KeyValue<K, V>> hmget(K key, K... fields);

    /**
     * Stream over the values of all the given hash fields.
     *
     * @param channel the channel.
     * @param key the key.
     * @param fields the fields.
     * @return Long count of the keys.
     */
    Long hmget(KeyValueStreamingChannel<K, V> channel, K key, K... fields);

    /**
     * Set multiple hash fields to multiple values.
     *
     * @param key the key.
     * @param map the hash to apply.
     * @return String simple-string-reply.
     */
    String hmset(K key, Map<K, V> map);

    /**
     * Return a random field from the hash stored at {@code key}.
     *
     * @param key the key.
     * @return hash field name.
     * @since 6.1
     */
    K hrandfield(K key);

    /**
     * Return {@code count} random fields from the hash stored at {@code key}.
     *
     * @param key the key.
     * @param count the number of fields to return. If the provided count argument is positive, return an array of distinct
     *        fields.
     * @return array-reply list of field names.
     * @since 6.1
     */
    List<K> hrandfield(K key, long count);

    /**
     * Return a random field along its value from the hash stored at {@code key}.
     *
     * @param key the key.
     * @return array-reply the key and value.
     * @since 6.1
     */
    KeyValue<K, V> hrandfieldWithvalues(K key);

    /**
     * Return {@code count} random fields along their value from the hash stored at {@code key}.
     *
     * @param key the key.
     * @param count the number of fields to return. If the provided count argument is positive, return an array of distinct
     *        fields.
     * @return array-reply the keys and values.
     * @since 6.1
     */
    List<KeyValue<K, V>> hrandfieldWithvalues(K key, long count);

    /**
     * Incrementally iterate hash fields and associated values.
     *
     * @param key the key.
     * @return MapScanCursor&lt;K, V&gt; map scan cursor.
     */
    MapScanCursor<K, V> hscan(K key);

    /**
     * Incrementally iterate hash fields, without associated values.
     *
     * @param key the key.
     * @return KeyScanCursor&lt;K&gt; key scan cursor.
     * @since 6.4
     */
    KeyScanCursor<K> hscanNovalues(K key);

    /**
     * Incrementally iterate hash fields and associated values.
     *
     * @param key the key.
     * @param scanArgs scan arguments.
     * @return MapScanCursor&lt;K, V&gt; map scan cursor.
     */
    MapScanCursor<K, V> hscan(K key, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields, without associated values.
     *
     * @param key the key.
     * @param scanArgs scan arguments.
     * @return KeyScanCursor&lt;K&gt; key scan cursor.
     * @since 6.4
     */
    KeyScanCursor<K> hscanNovalues(K key, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields and associated values.
     *
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @param scanArgs scan arguments.
     * @return MapScanCursor&lt;K, V&gt; map scan cursor.
     */
    MapScanCursor<K, V> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields, without associated values.
     *
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @param scanArgs scan arguments.
     * @return KeyScanCursor&lt;K&gt; key scan cursor.
     * @since 6.4
     */
    KeyScanCursor<K> hscanNovalues(K key, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields and associated values.
     *
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @return MapScanCursor&lt;K, V&gt; map scan cursor.
     */
    MapScanCursor<K, V> hscan(K key, ScanCursor scanCursor);

    /**
     * Incrementally iterate hash fields, without associated values.
     *
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @return KeyScanCursor&lt;K&gt; key scan cursor.
     * @since 6.4
     */
    KeyScanCursor<K> hscanNovalues(K key, ScanCursor scanCursor);

    /**
     * Incrementally iterate hash fields and associated values.
     *
     * @param channel streaming channel that receives a call for every key-value pair.
     * @param key the key.
     * @return StreamScanCursor scan cursor.
     */
    StreamScanCursor hscan(KeyValueStreamingChannel<K, V> channel, K key);

    /**
     * Incrementally iterate hash fields, without associated values.
     *
     * @param channel streaming channel that receives a call for every key.
     * @param key the key.
     * @return StreamScanCursor scan cursor.
     * @since 6.4
     */
    StreamScanCursor hscanNovalues(KeyStreamingChannel<K> channel, K key);

    /**
     * Incrementally iterate hash fields and associated values.
     *
     * @param channel streaming channel that receives a call for every key-value pair.
     * @param key the key.
     * @param scanArgs scan arguments.
     * @return StreamScanCursor scan cursor.
     */
    StreamScanCursor hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields, without associated values.
     *
     * @param channel streaming channel that receives a call for every key.
     * @param key the key.
     * @param scanArgs scan arguments.
     * @return StreamScanCursor scan cursor.
     * @since 6.4
     */
    StreamScanCursor hscanNovalues(KeyStreamingChannel<K> channel, K key, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields and associated values.
     *
     * @param channel streaming channel that receives a call for every key-value pair.
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @param scanArgs scan arguments.
     * @return StreamScanCursor scan cursor.
     */
    StreamScanCursor hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields, without associated values.
     *
     * @param channel streaming channel that receives a call for every key.
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @param scanArgs scan arguments.
     * @return StreamScanCursor scan cursor.
     * @since 6.4
     */
    StreamScanCursor hscanNovalues(KeyStreamingChannel<K> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields and associated values.
     *
     * @param channel streaming channel that receives a call for every key-value pair.
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @return StreamScanCursor scan cursor.
     */
    StreamScanCursor hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor);

    /**
     * Incrementally iterate hash fields, without associated values.
     *
     * @param channel streaming channel that receives a call for every key.
     * @param key the key.
     * @param scanCursor cursor to resume from a previous scan, must not be {@code null}.
     * @return StreamScanCursor scan cursor.
     * @since 6.4
     */
    StreamScanCursor hscanNovalues(KeyStreamingChannel<K> channel, K key, ScanCursor scanCursor);

    /**
     * Set the string value of a hash field.
     *
     * @param key the key.
     * @param field the field type: key.
     * @param value the value.
     * @return Boolean integer-reply specifically:
     *
     *         {@code true} if {@code field} is a new field in the hash and {@code value} was set. {@code false} if
     *         {@code field} already exists in the hash and the value was updated.
     */
    Boolean hset(K key, K field, V value);

    /**
     * Set multiple hash fields to multiple values.
     *
     * @param key the key of the hash.
     * @param map the field/value pairs to update.
     * @return Long integer-reply: the number of fields that were added.
     * @since 5.3
     */
    Long hset(K key, Map<K, V> map);

    /**
     * Set the value of one or more fields of a given hash key, and optionally set their expiration
     *
     * @param key the key of the hash.
     * @param map the field/value pairs to update.
     * @return Long long-reply: 0 if no fields were set, 1 if all the fields were set
     * @since 6.6
     */
    Long hsetex(K key, Map<K, V> map);

    /**
     * Set the value of one or more fields of a given hash key, and optionally set their expiration
     *
     * @param key the key of the hash.
     * @param hSetExArgs hsetex arguments.
     * @param map the field/value pairs to update.
     * @return Long long-reply: 0 if no fields were set, 1 if all the fields were set
     * @since 6.6
     */
    Long hsetex(K key, HSetExArgs hSetExArgs, Map<K, V> map);

    /**
     * Get the value of one or more fields of a given hash key, and optionally set their expiration
     *
     * @param key the key of the hash.
     * @param fields fields to retrieve.
     * @return List&lt;KeyValue&lt;K, V&gt;&gt; array-reply list of fields and their values.
     * @since 6.6
     */
    List<KeyValue<K, V>> hgetex(K key, K... fields);

    /**
     * Get the value of one or more fields of a given hash key, and optionally set their expiration
     *
     * @param key the key of the hash.
     * @param hGetExArgs hgetex arguments.
     * @param fields fields to retrieve.
     * @return List&lt;KeyValue&lt;K, V&gt;&gt; array-reply list of fields and their values.
     * @since 6.6
     */
    List<KeyValue<K, V>> hgetex(K key, HGetExArgs hGetExArgs, K... fields);

    /**
     * Stream over the values of all the given hash fields.
     *
     * @param channel the channel.
     * @param key the key.
     * @param hGetExArgs hgetex arguments.
     * @param fields fields to retrieve.
     * @return Long the number of fields that were removed from the hash.
     */
    Long hgetex(KeyValueStreamingChannel<K, V> channel, K key, HGetExArgs hGetExArgs, K... fields);

    /**
     * Get and delete one or more hash fields.
     *
     * @param key the hash key.
     * @param fields fields to retrieve and delete.
     * @return List&lt;KeyValue&lt;K, V&gt;&gt; array-reply list of fields and their values.
     */
    List<KeyValue<K, V>> hgetdel(K key, K... fields);

    /**
     * Stream over the values of all the given hash fields.
     *
     * @param channel the channel.
     * @param key the key.
     * @param fields fields to retrieve and delete.
     * @return Long the number of fields that were removed from the hash.
     */
    Long hgetdel(KeyValueStreamingChannel<K, V> channel, K key, K... fields);

    /**
     * Set the value of a hash field, only if the field does not exist.
     *
     * @param key the key.
     * @param field the field type: key.
     * @param value the value.
     * @return Boolean integer-reply specifically:
     *
     *         {@code 1} if {@code field} is a new field in the hash and {@code value} was set. {@code 0} if {@code field}
     *         already exists in the hash and no operation was performed.
     */
    Boolean hsetnx(K key, K field, V value);

    /**
     * Get the string length of the field value in a hash.
     *
     * @param key the key.
     * @param field the field type: key.
     * @return Long integer-reply the string length of the {@code field} value, or {@code 0} when {@code field} is not present
     *         in the hash or {@code key} does not exist at all.
     */
    Long hstrlen(K key, K field);

    /**
     * Get all the values in a hash.
     *
     * @param key the key.
     * @return List&lt;V&gt; array-reply list of values in the hash, or an empty list when {@code key} does not exist.
     */
    List<V> hvals(K key);

    /**
     * Stream over all the values in a hash.
     *
     * @param channel streaming channel that receives a call for every value.
     * @param key the key.
     * @return Long count of the keys.
     */
    Long hvals(ValueStreamingChannel<V> channel, K key);

    /**
     * Set the time to live (in seconds) for one or more fields, belonging to a certain key.
     *
     * @param key the key of the fields.
     * @param seconds the seconds type: long.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is 0; {@code 1} indicating expiration time is set/updated;
     *         {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not met);
     *         {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpire(K key, long seconds, K... fields);

    /**
     * Set the time to live (in seconds) for one or more fields, belonging to a certain key.
     *
     * @param key the key of the fields.
     * @param seconds the seconds type: long.
     * @param expireArgs the expiry arguments.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is 0; {@code 1} indicating expiration time is set/updated;
     *         {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not met);
     *         {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpire(K key, long seconds, ExpireArgs expireArgs, K... fields);

    /**
     * Set the time to live for one or more fields, belonging to a certain key.
     *
     * @param key the key.
     * @param seconds the TTL {@link Duration}
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is 0; {@code 1} indicating expiration time is set/updated;
     *         {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not met);
     *         {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpire(K key, Duration seconds, K... fields);

    /**
     * Set the time to live for one or more fields, belonging to a certain key.
     *
     * @param key the key.
     * @param seconds the TTL {@link Duration}
     * @param expireArgs the {@link ExpireArgs}.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is 0; {@code 1} indicating expiration time is set/updated;
     *         {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not met);
     *         {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpire(K key, Duration seconds, ExpireArgs expireArgs, K... fields);

    /**
     * Set the time to live for one or more fields, belonging to a certain key as a UNIX timestamp.
     *
     * @param key the key.
     * @param timestamp the timestamp type: posix time.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpireat(K key, long timestamp, K... fields);

    /**
     * Set the time to live for one or more fields, belonging to a certain key as a UNIX timestamp.
     *
     * @param key the key.
     * @param timestamp the timestamp type: posix time.
     * @param expireArgs the expiry arguments.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpireat(K key, long timestamp, ExpireArgs expireArgs, K... fields);

    /**
     * Set the time to live for one or more fields, belonging to a certain key as a UNIX timestamp.
     *
     * @param key the key.
     * @param timestamp the timestamp type: posix time.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpireat(K key, Date timestamp, K... fields);

    /**
     * Set the time to live for one or more fields, belonging to a certain key as a UNIX timestamp.
     *
     * @param key the key.
     * @param timestamp the timestamp type: posix time.
     * @param expireArgs the expiry arguments.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpireat(K key, Date timestamp, ExpireArgs expireArgs, K... fields);

    /**
     * Set the time to live for one or more fields, belonging to a certain key as a UNIX timestamp.
     *
     * @param key the key.
     * @param timestamp the timestamp type: posix time.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpireat(K key, Instant timestamp, K... fields);

    /**
     * Set the time to live for one or more fields, belonging to a certain key as a UNIX timestamp.
     *
     * @param key the key.
     * @param timestamp the timestamp type: posix time.
     * @param expireArgs the expiry arguments.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpireat(K key, Instant timestamp, ExpireArgs expireArgs, K... fields);

    /**
     * Get the time to live for one or more fields in as UNIX timestamp in seconds.
     *
     * @param key the key.
     * @param fields one or more fields to get the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: expiration time as a UNIX timestamp in seconds;
     *         {@code -1} indicating the field has no expiry time set; {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hexpiretime(K key, K... fields);

    /**
     * Remove the expiration from one or more fields.
     *
     * @param key the key.
     * @param fields one or more fields to remove the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 1} indicating expiration time is removed;
     *         {@code -1} field has no expiration time to be removed; {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpersist(K key, K... fields);

    /**
     * Set the time to live for one or more fields in milliseconds.
     *
     * @param key the key.
     * @param milliseconds the milliseconds type: long.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is 0; {@code 1} indicating expiration time is set/updated;
     *         {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not met);
     *         {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpexpire(K key, long milliseconds, K... fields);

    /**
     * Set the time to live for one or more fields in milliseconds.
     *
     * @param key the key.
     * @param milliseconds the milliseconds type: long.
     * @param expireArgs the expiry arguments.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is 0; {@code 1} indicating expiration time is set/updated;
     *         {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not met);
     *         {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpexpire(K key, long milliseconds, ExpireArgs expireArgs, K... fields);

    /**
     * Set the time to live for one or more fields in milliseconds.
     *
     * @param key the key.
     * @param milliseconds the milliseconds.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is 0; {@code 1} indicating expiration time is set/updated;
     *         {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not met);
     *         {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpexpire(K key, Duration milliseconds, K... fields);

    /**
     * Set the time to live for one or more fields in milliseconds.
     *
     * @param key the key.
     * @param milliseconds the milliseconds.
     * @param expireArgs the expiry arguments.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is 0; {@code 1} indicating expiration time is set/updated;
     *         {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not met);
     *         {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpexpire(K key, Duration milliseconds, ExpireArgs expireArgs, K... fields);

    /**
     * Set the time to live for one or more fields as a UNIX timestamp specified in milliseconds.
     *
     * @param key the key.
     * @param timestamp the milliseconds-timestamp type: posix time.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpexpireat(K key, long timestamp, K... fields);

    /**
     * Set the time to live for one or more fields as a UNIX timestamp specified in milliseconds.
     *
     * @param key the key.
     * @param timestamp the milliseconds-timestamp type: posix time.
     * @param expireArgs the expiry arguments.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpexpireat(K key, long timestamp, ExpireArgs expireArgs, K... fields);

    /**
     * Set the time to live for one or more fields as a UNIX timestamp specified in milliseconds.
     *
     * @param key the key.
     * @param timestamp the milliseconds-timestamp type: posix time.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpexpireat(K key, Date timestamp, K... fields);

    /**
     * Set the time to live for one or more fields as a UNIX timestamp specified in milliseconds.
     *
     * @param key the key.
     * @param timestamp the milliseconds-timestamp type: posix time.
     * @param expireArgs the expiry arguments.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpexpireat(K key, Date timestamp, ExpireArgs expireArgs, K... fields);

    /**
     * Set the time to live for one or more fields as a UNIX timestamp specified in milliseconds.
     *
     * @param key the key.
     * @param timestamp the milliseconds-timestamp type: posix time.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpexpireat(K key, Instant timestamp, K... fields);

    /**
     * Set the time to live for one or more fields as a UNIX timestamp specified in milliseconds.
     *
     * @param key the key.
     * @param timestamp the milliseconds-timestamp type: posix time.
     * @param expireArgs the expiry arguments.
     * @param fields one or more fields to set the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: {@code 2} indicating the specific field is deleted
     *         already due to expiration, or provided expiry interval is in the past; {@code 1} indicating expiration time is
     *         set/updated; {@code 0} indicating the expiration time is not set (a provided NX | XX | GT | LT condition is not
     *         met); {@code -2} indicating there is no such field
     * @since 6.4
     */
    List<Long> hpexpireat(K key, Instant timestamp, ExpireArgs expireArgs, K... fields);

    /**
     * Get the time to live for one or more fields as UNIX timestamp in milliseconds.
     *
     * @param key the key.
     * @param fields one or more fields to get the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: expiration time as a UNIX timestamp in
     *         milliseconds; {@code -1} indicating the field has no expiry time set; {@code -2} indicating there is no such
     *         field
     * @since 6.4
     */
    List<Long> hpexpiretime(K key, K... fields);

    /**
     * Get the time to live for one or more fields.
     *
     * @param key the key.
     * @param fields one or more fields to get the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: the time to live in seconds; or a negative value
     *         in order to signal an error. The command returns {@code -1} if the key exists but has no associated expiration
     *         time. The command returns {@code -2} if the key does not exist.
     * @since 6.4
     */
    List<Long> httl(K key, K... fields);

    /**
     * Get the time to live for one or more fields in milliseconds.
     *
     * @param key the key.
     * @param fields one or more fields to get the TTL for.
     * @return a list of {@link Long} values for each of the fields provided: the time to live in milliseconds; or a negative
     *         value in order to signal an error. The command returns {@code -1} if the key exists but has no associated
     *         expiration time. The command returns {@code -2} if the key does not exist.
     * @since 6.4
     */
    List<Long> hpttl(K key, K... fields);

}
