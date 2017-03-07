/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.commands;

import static io.lettuce.core.ZStoreArgs.Builder.max;
import static io.lettuce.core.ZStoreArgs.Builder.min;
import static io.lettuce.core.ZStoreArgs.Builder.sum;
import static io.lettuce.core.ZStoreArgs.Builder.weights;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import io.lettuce.core.*;
import io.lettuce.core.Range.Boundary;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public class SortedSetCommandTest extends AbstractRedisClientTest {

    @Test
    public void zadd() throws Exception {
        assertThat(redis.zadd(key, 1.0, "a")).isEqualTo(1);
        assertThat(redis.zadd(key, 1.0, "a")).isEqualTo(0);

        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a"));
        assertThat(redis.zadd(key, 2.0, "b", 3.0, "c")).isEqualTo(2);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "b", "c"));
    }

    @Test
    public void zaddScoredValue() throws Exception {
        assertThat(redis.zadd(key, ScoredValue.fromNullable(1.0, "a"))).isEqualTo(1);
        assertThat(redis.zadd(key, ScoredValue.fromNullable(1.0, "a"))).isEqualTo(0);

        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a"));
        assertThat(redis.zadd(key, ScoredValue.fromNullable(2.0, "b"), ScoredValue.fromNullable(3.0, "c"))).isEqualTo(2);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "b", "c"));
    }

    @Test
    public void zaddnx() throws Exception {
        assertThat(redis.zadd(key, 1.0, "a")).isEqualTo(1);
        assertThat(redis.zadd(key, ZAddArgs.Builder.nx(), ScoredValue.fromNullable(2.0, "a"))).isEqualTo(0);

        assertThat(redis.zadd(key, ZAddArgs.Builder.nx(), ScoredValue.fromNullable(2.0, "b"))).isEqualTo(1);

        assertThat(redis.zadd(key, ZAddArgs.Builder.nx(), new Object[] { 2.0, "b", 3.0, "c" })).isEqualTo(1);

        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(1.0, "a"), sv(2.0, "b"), sv(3.0, "c")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void zaddWrongArguments() throws Exception {
        assertThat(redis.zadd(key, 2.0, "b", 3.0)).isEqualTo(2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zaddnxWrongArguments() throws Exception {
        assertThat(redis.zadd(key, ZAddArgs.Builder.nx(), new Object[] { 2.0, "b", 3.0 })).isEqualTo(1);
    }

    @Test
    public void zaddxx() throws Exception {
        assertThat(redis.zadd(key, 1.0, "a")).isEqualTo(1);
        assertThat(redis.zadd(key, ZAddArgs.Builder.xx(), 2.0, "a")).isEqualTo(0);

        assertThat(redis.zadd(key, ZAddArgs.Builder.xx(), 2.0, "b")).isEqualTo(0);

        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(2.0, "a")));
    }

    @Test
    public void zaddch() throws Exception {
        assertThat(redis.zadd(key, 1.0, "a")).isEqualTo(1);
        assertThat(redis.zadd(key, ZAddArgs.Builder.ch().xx(), 2.0, "a")).isEqualTo(1);
        assertThat(redis.zadd(key, ZAddArgs.Builder.ch(), 2.0, "b")).isEqualTo(1);

        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(2.0, "a"), sv(2.0, "b")));
    }

    @Test
    public void zaddincr() throws Exception {
        assertThat(redis.zadd(key, 1.0, "a").longValue()).isEqualTo(1);
        assertThat(redis.zaddincr(key, 2.0, "a").longValue()).isEqualTo(3);

        assertThat(redis.zaddincr(key, 2.0, "b").longValue()).isEqualTo(2);

        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(2.0, "b"), sv(3.0, "a")));
    }

    @Test
    public void zaddincrnx() throws Exception {
        assertThat(redis.zaddincr(key, ZAddArgs.Builder.nx(), 2.0, "a").longValue()).isEqualTo(2);
        assertThat(redis.zaddincr(key, ZAddArgs.Builder.nx(), 2.0, "a")).isNull();
    }

    @Test
    public void zaddincrxx() throws Exception {
        assertThat(redis.zaddincr(key, ZAddArgs.Builder.xx(), 2.0, "a")).isNull();
        assertThat(redis.zaddincr(key, ZAddArgs.Builder.nx(), 2.0, "a").longValue()).isEqualTo(2);
        assertThat(redis.zaddincr(key, ZAddArgs.Builder.xx(), 2.0, "a").longValue()).isEqualTo(4);
    }

    @Test
    public void zcard() throws Exception {
        assertThat(redis.zcard(key)).isEqualTo(0);
        redis.zadd(key, 1.0, "a");
        assertThat(redis.zcard(key)).isEqualTo(1);
    }

    @Test
    public void zcount() throws Exception {
        assertThat(redis.zcount(key, 0, 0)).isEqualTo(0);

        redis.zadd(key, 1.0, "a", 2.0, "b", 2.1, "c");

        assertThat(redis.zcount(key, 1.0, 3.0)).isEqualTo(3);
        assertThat(redis.zcount(key, 1.0, 2.0)).isEqualTo(2);
        assertThat(redis.zcount(key, NEGATIVE_INFINITY, POSITIVE_INFINITY)).isEqualTo(3);

        assertThat(redis.zcount(key, "(1.0", "3.0")).isEqualTo(2);
        assertThat(redis.zcount(key, "-inf", "+inf")).isEqualTo(3);

        assertThat(redis.zcount(key, Range.create(1.0, 3.0))).isEqualTo(3);
        assertThat(redis.zcount(key, Range.create(1.0, 2.0))).isEqualTo(2);
        assertThat(redis.zcount(key, Range.create(NEGATIVE_INFINITY, POSITIVE_INFINITY))).isEqualTo(3);

        assertThat(redis.zcount(key, Range.from(Boundary.excluding(1.0), Boundary.including(3.0)))).isEqualTo(2);
        assertThat(redis.zcount(key, Range.unbounded())).isEqualTo(3);
    }

    @Test
    public void zincrby() throws Exception {
        assertThat(redis.zincrby(key, 0.0, "a")).isEqualTo(0, offset(0.1));
        assertThat(redis.zincrby(key, 1.1, "a")).isEqualTo(1.1, offset(0.1));
        assertThat(redis.zscore(key, "a")).isEqualTo(1.1, offset(0.1));
        assertThat(redis.zincrby(key, -1.2, "a")).isEqualTo(-0.1, offset(0.1));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zinterstore() throws Exception {
        redis.zadd("zset1", 1.0, "a", 2.0, "b");
        redis.zadd("zset2", 2.0, "a", 3.0, "b", 4.0, "c");
        assertThat(redis.zinterstore(key, "zset1", "zset2")).isEqualTo(2);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "b"));
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(3.0, "a"), sv(5.0, "b")));
    }

    @Test
    public void zrange() throws Exception {
        setup();
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "b", "c"));
    }

    @Test
    public void zrangeStreaming() throws Exception {
        setup();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();
        Long count = redis.zrange(streamingAdapter, key, 0, -1);
        assertThat(count.longValue()).isEqualTo(3);

        assertThat(streamingAdapter.getList()).isEqualTo(list("a", "b", "c"));
    }

    private void setup() {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c");
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrangeWithScores() throws Exception {
        setup();
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(1.0, "a"), sv(2.0, "b"), sv(3.0, "c")));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrangeWithScoresStreaming() throws Exception {
        setup();
        ScoredValueStreamingAdapter<String> streamingAdapter = new ScoredValueStreamingAdapter<>();
        Long count = redis.zrangeWithScores(streamingAdapter, key, 0, -1);
        assertThat(count.longValue()).isEqualTo(3);
        assertThat(streamingAdapter.getList()).isEqualTo(svlist(sv(1.0, "a"), sv(2.0, "b"), sv(3.0, "c")));
    }

    @Test
    public void zrangebyscore() throws Exception {

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");

        assertThat(redis.zrangebyscore(key, 2.0, 3.0)).isEqualTo(list("b", "c"));
        assertThat(redis.zrangebyscore(key, "(1.0", "(4.0")).isEqualTo(list("b", "c"));
        assertThat(redis.zrangebyscore(key, NEGATIVE_INFINITY, POSITIVE_INFINITY)).isEqualTo(list("a", "b", "c", "d"));
        assertThat(redis.zrangebyscore(key, "-inf", "+inf")).isEqualTo(list("a", "b", "c", "d"));
        assertThat(redis.zrangebyscore(key, 0.0, 4.0, 1, 3)).isEqualTo(list("b", "c", "d"));
        assertThat(redis.zrangebyscore(key, "-inf", "+inf", 2, 2)).isEqualTo(list("c", "d"));

        assertThat(redis.zrangebyscore(key, Range.create(2.0, 3.0))).isEqualTo(list("b", "c"));
        assertThat(redis.zrangebyscore(key, Range.from(Boundary.excluding(1.0), Boundary.excluding(4.0)))).isEqualTo(list("b", "c"));
        assertThat(redis.zrangebyscore(key, Range.unbounded())).isEqualTo(list("a", "b", "c", "d"));
        assertThat(redis.zrangebyscore(key, Range.create(0.0, 4.0), Limit.create(1, 3))).isEqualTo(list("b", "c", "d"));
        assertThat(redis.zrangebyscore(key, Range.unbounded(), Limit.create(2, 2))).isEqualTo(list("c", "d"));
    }

    @Test
    public void zrangebyscoreStreaming() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();

        assertThat(redis.zrangebyscore(streamingAdapter, key, 2.0, 3.0)).isEqualTo(2);
        assertThat(redis.zrangebyscore(streamingAdapter, key, "(1.0", "(4.0")).isEqualTo(2);
        assertThat(redis.zrangebyscore(streamingAdapter, key, NEGATIVE_INFINITY, POSITIVE_INFINITY)).isEqualTo(4);
        assertThat(redis.zrangebyscore(streamingAdapter, key, "-inf", "+inf")).isEqualTo(4);
        assertThat(redis.zrangebyscore(streamingAdapter, key, "-inf", "+inf")).isEqualTo(4);
        assertThat(redis.zrangebyscore(streamingAdapter, key, 0.0, 4.0, 1, 3)).isEqualTo(3);
        assertThat(redis.zrangebyscore(streamingAdapter, key, "-inf", "+inf", 2, 2)).isEqualTo(2);

        assertThat(redis.zrangebyscore(streamingAdapter, key, Range.create(2.0, 3.0))).isEqualTo(2);
        assertThat(redis.zrangebyscore(streamingAdapter, key, Range.from(Boundary.excluding(1.0), Boundary.excluding(4.0)))).isEqualTo(2);
        assertThat(redis.zrangebyscore(streamingAdapter, key, Range.unbounded())).isEqualTo(4);
        assertThat(redis.zrangebyscore(streamingAdapter, key, Range.create(0.0, 4.0), Limit.create(1, 3))).isEqualTo(3);
        assertThat(redis.zrangebyscore(streamingAdapter, key, Range.unbounded(), Limit.create(2, 2))).isEqualTo(2);
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrangebyscoreWithScores() throws Exception {

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");

        assertThat(redis.zrangebyscoreWithScores(key, 2.0, 3.0)).isEqualTo(svlist(sv(2.0, "b"), sv(3.0, "c")));
        assertThat(redis.zrangebyscoreWithScores(key, "(1.0", "(4.0")).isEqualTo(svlist(sv(2.0, "b"), sv(3.0, "c")));
        assertThat(redis.zrangebyscoreWithScores(key, NEGATIVE_INFINITY, POSITIVE_INFINITY)).isEqualTo(
                svlist(sv(1.0, "a"), sv(2.0, "b"), sv(3.0, "c"), sv(4.0, "d")));
        assertThat(redis.zrangebyscoreWithScores(key, "-inf", "+inf")).isEqualTo(
                svlist(sv(1.0, "a"), sv(2.0, "b"), sv(3.0, "c"), sv(4.0, "d")));
        assertThat(redis.zrangebyscoreWithScores(key, 0.0, 4.0, 1, 3)).isEqualTo(
                svlist(sv(2.0, "b"), sv(3.0, "c"), sv(4.0, "d")));
        assertThat(redis.zrangebyscoreWithScores(key, "-inf", "+inf", 2, 2)).isEqualTo(svlist(sv(3.0, "c"), sv(4.0, "d")));

        assertThat(redis.zrangebyscoreWithScores(key, Range.create(2.0, 3.0))).isEqualTo(svlist(sv(2.0, "b"), sv(3.0, "c")));
        assertThat(redis.zrangebyscoreWithScores(key, Range.from(Boundary.excluding(1.0), Boundary.excluding(4.0)))).isEqualTo(svlist(sv(2.0, "b"), sv(3.0, "c")));
        assertThat(redis.zrangebyscoreWithScores(key, Range.unbounded())).isEqualTo(
                svlist(sv(1.0, "a"), sv(2.0, "b"), sv(3.0, "c"), sv(4.0, "d")));
        assertThat(redis.zrangebyscoreWithScores(key, Range.create(0.0, 4.0), Limit.create(1, 3))).isEqualTo(
                svlist(sv(2.0, "b"), sv(3.0, "c"), sv(4.0, "d")));
        assertThat(redis.zrangebyscoreWithScores(key, Range.unbounded(), Limit.create(2, 2))).isEqualTo(svlist(sv(3.0, "c"), sv(4.0, "d")));
    }

    @Test
    public void zrangebyscoreWithScoresStreaming() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();

        assertThat(redis.zrangebyscoreWithScores(streamingAdapter, key, 2.0, 3.0).longValue()).isEqualTo(2);
        assertThat(redis.zrangebyscoreWithScores(streamingAdapter, key, "(1.0", "(4.0").longValue()).isEqualTo(2);
        assertThat(redis.zrangebyscoreWithScores(streamingAdapter, key, NEGATIVE_INFINITY, POSITIVE_INFINITY).longValue())
                .isEqualTo(4);
        assertThat(redis.zrangebyscoreWithScores(streamingAdapter, key, "-inf", "+inf").longValue()).isEqualTo(4);
        assertThat(redis.zrangebyscoreWithScores(streamingAdapter, key, "-inf", "+inf").longValue()).isEqualTo(4);
        assertThat(redis.zrangebyscoreWithScores(streamingAdapter, key, 0.0, 4.0, 1, 3).longValue()).isEqualTo(3);
        assertThat(redis.zrangebyscoreWithScores(streamingAdapter, key, "-inf", "+inf", 2, 2).longValue()).isEqualTo(2);

        assertThat(redis.zrangebyscoreWithScores(streamingAdapter,key, Range.create(2.0, 3.0))).isEqualTo(2);
        assertThat(redis.zrangebyscoreWithScores(streamingAdapter,key, Range.from(Boundary.excluding(1.0), Boundary.excluding(4.0)))).isEqualTo(2);
        assertThat(redis.zrangebyscoreWithScores(streamingAdapter,key, Range.unbounded())).isEqualTo(4);
        assertThat(redis.zrangebyscoreWithScores(streamingAdapter,key, Range.create(0.0, 4.0), Limit.create(1, 3))).isEqualTo(3);
        assertThat(redis.zrangebyscoreWithScores(streamingAdapter,key, Range.unbounded(), Limit.create(2, 2))).isEqualTo(2);

    }

    @Test
    public void zrank() throws Exception {
        assertThat(redis.zrank(key, "a")).isNull();
        setup();
        assertThat(redis.zrank(key, "a")).isEqualTo(0);
        assertThat(redis.zrank(key, "c")).isEqualTo(2);
    }

    @Test
    public void zrem() throws Exception {
        assertThat(redis.zrem(key, "a")).isEqualTo(0);
        setup();
        assertThat(redis.zrem(key, "b")).isEqualTo(1);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "c"));
        assertThat(redis.zrem(key, "a", "c")).isEqualTo(2);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list());
    }

    @Test
    public void zremrangebyscore() throws Exception {

        setup();
        assertThat(redis.zremrangebyscore(key, 1.0, 2.0)).isEqualTo(2);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("c"));

        setup();
        assertThat(redis.zremrangebyscore(key, Range.create(1.0, 2.0))).isEqualTo(2);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("c"));

        setup();
        assertThat(redis.zremrangebyscore(key, "(1.0", "(3.0")).isEqualTo(1);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "c"));

        setup();
        assertThat(redis.zremrangebyscore(key, Range.from(Boundary.excluding(1.0), Boundary.excluding(3.0)))).isEqualTo(1);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "c"));
    }

    @Test
    public void zremrangebyrank() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertThat(redis.zremrangebyrank(key, 1, 2)).isEqualTo(2);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "d"));

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertThat(redis.zremrangebyrank(key, 0, -1)).isEqualTo(4);
        assertThat(redis.zcard(key)).isEqualTo(0);
    }

    @Test
    public void zrevrange() throws Exception {
        setup();
        assertThat(redis.zrevrange(key, 0, -1)).isEqualTo(list("c", "b", "a"));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrevrangeWithScores() throws Exception {
        setup();
        assertThat(redis.zrevrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")));
    }

    @Test
    public void zrevrangeStreaming() throws Exception {
        setup();
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();
        Long count = redis.zrevrange(streamingAdapter, key, 0, -1);
        assertThat(count).isEqualTo(3);
        assertThat(streamingAdapter.getList()).isEqualTo(list("c", "b", "a"));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrevrangeWithScoresStreaming() throws Exception {
        setup();
        ScoredValueStreamingAdapter<String> streamingAdapter = new ScoredValueStreamingAdapter<>();
        Long count = redis.zrevrangeWithScores(streamingAdapter, key, 0, -1);
        assertThat(count).isEqualTo(3);
        assertThat(streamingAdapter.getList()).isEqualTo(svlist(sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")));
    }

    @Test
    public void zrevrangebylex() throws Exception {

        setup100KeyValues(new HashSet<>());

        assertThat(redis.zrevrangebylex(key, Range.unbounded())).hasSize(100);
        assertThat(redis.zrevrangebylex(key, Range.create("value", "zzz"))).hasSize(100);
        assertThat(redis.zrevrangebylex(key, Range.from(Boundary.including("value98"), Boundary.including("value99")))).containsSequence("value99", "value98");
        assertThat(redis.zrevrangebylex(key, Range.from(Boundary.including("value99"), Boundary.unbounded()))).hasSize(1);
        assertThat(redis.zrevrangebylex(key, Range.from(Boundary.excluding("value99"), Boundary.unbounded()))).hasSize(0);
    }

    @Test
    public void zrevrangebyscore() throws Exception {

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");

        assertThat(redis.zrevrangebyscore(key, 3.0, 2.0)).isEqualTo(list("c", "b"));
        assertThat(redis.zrevrangebyscore(key, "(4.0", "(1.0")).isEqualTo(list("c", "b"));
        assertThat(redis.zrevrangebyscore(key, POSITIVE_INFINITY, NEGATIVE_INFINITY)).isEqualTo(list("d", "c", "b", "a"));
        assertThat(redis.zrevrangebyscore(key, "+inf", "-inf")).isEqualTo(list("d", "c", "b", "a"));
        assertThat(redis.zrevrangebyscore(key, 4.0, 0.0, 1, 3)).isEqualTo(list("c", "b", "a"));
        assertThat(redis.zrevrangebyscore(key, "+inf", "-inf", 2, 2)).isEqualTo(list("b", "a"));

        assertThat(redis.zrevrangebyscore(key, Range.create(2.0, 3.0))).isEqualTo(list("c", "b"));
        assertThat(redis.zrevrangebyscore(key, Range.from(Boundary.excluding(1.0), Boundary.excluding(4.0)))).isEqualTo(list("c", "b"));
        assertThat(redis.zrevrangebyscore(key, Range.unbounded())).isEqualTo(list("d", "c", "b", "a"));
        assertThat(redis.zrevrangebyscore(key, Range.create(0.0, 4.0), Limit.create(1, 3))).isEqualTo(list("c", "b", "a"));
        assertThat(redis.zrevrangebyscore(key, Range.unbounded(), Limit.create(2, 2))).isEqualTo(list("b", "a"));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrevrangebyscoreWithScores() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");

        assertThat(redis.zrevrangebyscoreWithScores(key, 3.0, 2.0)).isEqualTo(svlist(sv(3.0, "c"), sv(2.0, "b")));
        assertThat(redis.zrevrangebyscoreWithScores(key, "(4.0", "(1.0")).isEqualTo(svlist(sv(3.0, "c"), sv(2.0, "b")));
        assertThat(redis.zrevrangebyscoreWithScores(key, POSITIVE_INFINITY, NEGATIVE_INFINITY)).isEqualTo(
                svlist(sv(4.0, "d"), sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")));
        assertThat(redis.zrevrangebyscoreWithScores(key, "+inf", "-inf")).isEqualTo(
                svlist(sv(4.0, "d"), sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")));
        assertThat(redis.zrevrangebyscoreWithScores(key, 4.0, 0.0, 1, 3)).isEqualTo(
                svlist(sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")));
        assertThat(redis.zrevrangebyscoreWithScores(key, "+inf", "-inf", 2, 2)).isEqualTo(svlist(sv(2.0, "b"), sv(1.0, "a")));

        assertThat(redis.zrevrangebyscoreWithScores(key, Range.create(2.0, 3.0))).isEqualTo(svlist(sv(3.0, "c"), sv(2.0, "b")));
        assertThat(redis.zrevrangebyscoreWithScores(key, Range.from(Boundary.excluding(1.0), Boundary.excluding(4.0)))).isEqualTo(svlist(sv(3.0, "c"), sv(2.0, "b")));
        assertThat(redis.zrevrangebyscoreWithScores(key, Range.unbounded())).isEqualTo(
                svlist(sv(4.0, "d"), sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")));
        assertThat(redis.zrevrangebyscoreWithScores(key, Range.create(0.0, 4.0), Limit.create(1, 3))).isEqualTo(
                svlist(sv(3.0, "c"), sv(2.0, "b"), sv(1.0, "a")));
        assertThat(redis.zrevrangebyscoreWithScores(key, Range.unbounded(), Limit.create(2, 2))).isEqualTo(svlist(sv(2.0, "b"), sv(1.0, "a")));
    }

    @Test
    public void zrevrangebyscoreStreaming() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();

        assertThat(redis.zrevrangebyscore(streamingAdapter, key, 3.0, 2.0).longValue()).isEqualTo(2);
        assertThat(redis.zrevrangebyscore(streamingAdapter, key, "(4.0", "(1.0").longValue()).isEqualTo(2);
        assertThat(redis.zrevrangebyscore(streamingAdapter, key, POSITIVE_INFINITY, NEGATIVE_INFINITY).longValue())
                .isEqualTo(4);
        assertThat(redis.zrevrangebyscore(streamingAdapter, key, "+inf", "-inf").longValue()).isEqualTo(4);
        assertThat(redis.zrevrangebyscore(streamingAdapter, key, 4.0, 0.0, 1, 3).longValue()).isEqualTo(3);
        assertThat(redis.zrevrangebyscore(streamingAdapter, key, "+inf", "-inf", 2, 2).longValue()).isEqualTo(2);

        assertThat(redis.zrevrangebyscore(streamingAdapter, key, Range.create(2.0, 3.0)).longValue()).isEqualTo(2);
        assertThat(redis.zrevrangebyscore(streamingAdapter, key, Range.from(Boundary.excluding(1.0), Boundary.excluding(4.0))).longValue()).isEqualTo(2);
        assertThat(redis.zrevrangebyscore(streamingAdapter, key, Range.unbounded()).longValue()).isEqualTo(4);
        assertThat(redis.zrevrangebyscore(streamingAdapter, key, Range.create(0.0, 4.0), Limit.create(1, 3)).longValue()).isEqualTo(3);
        assertThat(redis.zrevrangebyscore(streamingAdapter, key, Range.unbounded(), Limit.create(2, 2)).longValue()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zrevrangebyscoreWithScoresStreaming() throws Exception {
        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");

        ScoredValueStreamingAdapter<String> streamingAdapter = new ScoredValueStreamingAdapter<>();

        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, 3.0, 2.0)).isEqualTo(2);
        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, "(4.0", "(1.0")).isEqualTo(2);
        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, POSITIVE_INFINITY, NEGATIVE_INFINITY)).isEqualTo(4);
        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, "+inf", "-inf")).isEqualTo(4);
        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, 4.0, 0.0, 1, 3)).isEqualTo(3);
        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, "+inf", "-inf", 2, 2)).isEqualTo(2);

        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, Range.create(2.0, 3.0)).longValue()).isEqualTo(2);
        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, Range.from(Boundary.excluding(1.0), Boundary.excluding(4.0))).longValue()).isEqualTo(2);
        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, Range.unbounded()).longValue()).isEqualTo(4);
        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, Range.create(0.0, 4.0), Limit.create(1, 3)).longValue()).isEqualTo(3);
        assertThat(redis.zrevrangebyscoreWithScores(streamingAdapter, key, Range.unbounded(), Limit.create(2, 2)).longValue()).isEqualTo(2);
    }

    @Test
    public void zrevrank() throws Exception {
        assertThat(redis.zrevrank(key, "a")).isNull();
        setup();
        assertThat(redis.zrevrank(key, "c")).isEqualTo(0);
        assertThat(redis.zrevrank(key, "a")).isEqualTo(2);
    }

    @Test
    public void zscore() throws Exception {
        assertThat(redis.zscore(key, "a")).isNull();
        redis.zadd(key, 1.0, "a");
        assertThat(redis.zscore(key, "a")).isEqualTo(1.0);
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zunionstore() throws Exception {
        redis.zadd("zset1", 1.0, "a", 2.0, "b");
        redis.zadd("zset2", 2.0, "a", 3.0, "b", 4.0, "c");
        assertThat(redis.zunionstore(key, "zset1", "zset2")).isEqualTo(3);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "c", "b"));
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(3.0, "a"), sv(4.0, "c"), sv(5.0, "b")));

        assertThat(redis.zunionstore(key, weights(new long[] { 2, 3 }), "zset1", "zset2")).isEqualTo(3);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(8.0, "a"), sv(12.0, "c"), sv(13.0, "b")));

        assertThat(redis.zunionstore(key, weights(2, 3).sum(), "zset1", "zset2")).isEqualTo(3);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(8.0, "a"), sv(12.0, "c"), sv(13.0, "b")));

        assertThat(redis.zunionstore(key, weights(2, 3).min(), "zset1", "zset2")).isEqualTo(3);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(2.0, "a"), sv(4.0, "b"), sv(12.0, "c")));

        assertThat(redis.zunionstore(key, weights(2, 3).max(), "zset1", "zset2")).isEqualTo(3);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(6.0, "a"), sv(9.0, "b"), sv(12.0, "c")));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void zStoreArgs() throws Exception {
        redis.zadd("zset1", 1.0, "a", 2.0, "b");
        redis.zadd("zset2", 2.0, "a", 3.0, "b", 4.0, "c");

        assertThat(redis.zinterstore(key, sum(), "zset1", "zset2")).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(3.0, "a"), sv(5.0, "b")));

        assertThat(redis.zinterstore(key, min(), "zset1", "zset2")).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(1.0, "a"), sv(2.0, "b")));

        assertThat(redis.zinterstore(key, max(), "zset1", "zset2")).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(2.0, "a"), sv(3.0, "b")));

        assertThat(redis.zinterstore(key, weights(new long[] { 2, 3 }), "zset1", "zset2")).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(8.0, "a"), sv(13.0, "b")));

        assertThat(redis.zinterstore(key, weights(2, 3).sum(), "zset1", "zset2")).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(8.0, "a"), sv(13.0, "b")));

        assertThat(redis.zinterstore(key, weights(2, 3).min(), "zset1", "zset2")).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(2.0, "a"), sv(4.0, "b")));

        assertThat(redis.zinterstore(key, weights(2, 3).max(), "zset1", "zset2")).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(6.0, "a"), sv(9.0, "b")));
    }

    @Test
    public void zsscan() throws Exception {
        redis.zadd(key, 1, value);
        ScoredValueScanCursor<String> cursor = redis.zscan(key);

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getValues().get(0)).isEqualTo(sv(1, value));
    }

    @Test
    public void zsscanWithCursor() throws Exception {
        redis.zadd(key, 1, value);

        ScoredValueScanCursor<String> cursor = redis.zscan(key, ScanCursor.INITIAL);

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getValues().get(0)).isEqualTo(sv(1, value));
    }

    @Test
    public void zsscanWithCursorAndArgs() throws Exception {
        redis.zadd(key, 1, value);

        ScoredValueScanCursor<String> cursor = redis.zscan(key, ScanCursor.INITIAL, ScanArgs.Builder.limit(5));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getValues().get(0)).isEqualTo(sv(1, value));
    }

    @Test
    public void zscanStreaming() throws Exception {
        redis.zadd(key, 1, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.zscan(adapter, key);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getList().get(0)).isEqualTo(value);
    }

    @Test
    public void zscanStreamingWithCursor() throws Exception {
        redis.zadd(key, 1, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.zscan(adapter, key, ScanCursor.INITIAL);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    public void zscanStreamingWithCursorAndArgs() throws Exception {
        redis.zadd(key, 1, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.zscan(adapter, key, ScanCursor.INITIAL, ScanArgs.Builder.matches("*").limit(100));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    public void zscanStreamingWithArgs() throws Exception {
        redis.zadd(key, 1, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.zscan(adapter, key, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

    }

    @Test
    public void zscanMultiple() throws Exception {

        Set<String> expect = new HashSet<>();
        setup100KeyValues(expect);

        ScoredValueScanCursor<String> cursor = redis.zscan(key, ScanArgs.Builder.limit(5));

        assertThat(cursor.getCursor()).isNotNull();
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

        assertThat(cursor.getValues()).hasSize(100);

    }

    @Test
    public void zscanMatch() throws Exception {

        Set<String> expect = new HashSet<>();
        setup100KeyValues(expect);

        ScoredValueScanCursor<String> cursor = redis.zscan(key, ScanArgs.Builder.limit(10).match("val*"));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

        assertThat(cursor.getValues()).hasSize(100);
    }

    @Test
    public void zlexcount() throws Exception {

        setup100KeyValues(new HashSet<>());

        assertThat(redis.zlexcount(key, "-", "+")).isEqualTo(100);
        assertThat(redis.zlexcount(key, "[value", "[zzz")).isEqualTo(100);

        assertThat(redis.zlexcount(key, Range.unbounded())).isEqualTo(100);
        assertThat(redis.zlexcount(key, Range.create("value", "zzz"))).isEqualTo(100);
        assertThat(redis.zlexcount(key, Range.from(Boundary.including("value99"), Boundary.unbounded()))).isEqualTo(1);
        assertThat(redis.zlexcount(key, Range.from(Boundary.excluding("value99"), Boundary.unbounded()))).isEqualTo(0);
    }

    @Test
    public void zrangebylex() throws Exception {
        setup100KeyValues(new HashSet<>());

        assertThat(redis.zrangebylex(key, "-", "+")).hasSize(100);
        assertThat(redis.zrangebylex(key, "-", "+", 10, 10)).hasSize(10);

        assertThat(redis.zrangebylex(key, Range.unbounded())).hasSize(100);
        assertThat(redis.zrangebylex(key, Range.create("value", "zzz"))).hasSize(100);
        assertThat(redis.zrangebylex(key, Range.from(Boundary.including("value98"), Boundary.including("value99")))).containsSequence("value98", "value99");
        assertThat(redis.zrangebylex(key, Range.from(Boundary.including("value99"), Boundary.unbounded()))).hasSize(1);
        assertThat(redis.zrangebylex(key, Range.from(Boundary.excluding("value99"), Boundary.unbounded()))).hasSize(0);
    }

    @Test
    public void zremrangebylex() throws Exception {

        setup100KeyValues(new HashSet<>());
        assertThat(redis.zremrangebylex(key, "(aaa", "[zzz")).isEqualTo(100);

        setup100KeyValues(new HashSet<>());
        assertThat(redis.zremrangebylex(key, Range.create("value", "zzz"))).isEqualTo(100);

    }

    protected void setup100KeyValues(Set<String> expect) {
        for (int i = 0; i < 100; i++) {
            redis.zadd(key + 1, i, value + i);
            redis.zadd(key, i, value + i);
            expect.add(value + i);
        }
    }
}
