package com.lambdaworks.redis;

/**
 * Asynchronous executed commands for HyperLogLog (PF* commands).
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
 */
public interface RedisHLLAsyncConnection<K, V> {
    /**
     * Adds the specified elements to the specified HyperLogLog.
     * 
     * @return RedisFuture<Long> integer-reply specifically:
     * 
     *         1 if at least 1 HyperLogLog internal register was altered. 0 otherwise.
     */
    RedisFuture<Long> pfadd(K key, V value, V... moreValues);

    /**
     * Merge N different HyperLogLogs into a single one.
     * 
     * @return RedisFuture<Long> simple-string-reply The command just returns `OK`.
     */
    RedisFuture<Long> pfmerge(K destkey, K sourcekey, K... moreSourceKeys);

    /**
     * Return the approximated cardinality of the set(s) observed by the HyperLogLog at key(s).
     * 
     * @return RedisFuture<Long> integer-reply specifically:
     * 
     *         The approximated number of unique elements observed via `PFADD`.
     */
    RedisFuture<Long> pfcount(K key, K... moreKeys);

}
