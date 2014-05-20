package com.lambdaworks.redis;

import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.KeyValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

import java.util.List;
import java.util.Map;

/**
 * Asynchronous executed commands for Hashes (Key-Value pairs).
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:07
 */
public interface RedisHashesAsyncConnection<K, V> extends BaseRedisAsyncConnection<K, V> {

    RedisFuture<Long> hdel(K key, K... fields);

    RedisFuture<Boolean> hexists(K key, K field);

    RedisFuture<V> hget(K key, K field);

    RedisFuture<Long> hincrby(K key, K field, long amount);

    RedisFuture<Double> hincrbyfloat(K key, K field, double amount);

    RedisFuture<Map<K, V>> hgetall(K key);

    RedisFuture<Long> hgetall(KeyValueStreamingChannel<K, V> channel, K key);

    RedisFuture<List<K>> hkeys(K key);

    RedisFuture<Long> hkeys(KeyStreamingChannel<K> channel, K key);

    RedisFuture<Long> hlen(K key);

    RedisFuture<List<V>> hmget(K key, K... fields);

    RedisFuture<Long> hmget(ValueStreamingChannel<V> channel, K key, K... fields);

    RedisFuture<String> hmset(K key, Map<K, V> map);

    RedisFuture<Boolean> hset(K key, K field, V value);

    RedisFuture<Boolean> hsetnx(K key, K field, V value);

    RedisFuture<List<V>> hvals(K key);

    RedisFuture<Long> hvals(ValueStreamingChannel<V> channel, K key);

    RedisFuture<MapScanCursor<K, V>> hscan(K key);

    RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanArgs scanArgs);

    RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor, ScanArgs scanArgs);

    RedisFuture<MapScanCursor<K, V>> hscan(K key, ScanCursor scanCursor);

    RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key);

    RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanArgs scanArgs);

    RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs);

    RedisFuture<StreamScanCursor> hscan(KeyValueStreamingChannel<K, V> channel, K key, ScanCursor scanCursor);
}
