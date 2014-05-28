package com.lambdaworks.redis;

/**
 * Synchronous executed commands for HyperLogLog (PF* commands).
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
 */
public interface RedisHLLConnection<K, V> {
    Long pfadd(K key, V value, V... moreValues);

    Long pfmerge(K destkey, K sourcekey, K... moreSourceKeys);

    Long pfcount(K key, K... moreKeys);

}
