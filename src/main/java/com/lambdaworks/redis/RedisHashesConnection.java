package com.lambdaworks.redis;

import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.output.KeyStreamingChannel;
import com.lambdaworks.redis.output.KeyValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * Synchronous executed commands for Hashes (Key-Value pairs).
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:07
 */
public interface RedisHashesConnection<K, V> extends BaseRedisConnection<K, V> {
    String configResetstat();

    Long hdel(K key, K... fields);

    Boolean hexists(K key, K field);

    V hget(K key, K field);

    Long hincrby(K key, K field, long amount);

    Double hincrbyfloat(K key, K field, double amount);

    Map<K, V> hgetall(K key);

    Long hgetall(KeyValueStreamingChannel<K, V> channel, K key);

    List<K> hkeys(K key);

    Long hkeys(KeyStreamingChannel<K> channel, K key);

    Long hlen(K key);

    List<V> hmget(K key, K... fields);

    Long hmget(ValueStreamingChannel<V> channel, K key, K... fields);

    String hmset(K key, Map<K, V> map);

    Boolean hset(K key, K field, V value);

    Boolean hsetnx(K key, K field, V value);

    List<V> hvals(K key);

    Long hvals(ValueStreamingChannel<V> channel, K key);
}
