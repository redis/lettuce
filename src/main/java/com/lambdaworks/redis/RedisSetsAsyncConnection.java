package com.lambdaworks.redis;

import java.util.Set;

import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * Asynchronous executed commands for Sets.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:16
 */
public interface RedisSetsAsyncConnection<K, V> extends BaseRedisAsyncConnection<K, V> {
    RedisFuture<Long> sadd(K key, V... members);

    RedisFuture<Long> scard(K key);

    RedisFuture<Set<V>> sdiff(K... keys);

    RedisFuture<Long> sdiff(ValueStreamingChannel<V> channel, K... keys);

    RedisFuture<Long> sdiffstore(K destination, K... keys);

    RedisFuture<Set<V>> sinter(K... keys);

    RedisFuture<Long> sinter(ValueStreamingChannel<V> channel, K... keys);

    RedisFuture<Long> sinterstore(K destination, K... keys);

    RedisFuture<Boolean> sismember(K key, V member);

    RedisFuture<Boolean> smove(K source, K destination, V member);

    RedisFuture<Set<V>> smembers(K key);

    RedisFuture<Long> smembers(ValueStreamingChannel<V> channel, K key);

    RedisFuture<V> spop(K key);

    RedisFuture<V> srandmember(K key);

    RedisFuture<Set<V>> srandmember(K key, long count);

    RedisFuture<Long> srandmember(ValueStreamingChannel<V> channel, K key, long count);

    RedisFuture<Long> srem(K key, V... members);

    RedisFuture<Set<V>> sunion(K... keys);

    RedisFuture<Long> sunion(ValueStreamingChannel<V> channel, K... keys);

    RedisFuture<Long> sunionstore(K destination, K... keys);

    RedisFuture<ValueScanCursor<V>> sscan(K key);

    RedisFuture<ValueScanCursor<V>> sscan(K key, ScanArgs scanArgs);

    RedisFuture<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor, ScanArgs scanArgs);

    RedisFuture<ValueScanCursor<V>> sscan(K key, ScanCursor scanCursor);

    RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key);

    RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanArgs scanArgs);

    RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs);

    RedisFuture<StreamScanCursor> sscan(ValueStreamingChannel<V> channel, K key, ScanCursor scanCursor);
}
