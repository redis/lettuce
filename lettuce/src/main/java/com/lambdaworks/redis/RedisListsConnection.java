package com.lambdaworks.redis;

import java.util.List;

import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * 
 * Synchronous executed commands for Lists.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
 */
public interface RedisListsConnection<K, V> {
    KeyValue<K, V> blpop(long timeout, K... keys);

    KeyValue<K, V> brpop(long timeout, K... keys);

    V brpoplpush(long timeout, K source, K destination);

    V lindex(K key, long index);

    Long linsert(K key, boolean before, V pivot, V value);

    Long llen(K key);

    V lpop(K key);

    Long lpush(K key, V... values);

    Long lpushx(K key, V value);

    List<V> lrange(K key, long start, long stop);

    Long lrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    Long lrem(K key, long count, V value);

    String lset(K key, long index, V value);

    String ltrim(K key, long start, long stop);

    V rpop(K key);

    V rpoplpush(K source, K destination);

    Long rpush(K key, V... values);

    Long rpushx(K key, V value);
}
