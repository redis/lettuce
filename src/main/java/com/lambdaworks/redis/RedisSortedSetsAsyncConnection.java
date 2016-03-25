package com.lambdaworks.redis;

import java.util.List;

import com.lambdaworks.redis.api.async.RedisSortedSetAsyncCommands;
import com.lambdaworks.redis.output.ScoredValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * Asynchronous executed commands for Sorted Sets.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 * @deprecated Use {@literal RedisSortedSetAsyncCommands}
 */
@Deprecated
public interface RedisSortedSetsAsyncConnection<K, V> {
    /**
     * Add one or more members to a sorted set, or update its score if it already exists.
     * 
     * @param key the key
     * @param score the score
     * @param member the member
     * 
     * @return RedisFuture&lt;Long&gt; integer-reply specifically:
     * 
     *         The number of elements added to the sorted sets, not including elements already existing for which the score was
     *         updated.
     */
    RedisFuture<Long> zadd(K key, double score, V member);

    /**
     * Add one or more members to a sorted set, or update its score if it already exists.
     * 
     * @param key the key
     * @param scoresAndValues the scoresAndValue tuples (score,value,score,value,...)
     * @return RedisFuture&lt;Long&gt; integer-reply specifically:
     * 
     *         The number of elements added to the sorted sets, not including elements already existing for which the score was
     *         updated.
     */
    RedisFuture<Long> zadd(K key, Object... scoresAndValues);

    /**
     * Add one or more members to a sorted set, or update its score if it already exists.
     *
     * @param key the key
     * @param zAddArgs arguments for zadd
     * @param score the score
     * @param member the member
     *
     * @return RedisFuture&lt;Long&gt; integer-reply specifically:
     *
     *         The number of elements added to the sorted sets, not including elements already existing for which the score was
     *         updated.
     */
    RedisFuture<Long> zadd(K key, ZAddArgs zAddArgs, double score, V member);

    /**
     * Add one or more members to a sorted set, or update its score if it already exists.
     *
     * @param key the key
     * @param zAddArgs arguments for zadd
     * @param scoresAndValues the scoresAndValue tuples (score,value,score,value,...)
     * @return RedisFuture&lt;Long&gt; integer-reply specifically:
     *
     *         The number of elements added to the sorted sets, not including elements already existing for which the score was
     *         updated.
     */
    RedisFuture<Long> zadd(K key, ZAddArgs zAddArgs, Object... scoresAndValues);

    /**
     * ZADD acts like ZINCRBY
     *
     * @param key the key
     * @param score the score
     * @param member the member
     *
     * @return RedisFuture&lt;Long&gt; integer-reply specifically:
     *
     *         The total number of elements changed
     */
    RedisFuture<Double> zaddincr(K key, double score, V member);

    /**
     * Get the number of members in a sorted set.
     * 
     * @param key the key
     * @return RedisFuture&lt;Long&gt; integer-reply the cardinality (number of elements) of the sorted set, or {@literal false}
     *         if {@code key} does not exist.
     */
    RedisFuture<Long> zcard(K key);

    /**
     * Count the members in a sorted set with scores within the given values.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements in the specified score range.
     */
    RedisFuture<Long> zcount(K key, double min, double max);

    /**
     * Count the members in a sorted set with scores within the given values.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements in the specified score range.
     */
    RedisFuture<Long> zcount(K key, String min, String max);

    /**
     * Increment the score of a member in a sorted set.
     * 
     * @param key the key
     * @param amount the increment type: long
     * @param member the member type: key
     * @return RedisFuture&lt;Double;&gt; bulk-string-reply the new score of {@code member} (a double precision floating point
     *         number), represented as string.
     */
    RedisFuture<Double> zincrby(K key, double amount, K member);

    /**
     * Intersect multiple sorted sets and store the resulting sorted set in a new key.
     * 
     * @param destination the destination
     * @param keys the keys
     * 
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements in the resulting sorted set at {@code destination}.
     */
    RedisFuture<Long> zinterstore(K destination, K... keys);

