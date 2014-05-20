package com.lambdaworks.redis;

import java.util.List;

import com.lambdaworks.redis.output.ScoredValueStreamingChannel;
import com.lambdaworks.redis.output.ValueStreamingChannel;

/**
 * Synchronous executed commands for Sorted Sets.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:18
 */
public interface RedisSortedSetsConnection<K, V> extends BaseRedisConnection<K, V> {
    Long zadd(K key, double score, V member);

    /**
     * 
     * @param key
     * @param scoresAndValues tuples with (double) score, (V) value
     * @return
     */
    @SuppressWarnings("unchecked")
    Long zadd(K key, Object... scoresAndValues);

    Long zcard(K key);

    Long zcount(K key, double min, double max);

    Long zcount(K key, String min, String max);

    Double zincrby(K key, double amount, K member);

    Long zinterstore(K destination, K... keys);

    Long zinterstore(K destination, ZStoreArgs storeArgs, K... keys);

    List<V> zrange(K key, long start, long stop);

    List<ScoredValue<V>> zrangeWithScores(K key, long start, long stop);

    List<V> zrangebyscore(K key, double min, double max);

    List<V> zrangebyscore(K key, String min, String max);

    List<V> zrangebyscore(K key, double min, double max, long offset, long count);

    List<V> zrangebyscore(K key, String min, String max, long offset, long count);

    List<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max);

    List<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max);

    List<ScoredValue<V>> zrangebyscoreWithScores(K key, double min, double max, long offset, long count);

    List<ScoredValue<V>> zrangebyscoreWithScores(K key, String min, String max, long offset, long count);

    Long zrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    Long zrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop);

    Long zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max);

    Long zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max);

    Long zrangebyscore(ValueStreamingChannel<V> channel, K key, double min, double max, long offset, long count);

    Long zrangebyscore(ValueStreamingChannel<V> channel, K key, String min, String max, long offset, long count);

    Long zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max);

    Long zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max);

    Long zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double min, double max, long offset, long count);

    Long zrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String min, String max, long offset, long count);

    Long zrank(K key, V member);

    Long zrem(K key, V... members);

    Long zremrangebyrank(K key, long start, long stop);

    Long zremrangebyscore(K key, double min, double max);

    Long zremrangebyscore(K key, String min, String max);

    List<V> zrevrange(K key, long start, long stop);

    List<ScoredValue<V>> zrevrangeWithScores(K key, long start, long stop);

    List<V> zrevrangebyscore(K key, double max, double min);

    List<V> zrevrangebyscore(K key, String max, String min);

    List<V> zrevrangebyscore(K key, double max, double min, long offset, long count);

    List<V> zrevrangebyscore(K key, String max, String min, long offset, long count);

    List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min);

    List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min);

    List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, double max, double min, long offset, long count);

    List<ScoredValue<V>> zrevrangebyscoreWithScores(K key, String max, String min, long offset, long count);

    Long zrevrange(ValueStreamingChannel<V> channel, K key, long start, long stop);

    Long zrevrangeWithScores(ScoredValueStreamingChannel<V> channel, K key, long start, long stop);

    Long zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min);

    Long zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min);

    Long zrevrangebyscore(ValueStreamingChannel<V> channel, K key, double max, double min, long offset, long count);

    Long zrevrangebyscore(ValueStreamingChannel<V> channel, K key, String max, String min, long offset, long count);

    Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min);

    Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min);

    Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, double max, double min, long offset,
            long count);

    Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<V> channel, K key, String max, String min, long offset,
            long count);

    Long zrevrank(K key, V member);

    Double zscore(K key, V member);

    Long zunionstore(K destination, K... keys);

    Long zunionstore(K destination, ZStoreArgs storeArgs, K... keys);

    ScoredValueScanCursor<V> zscan(K key);

    ScoredValueScanCursor<V> zscan(K key, ScanArgs scanArgs);

    ScoredValueScanCursor<V> zscan(K key, ScanCursor scanCursor, ScanArgs scanArgs);

    ScoredValueScanCursor<V> zscan(K key, ScanCursor scanCursor);

    StreamScanCursor zscan(ScoredValueStreamingChannel<V> channel, K key);

    StreamScanCursor zscan(ScoredValueStreamingChannel<V> channel, K key, ScanArgs scanArgs);

    StreamScanCursor zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor, ScanArgs scanArgs);

    StreamScanCursor zscan(ScoredValueStreamingChannel<V> channel, K key, ScanCursor scanCursor);
}
