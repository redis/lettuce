package com.lambdaworks.redis;

import com.lambdaworks.redis.output.ValueStreamingChannel;

import java.util.List;

/**
 * Asynchronous executed commands for Lists.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
 */
public interface RedisListsAsyncConnection<K, V> extends BaseRedisAsyncConnection<K, V> {
    RedisFuture<KeyValue<K, V>> blpop(long timeout, K... keys);

    RedisFuture<KeyValue<K, V>> brpop(long timeout, K... keys);

    RedisFuture<V> brpoplpush(long timeout, K source, K destination);

    RedisFuture<V> lindex(K key, long index);

    RedisFuture<Long> linsert(K key, boolean before, V pivot, V value);

    RedisFuture<Long> llen(K key);

    RedisFuture<V> lpop(K key);

    RedisFuture<Long> lpush(K key, V... values);

    RedisFuture<Long> lpushx(K key, V value);

    RedisFuture<List<V>> lrange(K key, long start, long stop);

    RedisFuture<Long> lrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    RedisFuture<Long> lrem(K key, long count, V value);

    RedisFuture<String> lset(K key, long index, V value);

    RedisFuture<String> ltrim(K key, long start, long stop);

    RedisFuture<V> rpop(K key);

    RedisFuture<V> rpoplpush(K source, K destination);

    RedisFuture<Long> rpush(K key, V... values);

    RedisFuture<Long> rpushx(K key, V value);
}
