package io.lettuce.core.api;

/**
 * ${intent} for HyperLogLog (PF* commands).
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
public interface RedisHLLCommands<K, V> {

    /**
     * Adds the specified elements to the specified HyperLogLog.
     *
     * @param key the key.
     * @param values the values.
     * @return Long integer-reply specifically:
     *
     *         1 if at least 1 HyperLogLog internal register was altered. 0 otherwise.
     */
    Long pfadd(K key, V... values);

    /**
     * Merge N different HyperLogLogs into a single one.
     *
     * @param destkey the destination key.
     * @param sourcekeys the source key.
     * @return String simple-string-reply The command just returns {@code OK}.
     */
    String pfmerge(K destkey, K... sourcekeys);

    /**
     * Return the approximated cardinality of the set(s) observed by the HyperLogLog at key(s).
     *
     * @param keys the keys.
     * @return Long integer-reply specifically:
     *
     *         The approximated number of unique elements observed via {@code PFADD}.
     */
    Long pfcount(K... keys);

}
