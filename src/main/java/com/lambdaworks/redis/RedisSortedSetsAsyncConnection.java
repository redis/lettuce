package com.lambdaworks.redis;

import java.util.List;

import com.lambdaworks.redis.output.ScoredValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * Asynchronous executed commands for Sorted Sets.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:18
 */
public interface RedisSortedSetsAsyncConnection<K, V> {
    RedisFuture<Long> zadd(K key, double score, V member);

    /**
     * 
     * @param key
     * @param scoresAndValues tuples with (double) score, (V) value
     * @return
     */
    @SuppressWarnings("unchecked")
    RedisFuture<Long> zadd(K key, Object... scoresAndValues);

    RedisFuture<Long> zcard(K key);

    RedisFuture<Long> zcount(K key, double min, double max);

    RedisFuture<Long> zcount(K key, String min, String max);

    RedisFuture<Double> zincrby(K key, double amount, K member);

    RedisFuture<Long> zinterstore(K destination, K... keys);

    RedisFuture<Long> zinterstore(K destination, ZStoreArgs storeArgs, K... keys);

    RedisFuture<List<V>> zrange(K key, long start, long stop);

    RedisFuture<List<ScoredValue<V>>> zrangeWithScores(K key, long start, long stop);

    RedisFuture<List<V>> zrangebyscore(K key, double min, double max);

    RedisFuture<List<V>> zrangebyscore(K key, String min, String max);

    RedisFuture<List<V>> zrangebyscore(K key, double min, double max, long offset, long count);

    RedisFuture<List<V>> zrangebyscore(K key, String min, String max, long offset, long count);

    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max);

    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max);

    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count);

    RedisFuture<List<ScoredValue<V>>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count);

    RedisFuture<Long> zrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    RedisFuture<Long> zrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop);

    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max);

    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max);

    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max, long offset, long count);

    RedisFuture<Long> zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max, long offset, long count);

    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max);

    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max);

    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max,
            long offset, long count);

    RedisFuture<Long> zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max,
            long offset, long count);

    RedisFuture<Long> zrank(K key, V member);

    RedisFuture<Long> zrem(K key, V... members);

    RedisFuture<Long> zremrangebyrank(K key, long start, long stop);

    RedisFuture<Long> zremrangebyscore(K key, double min, double max);

    RedisFuture<Long> zremrangebyscore(K key, String min, String max);

    RedisFuture<List<V>> zrevrange(K key, long start, long stop);

    RedisFuture<List<ScoredValue<V>>> zrevrangeWithScores(K key, long start, long stop);

    RedisFuture<List<V>> zrevrangebyscore(K key, double max, double min);

    RedisFuture<List<V>> zrevrangebyscore(K key, String max, String min);

    RedisFuture<List<V>> zrevrangebyscore(K key, double max, double min, long offset, long count);

    RedisFuture<List<V>> zrevrangebyscore(K key, String max, String min, long offset, long count);

    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min);

    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min);

    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count);

    RedisFuture<List<ScoredValue<V>>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count);

    RedisFuture<Long> zrevrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    RedisFuture<Long> zrevrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop);

    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min);

    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min);

    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min, long offset, long count);

    RedisFuture<Long> zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min, long offset, long count);

    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min);

    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min);

    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min,
            long offset, long count);

    RedisFuture<Long> zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min,
            long offset, long count);

    RedisFuture<Long> zrevrank(K key, V member);

    RedisFuture<Double> zscore(K key, V member);

    RedisFuture<Long> zunionstore(K destination, K... keys);

    RedisFuture<Long> zunionstore(K destination, ZStoreArgs storeArgs, K... keys);

    RedisFuture<ScoredValueScanCursor<V>> zscan(K key);

    RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanArgs scanArgs);

    RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs);

    RedisFuture<ScoredValueScanCursor<V>> zscan(K key, ScanCursor scanCursor);

    RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key);

    RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs);

    RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs);

    RedisFuture<StreamScanCursor> zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor);
}
