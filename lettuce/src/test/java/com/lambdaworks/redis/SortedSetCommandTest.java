// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static com.lambdaworks.redis.ZStoreArgs.Builder.max;
import static com.lambdaworks.redis.ZStoreArgs.Builder.min;
import static com.lambdaworks.redis.ZStoreArgs.Builder.sum;
import static com.lambdaworks.redis.ZStoreArgs.Builder.weights;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SortedSetCommandTest extends AbstractCommandTest {
    @Test
    public void zadd() throws Exception {
        assertEquals(1, (long) redis.zadd(key, 1.0, "a"));
        assertEquals(0, (long) redis.zadd(key, 1.0, "a"));
        assertEquals(list("a"), redis.zrange(key, 0, -1));
        assertEquals(2, (long) redis.zadd(key, 2.0, "b", 3.0, "c"));
        assertEquals(list("a", "b", "c"), redis.zrange(key, 0, -1));
    }

    @Test
    public void zcard() throws Exception {
        assertEquals(0, (long) redis.zcard(key));
        redis.zadd(key, 1.0, "a");
        assertEquals(1, (long) redis.zcard(key));
    }

    @Test
    public void zcount() throws Exception {
        assertEquals(0, (long) redis.zcount(key, 0, 0));

        redis.zadd(key, 1.0, "a", 2.0, "b", 2.1, "c");

        assertEquals(3, (long) redis.zcount(key, 1.0, 3.0));
        assertEquals(2, (long) redis.zcount(key, 1.0, 2.0));
        assertEquals(3, (long) redis.zcount(key, NEGATIVE_INFINITY, POSITIVE_INFINITY));

        assertEquals(2, (long) redis.zcount(key, "(1.0", "3.0"));
        assertEquals(3, (long) redis.zcount(key, "-inf", "+inf"));
    }

    @Test
    public void zincrby() throws Exception {
        assertEquals(0.0, redis.zincrby(key, 0.0, "a"), 0.0);
        assertEquals(1.1, redis.zincrby(key, 1.1, "a"), 0.0);
        assertEquals(1.1, redis.zscore(key, "a"), 0.0);
        assertEquals(-0.1, redis.zincrby(key, -1.2, "a"), 0.0001);
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zinterstore() throws Exception {
        redis.zadd("zset1", 1.0, "a", 2.0, "b");
        redis.zadd("zset2", 2.0, "a", 3.0, "b", 4.0, "c");
        assertEquals(2, redis.zinterstore(key, "zset1", "zset2"), 0.0);
        assertEquals(list("a", "b"), redis.zrange(key, 0, -1));
        assertEquals(svlist(sv(3.0, "a"), sv(5.0, "b")), redis.zrangeWithScores(key, 0, -1));
    }

    @Test
    public void zrange() throws Exception {
        setup();
        assertEquals(list("a", "b", "c"), redis.zrange(key, 0, -1));
    }

    @Test
    public void zrangeStreaming() throws Exception {
        setup();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.zrange(streamingAdapter, key, 0, -1);
        assertEquals(3, count.longValue());

        assertEquals(list("a", "b", "c"), streamingAdapter.getList());
    }

    private void setup() {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c");
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrangeWithScores() throws Exception {
        setup();
        assertEquals(svlist(sv(1.0, "a"), sv(2.0, "b"), sv(3.0, "c")), redis.zrangeWithScores(key, 0, -1));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrangeWithScoresStreaming() throws Exception {
        setup();
        ScoredValueStreamingAdapter streamingAdapter = new ScoredValueStreamingAdapter();
        Long count = redis.zrangeWithScores(streamingAdapter, key, 0, -1);
        assertEquals(3, count.longValue());
        assertEquals(svlist(sv(1.0, "a"), sv(2.0, "b"), sv(3.0, "c")), streamingAdapter.getList());
    }

    @Test
    public void zrangebyscore() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertEquals(list("b", "c"), redis.zrangebyscore(key, 2.0, 3.0));
        assertEquals(list("b", "c"), redis.zrangebyscore(key, "(1.0", "(4.0"));
        assertEquals(list("a", "b", "c", "d"), redis.zrangebyscore(key, NEGATIVE_INFINITY, POSITIVE_INFINITY));
        assertEquals(list("a", "b", "c", "d"), redis.zrangebyscore(key, "-inf", "+inf"));
        assertEquals(list("b", "c", "d"), redis.zrangebyscore(key, 0.0, 4.0, 1, 3));
        assertEquals(list("c", "d"), redis.zrangebyscore(key, "-inf", "+inf", 2, 2));
    }

    @Test
    public void zrangebyscoreStreaming() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();

        assertEquals(2, redis.zrangebyscore(streamingAdapter, key, 2.0, 3.0).longValue());
        assertEquals(2, redis.zrangebyscore(streamingAdapter, key, "(1.0", "(4.0").longValue());
        assertEquals(4, redis.zrangebyscore(streamingAdapter, key, NEGATIVE_INFINITY, POSITIVE_INFINITY).longValue());
        assertEquals(4, redis.zrangebyscore(streamingAdapter, key, "-inf", "+inf").longValue());
        assertEquals(4, redis.zrangebyscore(streamingAdapter, key, "-inf", "+inf").longValue());
        assertEquals(3, redis.zrangebyscore(streamingAdapter, key, 0.0, 4.0, 1, 3).longValue());
        assertEquals(2, redis.zrangebyscore(streamingAdapter, key, "-inf", "+inf", 2, 2).longValue());

    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrangebyscoreWithScores() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertEquals(svlist(sv(2.0, "b"), sv(3.0, "c")), redis.zrangebyscoreWithScores(key, 2.0, 3.0));
        assertEquals(svlist(sv(2.0, "b"), sv(3.0, "c")), redis.zrangebyscoreWithScores(key, "(1.0", "(4.0"));
        assertEquals(svlist(sv(1.0, "a"), sv(2.0, "b"), sv(3.0, "c"), sv(4.0, "d")),
                redis.zrangebyscoreWithScores(key, NEGATIVE_INFINITY, POSITIVE_INFINITY));
        assertEquals(svlist(sv(1.0, "a"), sv(2.0, "b"), sv(3.0, "c"), sv(4.0, "d")),
                redis.zrangebyscoreWithScores(key, "-inf", "+inf"));
        assertEquals(svlist(sv(2.0, "b"), sv(3.0, "c"), sv(4.0, "d")), redis.zrangebyscoreWithScores(key, 0.0, 4.0, 1, 3));
        assertEquals(svlist(sv(3.0, "c"), sv(4.0, "d")), redis.zrangebyscoreWithScores(key, "-inf", "+inf", 2, 2));
    }

    @Test
    public void zrangebyscoreWithScoresStreaming() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();

        assertEquals(2, redis.zrangebyscoreWithScores(streamingAdapter, key, 2.0, 3.0).longValue());
        assertEquals(2, redis.zrangebyscoreWithScores(streamingAdapter, key, "(1.0", "(4.0").longValue());
        assertEquals(4, redis.zrangebyscoreWithScores(streamingAdapter, key, NEGATIVE_INFINITY, POSITIVE_INFINITY).longValue());
        assertEquals(4, redis.zrangebyscoreWithScores(streamingAdapter, key, "-inf", "+inf").longValue());
        assertEquals(4, redis.zrangebyscoreWithScores(streamingAdapter, key, "-inf", "+inf").longValue());
        assertEquals(3, redis.zrangebyscoreWithScores(streamingAdapter, key, 0.0, 4.0, 1, 3).longValue());
        assertEquals(2, redis.zrangebyscoreWithScores(streamingAdapter, key, "-inf", "+inf", 2, 2).longValue());

    }

    @Test
    public void zrank() throws Exception {
        assertNull(redis.zrank(key, "a"));
        setup();
        assertEquals(0, (long) redis.zrank(key, "a"));
        assertEquals(2, (long) redis.zrank(key, "c"));
    }

    @Test
    public void zrem() throws Exception {
        assertEquals(0, (long) redis.zrem(key, "a"));
        setup();
        assertEquals(1, (long) redis.zrem(key, "b"));
        assertEquals(list("a", "c"), redis.zrange(key, 0, -1));
        assertEquals(2, (long) redis.zrem(key, "a", "c"));
        assertEquals(list(), redis.zrange(key, 0, -1));
    }

    @Test
    public void zremrangebyscore() throws Exception {
        setup();
        assertEquals(2, (long) redis.zremrangebyscore(key, 1.0, 2.0));
        assertEquals(list("c"), redis.zrange(key, 0, -1));

        setup();
        assertEquals(1, (long) redis.zremrangebyscore(key, "(1.0", "(3.0"));
        assertEquals(list("a", "c"), redis.zrange(key, 0, -1));
    }

    @Test
    public void zremrangebyrank() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertEquals(2, (long) redis.zremrangebyrank(key, 1, 2));
        assertEquals(list("a", "d"), redis.zrange(key, 0, -1));

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertEquals(4, (long) redis.zremrangebyrank(key, 0, -1));
        assertEquals(0, (long) redis.zcard(key));
    }

    @Test
    public void zrevrange() throws Exception {
        setup();
        assertEquals(list("c", "b", "a"), redis.zrevrange(key, 0, -1));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrevrangeWithScores() throws Exception {
        setup();
        assertEquals(svlist(sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")), redis.zrevrangeWithScores(key, 0, -1));
    }

    @Test
    public void zrevrangeStreaming() throws Exception {
        setup();
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.zrevrange(streamingAdapter, key, 0, -1);
        assertEquals(3, count.longValue());
        assertEquals(list("c", "b", "a"), streamingAdapter.getList());
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrevrangeWithScoresStreaming() throws Exception {
        setup();
        ScoredValueStreamingAdapter streamingAdapter = new ScoredValueStreamingAdapter();
        Long count = redis.zrevrangeWithScores(streamingAdapter, key, 0, -1);
        assertEquals(3, count.longValue());
        assertEquals(svlist(sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")), streamingAdapter.getList());
    }

    @Test
    public void zrevrangebyscore() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertEquals(list("c", "b"), redis.zrevrangebyscore(key, 3.0, 2.0));
        assertEquals(list("c", "b"), redis.zrevrangebyscore(key, "(4.0", "(1.0"));
        assertEquals(list("d", "c", "b", "a"), redis.zrevrangebyscore(key, POSITIVE_INFINITY, NEGATIVE_INFINITY));
        assertEquals(list("d", "c", "b", "a"), redis.zrevrangebyscore(key, "+inf", "-inf"));
        assertEquals(list("c", "b", "a"), redis.zrevrangebyscore(key, 4.0, 0.0, 1, 3));
        assertEquals(list("b", "a"), redis.zrevrangebyscore(key, "+inf", "-inf", 2, 2));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrevrangebyscoreWithScores() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertEquals(svlist(sv(3.0, "c"), sv(2.0, "b")), redis.zrevrangebyscoreWithScores(key, 3.0, 2.0));
        assertEquals(svlist(sv(3.0, "c"), sv(2.0, "b")), redis.zrevrangebyscoreWithScores(key, "(4.0", "(1.0"));
        assertEquals(svlist(sv(4.0, "d"), sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")),
                redis.zrevrangebyscoreWithScores(key, POSITIVE_INFINITY, NEGATIVE_INFINITY));
        assertEquals(svlist(sv(4.0, "d"), sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")),
                redis.zrevrangebyscoreWithScores(key, "+inf", "-inf"));
        assertEquals(svlist(sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")), redis.zrevrangebyscoreWithScores(key, 4.0, 0.0, 1, 3));
        assertEquals(svlist(sv(2.0, "b"), sv(1.0, "a")), redis.zrevrangebyscoreWithScores(key, "+inf", "-inf", 2, 2));
    }

    @Test
    public void zrevrangebyscoreStreaming() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();

        assertEquals(2, redis.zrevrangebyscore(streamingAdapter, key, 3.0, 2.0).longValue());
        assertEquals(2, redis.zrevrangebyscore(streamingAdapter, key, "(4.0", "(1.0").longValue());
        assertEquals(4, redis.zrevrangebyscore(streamingAdapter, key, POSITIVE_INFINITY, NEGATIVE_INFINITY).longValue());
        assertEquals(4, redis.zrevrangebyscore(streamingAdapter, key, "+inf", "-inf").longValue());
        assertEquals(3, redis.zrevrangebyscore(streamingAdapter, key, 4.0, 0.0, 1, 3).longValue());
        assertEquals(2, redis.zrevrangebyscore(streamingAdapter, key, "+inf", "-inf", 2, 2).longValue());
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrevrangebyscoreWithScoresStreaming() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");

        ScoredValueStreamingAdapter streamingAdapter = new ScoredValueStreamingAdapter();

        assertEquals(2, redis.zrevrangebyscoreWithScores(streamingAdapter, key, 3.0, 2.0).longValue());
        assertEquals(2, redis.zrevrangebyscoreWithScores(streamingAdapter, key, "(4.0", "(1.0").longValue());
        assertEquals(4, redis.zrevrangebyscoreWithScores(streamingAdapter, key, POSITIVE_INFINITY, NEGATIVE_INFINITY)
                .longValue());
        assertEquals(4, redis.zrevrangebyscoreWithScores(streamingAdapter, key, "+inf", "-inf").longValue());
        assertEquals(3, redis.zrevrangebyscoreWithScores(streamingAdapter, key, 4.0, 0.0, 1, 3).longValue());
        assertEquals(2, redis.zrevrangebyscoreWithScores(streamingAdapter, key, "+inf", "-inf", 2, 2).longValue());
    }

    @Test
    public void zrevrank() throws Exception {
        assertNull(redis.zrevrank(key, "a"));
        setup();
        assertEquals(0, (long) redis.zrevrank(key, "c"));
        assertEquals(2, (long) redis.zrevrank(key, "a"));
    }

    @Test
    public void zscore() throws Exception {
        assertNull(redis.zscore(key, "a"));
        redis.zadd(key, 1.0, "a");
        assertEquals(1.0, redis.zscore(key, "a"), 0.0);
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zunionstore() throws Exception {
        redis.zadd("zset1", 1.0, "a", 2.0, "b");
        redis.zadd("zset2", 2.0, "a", 3.0, "b", 4.0, "c");
        assertEquals(3, redis.zunionstore(key, "zset1", "zset2"), 0.0);
        assertEquals(list("a", "c", "b"), redis.zrange(key, 0, -1));
        assertEquals(svlist(sv(3.0, "a"), sv(4.0, "c"), sv(5.0, "b")), redis.zrangeWithScores(key, 0, -1));

        assertEquals(3, redis.zunionstore(key, weights(2, 3), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(8.0, "a"), sv(12.0, "c"), sv(13.0, "b")), redis.zrangeWithScores(key, 0, -1));

        assertEquals(3, redis.zunionstore(key, weights(2, 3).sum(), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(8.0, "a"), sv(12.0, "c"), sv(13.0, "b")), redis.zrangeWithScores(key, 0, -1));

        assertEquals(3, redis.zunionstore(key, weights(2, 3).min(), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(2.0, "a"), sv(4.0, "b"), sv(12.0, "c")), redis.zrangeWithScores(key, 0, -1));

        assertEquals(3, redis.zunionstore(key, weights(2, 3).max(), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(6.0, "a"), sv(9.0, "b"), sv(12.0, "c")), redis.zrangeWithScores(key, 0, -1));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zStoreArgs() throws Exception {
        redis.zadd("zset1", 1.0, "a", 2.0, "b");
        redis.zadd("zset2", 2.0, "a", 3.0, "b", 4.0, "c");

        assertEquals(2, redis.zinterstore(key, sum(), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(3.0, "a"), sv(5.0, "b")), redis.zrangeWithScores(key, 0, -1));

        assertEquals(2, redis.zinterstore(key, min(), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(1.0, "a"), sv(2.0, "b")), redis.zrangeWithScores(key, 0, -1));

        assertEquals(2, redis.zinterstore(key, max(), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(2.0, "a"), sv(3.0, "b")), redis.zrangeWithScores(key, 0, -1));

        assertEquals(2, redis.zinterstore(key, weights(2, 3), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(8.0, "a"), sv(13.0, "b")), redis.zrangeWithScores(key, 0, -1));

        assertEquals(2, redis.zinterstore(key, weights(2, 3).sum(), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(8.0, "a"), sv(13.0, "b")), redis.zrangeWithScores(key, 0, -1));

        assertEquals(2, redis.zinterstore(key, weights(2, 3).min(), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(2.0, "a"), sv(4.0, "b")), redis.zrangeWithScores(key, 0, -1));

        assertEquals(2, redis.zinterstore(key, weights(2, 3).max(), "zset1", "zset2"), 0.0);
        assertEquals(svlist(sv(6.0, "a"), sv(9.0, "b")), redis.zrangeWithScores(key, 0, -1));
    }

    @Test
    public void zsscan() throws Exception {
        redis.zadd(key, 1, value);
        ScoredValueScanCursor<String> cursor = redis.zscan(key);

        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());
        assertEquals(new ScoredValue<String>(1, value), cursor.getValues().get(0));

    }

    @Test
    public void zscanStreaming() throws Exception {
        redis.zadd(key, 1, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.zscan(adapter, key, ScanArgs.Builder.count(100).match("*"));

        assertEquals(1, cursor.getCount());
        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());
        assertEquals(value, adapter.getList().get(0));

    }

    @Test
    public void zscanMultiple() throws Exception {

        Set<String> expect = new HashSet<String>();
        setup100KeyValues(expect);

        ScoredValueScanCursor<String> cursor = redis.zscan(key, ScanArgs.Builder.count(5));

        assertNotNull(cursor.getCursor());
        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());

        assertEquals(100, cursor.getValues().size());

    }

    @Test
    public void zscanMatch() throws Exception {

        Set<String> expect = new HashSet<String>();
        setup100KeyValues(expect);

        ScoredValueScanCursor<String> cursor = redis.zscan(key, ScanArgs.Builder.count(10).match("val*"));

        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());

        assertEquals(100, cursor.getValues().size());
    }

    @Test
    public void zlexcount() throws Exception {
        setup100KeyValues(new HashSet<String>());
        Long result = redis.zlexcount(key, "-", "+");

        assertEquals(100, result.longValue());

        Long resultFromTo = redis.zlexcount(key, "[value", "[zzz");
        assertEquals(100, resultFromTo.longValue());
    }

    @Test
    public void zrangebylex() throws Exception {
        setup100KeyValues(new HashSet<String>());
        List<String> result = redis.zrangebylex(key, "-", "+");

        assertEquals(100, result.size());

        List<String> result2 = redis.zrangebylex(key, "-", "+", 10, 10);

        assertEquals(10, result2.size());
    }

    @Test
    public void zremrangebylex() throws Exception {
        setup100KeyValues(new HashSet<String>());
        Long result = redis.zremrangebylex(key, "(aaa", "[zzz");

        assertEquals(100, result.longValue());

    }

    protected void setup100KeyValues(Set<String> expect) {
        for (int i = 0; i < 100; i++) {
            redis.zadd(key + 1, i, value + i);
            redis.zadd(key, i, value + i);
            expect.add(value + i);
        }

  }
}
