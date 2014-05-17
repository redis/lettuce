package com.lambdaworks.redis;

import java.util.Date;
import java.util.List;

import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * Asynchronous executed commands for Keys (Key manipulation/querying).
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:10
 */
public interface RedisKeysAsyncConnection<K, V> extends BaseRedisAsyncConnection<K, V> {
    RedisFuture<Long> del(K... keys);

    RedisFuture<byte[]> dump(K key);

    RedisFuture<Boolean> exists(K key);

    RedisFuture<Boolean> expire(K key, long seconds);

    RedisFuture<Boolean> expireat(K key, Date timestamp);

    RedisFuture<Boolean> expireat(K key, long timestamp);

    RedisFuture<List<K>> keys(K pattern);

    RedisFuture<Long> keys(KeyStreamingChannel<K> channel, K pattern);

    RedisFuture<String> migrate(String host, int port, K key, int db, long timeout);

    RedisFuture<Boolean> move(K key, int db);

    RedisFuture<String> objectEncoding(K key);

    RedisFuture<Long> objectIdletime(K key);

    RedisFuture<Long> objectRefcount(K key);

    RedisFuture<Boolean> persist(K key);

    RedisFuture<Boolean> pexpire(K key, long milliseconds);

    RedisFuture<Boolean> pexpireat(K key, Date timestamp);

    RedisFuture<Boolean> pexpireat(K key, long timestamp);

    RedisFuture<Long> pttl(K key);

    RedisFuture<V> randomkey();

    RedisFuture<String> rename(K key, K newKey);

    RedisFuture<Boolean> renamenx(K key, K newKey);

    RedisFuture<String> restore(K key, long ttl, byte[] value);

    RedisFuture<List<V>> sort(K key);

    RedisFuture<Long> sort(ValueStreamingChannel<V> channel, K key);

    RedisFuture<List<V>> sort(K key, SortArgs sortArgs);

    RedisFuture<Long> sort(ValueStreamingChannel<V> channel, K key, SortArgs sortArgs);

    RedisFuture<Long> sortStore(K key, SortArgs sortArgs, K destination);

    RedisFuture<Long> ttl(K key);

    RedisFuture<String> type(K key);
}
