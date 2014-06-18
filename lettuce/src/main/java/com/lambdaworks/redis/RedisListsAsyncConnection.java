package com.lambdaworks.redis;

import java.util.List;

import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * Asynchronous executed commands for Lists.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
 */
public interface RedisListsAsyncConnection<K, V> {
    /**
     * Remove and get the first element in a list, or block until one is available.
     * 
     * @param timeout the key
     * @param keys the timeout type: long
     * @return RedisFuture<KeyValue<K,V>> array-reply specifically:
     * 
     *         A `nil` multi-bulk when no element could be popped and the timeout expired. A two-element multi-bulk with the
     *         first element being the name of the key where an element was popped and the second element being the value of the
     *         popped element.
     */
    RedisFuture<KeyValue<K, V>> blpop(long timeout, K... keys);

    /**
     * Remove and get the last element in a list, or block until one is available.
     * 
     * @param timeout the key
     * @param keys the timeout type: long
     * @return RedisFuture<KeyValue<K,V>> array-reply specifically:
     * 
     *         A `nil` multi-bulk when no element could be popped and the timeout expired. A two-element multi-bulk with the
     *         first element being the name of the key where an element was popped and the second element being the value of the
     *         popped element.
     */
    RedisFuture<KeyValue<K, V>> brpop(long timeout, K... keys);

    /**
     * Pop a value from a list, push it to another list and return it; or block until one is available.
     * 
     * @param timeout the source type: key
     * @param source the destination type: key
     * @param destination the timeout type: long
     * @return RedisFuture<V> bulk-string-reply the element being popped from `source` and pushed to `destination`. If `timeout`
     *         is reached, a
     */
    RedisFuture<V> brpoplpush(long timeout, K source, K destination);

    /**
     * Get an element from a list by its index.
     * 
     * @param key the key
     * @param index the index type: long
     * @return RedisFuture<V> bulk-string-reply the requested element, or `nil` when `index` is out of range.
     */
    RedisFuture<V> lindex(K key, long index);

    /**
     * Insert an element before or after another element in a list.
     * 
     * @param key the key
     * @param before the where type: enum
     * @param pivot the pivot type: value
     * @param value the value
     * @return RedisFuture<Long> integer-reply the length of the list after the insert operation, or `-1` when the value `pivot`
     *         was not found.
     */
    RedisFuture<Long> linsert(K key, boolean before, V pivot, V value);

    /**
     * Get the length of a list.
     * 
     * @param key the key
     * @return RedisFuture<Long> integer-reply the length of the list at `key`.
     */
    RedisFuture<Long> llen(K key);

    /**
     * Remove and get the first element in a list.
     * 
     * @param key the key
     * @return RedisFuture<V> bulk-string-reply the value of the first element, or `nil` when `key` does not exist.
     */
   RedisFuture<V> lpop(K key);

    /**
     * Prepend one or multiple values to a list.
     * 
     * @param key the key
     * @param values the value
     * @return RedisFuture<Long> integer-reply the length of the list after the push operations.
     */
    RedisFuture<Long> lpush(K key, V... values);

    /**
     * Prepend a value to a list, only if the list exists.
     * 
     * @param key the key
     * @param value the value
     * @return RedisFuture<Long> integer-reply the length of the list after the push operation.
     */
    RedisFuture<Long> lpushx(K key, V value);

    /**
     * Get a range of elements from a list.
     * 
     * @param key the key
     * @param start the start type: long
     * @param stop the stop type: long
     * @return RedisFuture<List<V>> array-reply list of elements in the specified range.
     */
    RedisFuture<List<V>> lrange(K key, long start, long stop);

    /**
     * Get a range of elements from a list.
     * 
     * @return RedisFuture<Long> array-reply list of elements in the specified range.
     */
    RedisFuture<Long> lrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    /**
     * Remove elements from a list.
     * 
     * @param key the key
     * @param count the count type: long
     * @param value the value
     * @return RedisFuture<Long> integer-reply the number of removed elements.
     */
    RedisFuture<Long> lrem(K key, long count, V value);

    /**
     * Set the value of an element in a list by its index.
     * 
     * @param key the key
     * @param index the index type: long
     * @param value the value
     * @return RedisFuture<String> simple-string-reply
     */
    RedisFuture<String> lset(K key, long index, V value);

    /**
     * Trim a list to the specified range.
     * 
     * @param key the key
     * @param start the start type: long
     * @param stop the stop type: long
     * @return RedisFuture<String> simple-string-reply
     */
    RedisFuture<String> ltrim(K key, long start, long stop);

    /**
     * Remove and get the last element in a list.
     * 
     * @param key the key
     * @return RedisFuture<V> bulk-string-reply the value of the last element, or `nil` when `key` does not exist.
     */
    RedisFuture<V> rpop(K key);

    /**
     * Remove the last element in a list, append it to another list and return it.
     * 
     * @param source the source type: key
     * @param destination the destination type: key
     * @return RedisFuture<V> bulk-string-reply the element being popped and pushed.
     */
    RedisFuture<V> rpoplpush(K source, K destination);

    /**
     * Append one or multiple values to a list.
     * 
     * @param key the key
     * @param values the value
     * @return RedisFuture<Long> integer-reply the length of the list after the push operation.
     */
    RedisFuture<Long> rpush(K key, V... values);

    /**
     * Append a value to a list, only if the list exists.
     * 
     * @param key the key
     * @param value the value
     * @return RedisFuture<Long> integer-reply the length of the list after the push operation.
     */
    RedisFuture<Long> rpushx(K key, V value);
}
