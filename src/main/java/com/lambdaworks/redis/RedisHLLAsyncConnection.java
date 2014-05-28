package com.lambdaworks.redis;

/**
 * Asynchronous executed commands for HyperLogLog (PF* commands).
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
 */
public interface RedisHLLAsyncConnection<K, V> {
    RedisFuture<Long> pfadd(K key, V value, V... moreValues);

    RedisFuture<Long> pfmerge(K destkey, K sourcekey, K... moreSourceKeys);

    RedisFuture<Long> pfcount(K key, K... moreKeys);

}
