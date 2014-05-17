package com.lambdaworks.redis;

import java.util.Set;

import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * Synchronous executed commands for Sets.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:16
 */
public interface RedisSetsConnection<K, V> extends BaseRedisConnection<K, V> {
    Long sadd(K key, V... members);

    Long scard(K key);

    Set<V> sdiff(K... keys);

    Long sdiff(ValueStreamingChannel<V> channel, K... keys);

    Long sdiffstore(K destination, K... keys);

    Set<V> sinter(K... keys);

    Long sinter(ValueStreamingChannel<V> channel, K... keys);

    Long sinterstore(K destination, K... keys);

    Boolean sismember(K key, V member);

    Boolean smove(K source, K destination, V member);

    Set<V> smembers(K key);

    Long smembers(ValueStreamingChannel<V> channel, K key);

    V spop(K key);

    V srandmember(K key);

    Set<V> srandmember(K key, long count);

    Long srandmember(ValueStreamingChannel<V> channel, K key, long count);

    Long srem(K key, V... members);

    Set<V> sunion(K... keys);

    Long sunion(ValueStreamingChannel<V> channel, K... keys);

    Long sunionstore(K destination, K... keys);
}