    /**
     * Intersect multiple sorted sets and store the resulting sorted set in a new key.
     * 
     * @param destination the destination
     * @param storeArgs the storeArgs
     * @param keys the keys
     * 
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements in the resulting sorted set at {@code destination}.
     */
    RedisFuture<Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys);

    /**
     * Return a range of members in a sorted set, by index.
     * 
     * @param key the key
     * @param start the start
     * @param stop the stop
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified range.
     */
    RedisFuture<List<V>> zrange(K key, long start, long stop);

    /**
     * Return a range of members in a sorted set, by index.
     * 
     * @param key the key
     * @param start the start
     * @param stop the stop
     * @return RedisFuture&lt;List&lt;ScoredValue&lt;V&gt;&gt;&gt; array-reply list of elements in the specified range.
     */
    RedisFuture<List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop);

    /**
     * Return a range of members in a sorted set, by score.
     *
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<V>> zrangebyscore(K key, double min, double max);

    /**
     * Return a range of members in a sorted set, by score.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<V>> zrangebyscore(K key, String min, String max);

    /**
     * Return a range of members in a sorted set, by score.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<V>> zrangebyscore(K key, double min, double max, long offset, long count);

    /**
     * Return a range of members in a sorted set, by score.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<V>> zrangebyscore(K key, String min, String max, long offset, long count);

    /**
     * Return a range of members with score in a sorted set, by score.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;List&lt;ScoredValue&lt;V&gt;&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max);

    /**
     * Return a range of members with score in a sorted set, by score.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;List&lt;ScoredValue&lt;V&gt;&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max);

    /**
     * Return a range of members with score in a sorted set, by score.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;List&lt;ScoredValue&lt;V&gt;&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count);

    /**
     * Return a range of members with score in a sorted set, by score.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;List&lt;ScoredValue&lt;V&gt;&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count);

    /**
     * Stream over a range of members in a sorted set, by index.
     * 
     * @param channel streaming channel that receives a call for every value
     * @param key the key
     * @param start the start
     * @param stop the stop
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    /**
     * Stream over a range of members with scores in a sorted set, by index.
     * 
     * @param channel streaming channel that receives a call for every value
     * @param key the key
     * @param start the start
     * @param stop the stop
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop);

    /**
     * Stream over a range of members in a sorted set, by score.
     * 
     * @param channel streaming channel that receives a call for every value
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; count of elements in the specified score range.
     */
    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max);

    /**
     * Stream over a range of members in a sorted set, by score.
     * 
     * @param channel streaming channel that receives a call for every value
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; count of elements in the specified score range.
     */
    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max);

    /**
     * Stream over a range of members in a sorted set, by score.
     * 
     * @param channel streaming channel that receives a call for every value
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;Long&gt; count of elements in the specified score range.
     */
    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max, long offset, long count);

    /**
     * Stream over a range of members in a sorted set, by score.
     * 
     * @param channel streaming channel that receives a call for every value
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;Long&gt; count of elements in the specified score range.
     */
    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max, long offset, long count);

    /**
     * Stream over a range of members with scores in a sorted set, by score.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; count of elements in the specified score range.
     */
    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max);

    /**
     * Stream over a range of members with scores in a sorted set, by score.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; count of elements in the specified score range.
     */
    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max);

    /**
     * Stream over a range of members with scores in a sorted set, by score.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;Long&gt; count of elements in the specified score range.
     */
    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max,
            long offset, long count);

    /**
     * Stream over a range of members with scores in a sorted set, by score.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;Long&gt; count of elements in the specified score range.
     */
    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max,
            long offset, long count);

    /**
     * Determine the index of a member in a sorted set.
     * 
     * @param key the key
     * @param member the member type: value
     * @return RedisFuture&lt;Long&gt; integer-reply the rank of {@code member}. If {@code member} does not exist in the sorted
     *         set or {@code key} does not exist,
     */
    RedisFuture<Long> zrank(K key, V member);

    /**
     * Remove one or more members from a sorted set.
     * 
     * @param key the key
     * @param members the member type: value
     * @return RedisFuture&lt;Long&gt; integer-reply specifically:
     * 
     *         The number of members removed from the sorted set, not including non existing members.
     */
    RedisFuture<Long> zrem(K key, V... members);

    /**
     * Remove all members in a sorted set within the given indexes.
     * 
     * @param key the key
     * @param start the start type: long
     * @param stop the stop type: long
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements removed.
     */
    RedisFuture<Long> zremrangebyrank(K key, long start, long stop);

    /**
     * Remove all members in a sorted set within the given scores.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements removed.
     */
    RedisFuture<Long> zremrangebyscore(K key, double min, double max);

    /**
     * Remove all members in a sorted set within the given scores.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements removed.
     */
    RedisFuture<Long> zremrangebyscore(K key, String min, String max);

    /**
     * Return a range of members in a sorted set, by index, with scores ordered from high to low.
     * 
     * @param key the key
     * @param start the start
     * @param stop the stop
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified range.
     */
    RedisFuture<List<V>> zrevrange(K key, long start, long stop);

    /**
     * Return a range of members with scores in a sorted set, by index, with scores ordered from high to low.
     * 
     * @param key the key
     * @param start the start
     * @param stop the stop
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified range.
     */
    RedisFuture<List<ScoredValue<V>>> zrevrangeWithScores(K key, long start, long stop);

    /**
     * Return a range of members in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<V>> zrevrangebyscore(K key, double max, double min);

    /**
     * Return a range of members in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<V>> zrevrangebyscore(K key, String max, String min);

    /**
     * Return a range of members in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<V>> zrevrangebyscore(K key, double max, double min, long offset, long count);

    /**
     * Return a range of members in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param key the key
     * @param max max score
     * @param min min score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<V>> zrevrangebyscore(K key, String max, String min, long offset, long count);

    /**
     * Return a range of members with scores in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param key the key
     * @param max max score
     * @param min min score
     * @return RedisFuture&lt;List&lt;ScoredValue&lt;V&gt;&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min);

    /**
     * Return a range of members with scores in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param key the key
     * @param max max score
     * @param min min score
     * @return RedisFuture&lt;List&lt;ScoredValue&lt;V&gt;&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min);

    /**
     * Return a range of members with scores in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param key the key
     * @param max max score
     * @param min min score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;List&lt;ScoredValue&lt;V&gt;&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count);

    /**
     * Return a range of members with scores in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param key the key
     * @param max max score
     * @param min min score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;List&lt;ScoredValue&lt;V&gt;&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count);

    /**
     * Stream over a range of members in a sorted set, by index, with scores ordered from high to low.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param start the start
     * @param stop the stop
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrevrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    /**
     * Stream over a range of members with scores in a sorted set, by index, with scores ordered from high to low.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param start the start
     * @param stop the stop
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrevrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop);

    /**
     * Stream over a range of members in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param channel streaming channel that receives a call for every value
     * @param key the key
     * @param max max score
     * @param min min score
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min);

    /**
     * Stream over a range of members in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param channel streaming channel that receives a call for every value
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min);

    /**
     * Stream over a range of members in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param channel streaming channel that receives a call for every value
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min, long offset, long count);

    /**
     * Stream over a range of members in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param channel streaming channel that receives a call for every value
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min, long offset, long count);

    /**
     * Stream over a range of members with scores in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min);

    /**
     * Stream over a range of members with scores in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min);

    /**
     * Stream over a range of members with scores in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min,
            long offset, long count);

    /**
     * Stream over a range of members with scores in a sorted set, by score, with scores ordered from high to low.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;Long&gt; count of elements in the specified range.
     */
    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min,
            long offset, long count);

    /**
     * Determine the index of a member in a sorted set, with scores ordered from high to low.
     * 
     * @param key the key
     * @param member the member type: value
     * @return RedisFuture&lt;Long&gt; integer-reply the rank of {@code member}. If {@code member} does not exist in the sorted
     *         set or {@code key} does not exist,
     */
    RedisFuture<Long> zrevrank(K key, V member);

    /**
     * Get the score associated with the given member in a sorted set.
     * 
     * @param key the key
     * @param member the member type: value
     * @return RedisFuture&lt;Double;&gt; bulk-string-reply the score of {@code member} (a double precision floating point
     *         number), represented as string.
     */
    RedisFuture<Double> zscore(K key, V member);

    /**
     * Add multiple sorted sets and store the resulting sorted set in a new key.
     *
     * @param destination destination key
     * @param keys source keys
     * 
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements in the resulting sorted set at {@code destination}.
     */
    RedisFuture<Long> zunionstore(K destination, K... keys);

    /**
     * Add multiple sorted sets and store the resulting sorted set in a new key.
     * 
     * @param destination the destination
     * @param storeArgs the storeArgs
     * @param keys the keys
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements in the resulting sorted set at {@code destination}.
     */
    RedisFuture<Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys);

    /**
     * Incrementally iterate sorted sets elements and associated scores.
     * 
     * @param key the key
     * @return RedisFuture&lt;ScoredValueScanCursor&lt;V&gt;&gt; scan cursor.
     */
    RedisFuture<ScoredValueScanCursor<V>> zscan(K key);

    /**
     * Incrementally iterate sorted sets elements and associated scores.
     * 
     * @param key the key
     * @param scanArgs scan arguments
     * @return RedisFuture&lt;ScoredValueScanCursor&lt;V&gt;&gt; scan cursor.
     */
    RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanArgs scanArgs);

    /**
     * Incrementally iterate sorted sets elements and associated scores.
     * 
     * @param key the key
     * @param scanCursor cursor to resume from a previous scan, must not be {@literal null}
     * @param scanArgs scan arguments
     * @return RedisFuture&lt;ScoredValueScanCursor&lt;V&gt;&gt; scan cursor.
     */
    RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate sorted sets elements and associated scores.
     * 
     * @param key the key
     * @param scanCursor cursor to resume from a previous scan, must not be {@literal null}
     * @return RedisFuture&lt;ScoredValueScanCursor&lt;V&gt;&gt; scan cursor.
     */
    RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor);

    /**
     * Incrementally iterate sorted sets elements and associated scores.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @return RedisFuture&lt;StreamScanCursor&gt; scan cursor.
     */
    RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key);

    /**
     * Incrementally iterate sorted sets elements and associated scores.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param scanArgs scan arguments
     * @return RedisFuture&lt;StreamScanCursor&gt; scan cursor.
     */
    RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs);

    /**
     * Incrementally iterate sorted sets elements and associated scores.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param scanCursor cursor to resume from a previous scan, must not be {@literal null}
     * @param scanArgs scan arguments
     * @return RedisFuture&lt;StreamScanCursor&gt; scan cursor.
     */
    RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate sorted sets elements and associated scores.
     * 
     * @param channel streaming channel that receives a call for every scored value
     * @param key the key
     * @param scanCursor cursor to resume from a previous scan, must not be {@literal null}
     * @return RedisFuture&lt;StreamScanCursor&gt; scan cursor.
     */
    RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor);

    /**
     * Count the number of members in a sorted set between a given lexicographical range.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements in the specified score range.
     */
    RedisFuture<Long> zlexcount(K key, String min, String max);

    /**
     * Remove all members in a sorted set between the given lexicographical range.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;Long&gt; integer-reply the number of elements removed.
     */
    RedisFuture<Long> zremrangebylex(K key, String min, String max);

    /**
     * Return a range of members in a sorted set, by lexicographical range.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<V>> zrangebylex(K key, String min, String max);

    /**
     * Return a range of members in a sorted set, by lexicographical range.
     * 
     * @param key the key
     * @param min min score
     * @param max max score
     * @param offset the offset
     * @param count the count
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of elements in the specified score range.
     */
    RedisFuture<List<V>> zrangebylex(K key, String min, String max, long offset, long count);
}
