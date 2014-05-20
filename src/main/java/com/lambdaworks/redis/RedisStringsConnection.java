package com.lambdaworks.redis;

import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.output.ValueStreamingChannel;
import com.lambdaworks.redis.protocol.SetArgs;

/**
 * 
 * Synchronous executed commands for Strings.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:12
 */
public interface RedisStringsConnection<K, V> extends BaseRedisConnection<K, V> {
    Long append(K key, V value);

    Long bitcount(K key);

    Long bitcount(K key, long start, long end);

    Long bitpos(K key, boolean state);

    Long bitpos(K key, boolean state, long start, long end);

    Long bitopAnd(K destination, K... keys);

    Long bitopNot(K destination, K source);

    Long bitopOr(K destination, K... keys);

    Long bitopXor(K destination, K... keys);

    Long decr(K key);

    Long decrby(K key, long amount);

    V get(K key);

    Long getbit(K key, long offset);

    V getrange(K key, long start, long end);

    V getset(K key, V value);

    Long incr(K key);

    Long incrby(K key, long amount);

    Double incrbyfloat(K key, double amount);

    List<V> mget(K... keys);

    Long mget(ValueStreamingChannel<V> channel, K... keys);

    String mset(Map<K, V> map);

    Boolean msetnx(Map<K, V> map);

    String set(K key, V value);

    V set(K key, V value, SetArgs setArgs);

    Long setbit(K key, long offset, int value);

    String setex(K key, long seconds, V value);

    String psetex(K key, long milliseconds, V value);

    Boolean setnx(K key, V value);

    Long setrange(K key, long offset, V value);

    Long strlen(K key);
}
