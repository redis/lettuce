package com.lambdaworks.redis;

/**
 * Synchronous executed commands for HyperLogLog (PF* commands).
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
 */
public interface RedisHLLConnection<K, V> {
    /**
     * Adds the specified elements to the specified HyperLogLog.
     * 
     * @param key the key
     * @param value the value
     * @param moreValues more values
     * 
     * @return Long integer-reply specifically:
     * 
     *         1 if at least 1 HyperLogLog internal register was altered. 0 otherwise.
     */
    Long pfadd(K key, V value, V... moreValues);

    /**
     * Merge N different HyperLogLogs into a single one.
     * 
     * @param destkey the destination key
     * @param sourcekey the source key
     * @param moreSourceKeys more source keys
     * 
     * @return Long simple-string-reply The command just returns <code>OK</code>.
     */
    Long pfmerge(K destkey, K sourcekey, K... moreSourceKeys);

    /**
     * Return the approximated cardinality of the set(s) observed by the HyperLogLog at key(s).
     * 
     * @param key the key
     * @param moreKeys more keys
     * 
     * @return Long integer-reply specifically:
     * 
     *         The approximated number of unique elements observed via <code>PFADD</code>.
     */
    Long pfcount(K key, K... moreKeys);

}
