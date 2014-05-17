package com.lambdaworks.redis;

import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.output.ValueStreamingChannel;
import com.lambdaworks.redis.protocol.SetArgs;

/**
 * 
 * Asynchronous executed commands for Strings.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:12
 */
public interface RedisStringsAsyncConnection<K, V> extends BaseRedisAsyncConnection<K, V> {
    RedisFuture<Long> append(K key, V value);

    RedisFuture<Long> bitcount(K key);

    RedisFuture<Long> bitcount(K key, long start, long end);

    RedisFuture<Long> bitopAnd(K destination, K... keys);

    RedisFuture<Long> bitopNot(K destination, K source);

    RedisFuture<Long> bitopOr(K destination, K... keys);

    RedisFuture<Long> bitopXor(K destination, K... keys);

    RedisFuture<Long> decr(K key);

    RedisFuture<Long> decrby(K key, long amount);

    RedisFuture<V> get(K key);

    RedisFuture<Long> getbit(K key, long offset);

    RedisFuture<V> getrange(K key, long start, long end);

    RedisFuture<V> getset(K key, V value);

    RedisFuture<Long> incr(K key);

    RedisFuture<Long> incrby(K key, long amount);

    RedisFuture<Double> incrbyfloat(K key, double amount);

    RedisFuture<List<V>> mget(K... keys);

    RedisFuture<Long> mget(ValueStreamingChannel<V> channel, K... keys);

    RedisFuture<String> mset(Map<K, V> map);

    RedisFuture<Boolean> msetnx(Map<K, V> map);

    RedisFuture<String> set(K key, V value);

    RedisFuture<V> set(K key, V value, SetArgs setArgs);

    RedisFuture<Long> setbit(K key, long offset, int value);

    RedisFuture<String> setex(K key, long seconds, V value);

    RedisFuture<Boolean> setnx(K key, V value);

    RedisFuture<Long> setrange(K key, long offset, V value);

    RedisFuture<Long> strlen(K key);
}
