package com.lambdaworks.redis;

import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.KeyValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * Asynchronous executed commands for Hashes (Key-Value pairs).
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:07
 */
public interface RedisHashesAsyncConnection<K, V> {

    /**
     * Delete one or more hash fields.
     * 
     * @param key the key
     * @param fields the field type: key
     * @return RedisFuture<Long> integer-reply the number of fields that were removed from the hash, not including specified but
     *         non existing fields.
     */
    RedisFuture<Long> hdel(K key, K... fields);

    /**
     * Determine if a hash field exists.
     * 
     * @param key the key
     * @param field the field type: key
     * @return RedisFuture<Boolean> integer-reply specifically:
     * 
     *         `1` if the hash contains `field`. `0` if the hash does not contain `field`, or `key` does not exist.
     */
    RedisFuture<Boolean> hexists(K key, K field);

    /**
     * Get the value of a hash field.
     * 
     * @param key the key
     * @param field the field type: key
     * @return RedisFuture<V> bulk-string-reply the value associated with `field`, or `nil` when `field` is not present in the
     *         hash or `key` does not exist.
     */
    RedisFuture<V> hget(K key, K field);

    /**
     * Increment the integer value of a hash field by the given number.
     * 
     * @param key the key
     * @param field the field type: key
     * @param amount the increment type: long
     * @return RedisFuture<Long> integer-reply the value at `field` after the increment operation.
     */
    RedisFuture<Long> hincrby(K key, K field, long amount);

    /**
     * Increment the float value of a hash field by the given amount.
     * 
     * @param key the key
     * @param field the field type: key
     * @param amount the increment type: double
     * @return RedisFuture<Double> bulk-string-reply the value of `field` after the increment.
     */
    RedisFuture<Double> hincrbyfloat(K key, K field, double amount);

    /**
     * Get all the fields and values in a hash.
     * 
     * @param key the key
     * @return RedisFuture<Map<K,V>> array-reply list of fields and their values stored in the hash, or an empty list when `key`
     *         does not exist.
     */
    RedisFuture<Map<K, V>> hgetall(K key);

    /**
     * Get all the fields and values in a hash.
     * 
     * @return RedisFuture<Long> array-reply list of fields and their values stored in the hash, or an empty list when `key`
     *         does not exist.
     */
    RedisFuture<Long> hgetall(KeyValueStreamingChannel<K, V> channel, K key);

    /**
     * Get all the fields in a hash.
     * 
     * @param key the key
     * @return RedisFuture<List<K>> array-reply list of fields in the hash, or an empty list when `key` does not exist.
     */
    RedisFuture<List<K>> hkeys(K key);

    /**
     * Get all the fields in a hash.
     * 
     * @return RedisFuture<Long> array-reply list of fields in the hash, or an empty list when `key` does not exist.
     */
    RedisFuture<Long> hkeys(KeyStreamingChannel<K> channel, K key);

    /**
     * Get the number of fields in a hash.
     * 
     * @param key the key
     * @return RedisFuture<Long> integer-reply number of fields in the hash, or `0` when `key` does not exist.
     */
    RedisFuture<Long> hlen(K key);

    /**
     * Get the values of all the given hash fields.
     * 
     * @param key the key
     * @param fields the field type: key
     * @return RedisFuture<List<V>> array-reply list of values associated with the given fields, in the same
     */
    RedisFuture<List<V>> hmget(K key, K... fields);

    /**
     * Get the values of all the given hash fields.
     * 
     * @return RedisFuture<Long> array-reply list of values associated with the given fields, in the same
     */
    RedisFuture<Long> hmget(ValueStreamingChannel<V> channel, K key, K... fields);

    /**
     * Set multiple hash fields to multiple values.
     * 
     * @param key the key
     * @param map the null
     * @return RedisFuture<String> simple-string-reply
     */
    RedisFuture<String> hmset(K key, Map<K, V> map);

    /**
     * Set the string value of a hash field.
     * 
     * @param key the key
     * @param field the field type: key
     * @param value the value type: value
     * @return RedisFuture<Boolean> integer-reply specifically:
     * 
     *         `1` if `field` is a new field in the hash and `value` was set. `0` if `field` already exists in the hash and the
     *         value was updated.
     */
    RedisFuture<Boolean> hset(K key, K field, V value);

    /**
     * Set the value of a hash field, only if the field does not exist.
     * 
     * @param key the key
     * @param field the field type: key
     * @param value the value type: value
     * @return RedisFuture<Boolean> integer-reply specifically:
     * 
     *         `1` if `field` is a new field in the hash and `value` was set. `0` if `field` already exists in the hash and no
     *         operation was performed.
     */
    RedisFuture<Boolean> hsetnx(K key, K field, V value);

    /**
     * Get all the values in a hash.
     * 
     * @param key the key
     * @return RedisFuture<List<V>> array-reply list of values in the hash, or an empty list when `key` does not exist.
     */
    RedisFuture<List<V>> hvals(K key);

    /**
     * Get all the values in a hash.
     * 
     * @return RedisFuture<Long> array-reply list of values in the hash, or an empty list when `key` does not exist.
     */
    RedisFuture<Long> hvals(ValueStreamingChannel<V> channel, K key);

    /**
     * Incrementally iterate hash fields and associated values.
     */
    RedisFuture<MapScanCursor<K, V>> hscan(K key);

    /**
     * Incrementally iterate hash fields and associated values.
     */
    RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields and associated values.
     */
    RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields and associated values.
     */
    RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor);

    /**
     * Incrementally iterate hash fields and associated values.
     */
    RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key);

    /**
     * Incrementally iterate hash fields and associated values.
     */
    RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields and associated values.
     * 
     * @param channel the key
     * @param key the cursor type: long
     * @param scanCursor the pattern type: pattern
     * @param scanArgs the count type: long
     */
    RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate hash fields and associated values.
     */
    RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor);
}
