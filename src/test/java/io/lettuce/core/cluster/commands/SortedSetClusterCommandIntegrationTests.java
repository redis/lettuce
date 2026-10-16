/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cluster.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.KeyValue;
import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.Range.Boundary;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.ZPopArgs;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.commands.SortedSetCommandIntegrationTests;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.condition.RedisConditions;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisSortedSetCommands} using Redis Cluster.
 * <p>
 * Extends {@link SortedSetCommandIntegrationTests}. Multi-key tests are re-implemented with hash-tagged keys so that all keys
 * of a single command hash to the same slot.
 */
@Tag(INTEGRATION_TEST)
class SortedSetClusterCommandIntegrationTests extends SortedSetCommandIntegrationTests {

    private final RedisClusterCommands<String, String> redis;

    @Inject
    SortedSetClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
        this.redis = connection.sync();
    }

    /**
     * Derive a key that hashes to the same slot as {@link #key} by using {@link #key} as tag.
     */
    private String tagged(String name) {
        return "{" + key + "}" + name;
    }

    // re-implementation because keys have to be on the same slot
    @Test
    @EnabledOnCommand("ZDIFF") // Redis 6.2
    void zdiff() {
        String zset1 = tagged("zset1");
        String zset2 = tagged("zset2");

        assertThat(redis.zadd(zset1, 1.0, "one")).isEqualTo(1);
        assertThat(redis.zadd(zset1, 2.0, "two")).isEqualTo(1);
        assertThat(redis.zadd(zset1, 3.0, "three")).isEqualTo(1);
        assertThat(redis.zadd(zset2, 1.0, "one")).isEqualTo(1);
        assertThat(redis.zadd(zset2, 2.0, "two")).isEqualTo(1);

        assertThat(redis.zdiff(zset1, zset2)).isEqualTo(list("three"));
        assertThat(redis.zdiffWithScores(zset1, zset2)).isEqualTo(svlist(sv(3.0, "three")));
    }

    @Test
    @EnabledOnCommand("ZDIFFSTORE") // Redis 6.2
    void zdiffstore() {
        String zset1 = tagged("zset1");
        String zset2 = tagged("zset2");
        String out = tagged("out");

        assertThat(redis.zadd(zset1, 1.0, "one")).isEqualTo(1);
        assertThat(redis.zadd(zset1, 2.0, "two")).isEqualTo(1);
        assertThat(redis.zadd(zset1, 3.0, "three")).isEqualTo(1);
        assertThat(redis.zadd(zset2, 1.0, "one")).isEqualTo(1);
        assertThat(redis.zadd(zset2, 2.0, "two")).isEqualTo(1);

        assertThat(redis.zdiffstore(out, zset1, zset2)).isEqualTo(1);
        assertThat(redis.zrangeWithScores(out, 0, -1)).isEqualTo(svlist(sv(3.0, "three")));
    }

    @Test
    @EnabledOnCommand("ZINTERCARD")
    void zintercard() {
        String zset1 = tagged("zset1");
        String zset2 = tagged("zset2");

        redis.zadd(zset1, 1.0, "a", 2.0, "b");
        redis.zadd(zset2, 2.0, "a", 1.0, "b");
        assertThat(redis.zintercard(zset1, zset2)).isEqualTo(2);
        assertThat(redis.zintercard(1, zset1, zset2)).isEqualTo(1);
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    void zinterstore() {
        String zset1 = tagged("zset1");
        String zset2 = tagged("zset2");

        redis.zadd(zset1, 1.0, "a", 2.0, "b");
        redis.zadd(zset2, 2.0, "a", 3.0, "b", 4.0, "c");
        assertThat(redis.zinterstore(key, zset1, zset2)).isEqualTo(2);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "b"));
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(3.0, "a"), sv(5.0, "b")));
    }

    @Test
    @EnabledOnCommand("BZMPOP")
    void bzmpop() {
        String zset = tagged("zset");
        String other = tagged("other");

        redis.zadd(zset, 2.0, "a1", 3.0, "b1");
        redis.zadd(other, 2.0, "a2", 3.0, "b2");

        assertThat(redis.bzmpop(1, ZPopArgs.Builder.min(), zset, other))
                .isEqualTo(KeyValue.just(zset, ScoredValue.just(2.0, "a1")));
        assertThat(redis.bzmpop(1, ZPopArgs.Builder.min(), zset)).isEqualTo(KeyValue.just(zset, ScoredValue.just(3.0, "b1")));
        assertThat(redis.bzmpop(0.01, ZPopArgs.Builder.min(), "does_not_exist")).isEqualTo(KeyValue.empty("does_not_exist"));
        assertThat(redis.bzmpop(0.5, ZPopArgs.Builder.max(), zset, other))
                .isEqualTo(KeyValue.just(other, ScoredValue.just(3.0, "b2")));
    }

    @Test
    @EnabledOnCommand("ZMPOP")
    void zmpop() {
        String zset = tagged("zset");
        String other = tagged("other");

        redis.zadd(zset, 2.0, "a1", 3.0, "b1");
        redis.zadd(other, 2.0, "a2", 3.0, "b2");

        assertThat(redis.zmpop(ZPopArgs.Builder.min(), zset, other))
                .isEqualTo(KeyValue.just(zset, ScoredValue.just(2.0, "a1")));
        assertThat(redis.zmpop(ZPopArgs.Builder.min(), zset)).isEqualTo(KeyValue.just(zset, ScoredValue.just(3.0, "b1")));
        assertThat(redis.zmpop(ZPopArgs.Builder.min(), "does_not_exist")).isEqualTo(KeyValue.empty("does_not_exist"));
        assertThat(redis.zmpop(ZPopArgs.Builder.max(), zset, other))
                .isEqualTo(KeyValue.just(other, ScoredValue.just(3.0, "b2")));
    }

    @Test
    @EnabledOnCommand("ZRANGESTORE") // Redis 6.2
    void zrangestore() {
        String key1 = tagged("key1");

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertThat(redis.zrangestore(key1, key, Range.create(2L, 3L))).isEqualTo(2);
        assertThat(redis.zrange(key1, 0, 2)).isEqualTo(list("c", "d"));
    }

    @Test
    @EnabledOnCommand("ZRANGESTORE") // Redis 6.2
    void zrangestorebylex() {
        String key1 = tagged("key1");

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertThat(redis.zrangestorebylex(key1, key, Range.create("a", "b"), Limit.create(0, 4))).isEqualTo(2);
        assertThat(redis.zrange(key1, 0, 2)).isEqualTo(list("a", "b"));
    }

    @Test
    @EnabledOnCommand("ZRANGESTORE") // Redis 6.2
    void zrangestorebyscore() {
        String key1 = tagged("key1");

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertThat(redis.zrangestorebyscore(key1, key, Range.create(0, 2), Limit.create(0, 2))).isEqualTo(2);
        assertThat(redis.zrange(key1, 0, 2)).isEqualTo(list("a", "b"));
    }

    @Test
    @EnabledOnCommand("ZRANGESTORE") // Redis 6.2
    void zrevrangestore() {
        String key1 = tagged("key1");

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertThat(redis.zrevrangestore(key1, key, Range.create(2L, 3L))).isEqualTo(2);
        assertThat(redis.zrange(key1, 0, 2)).isEqualTo(list("a", "b"));
    }

    @Test
    @EnabledOnCommand("ZRANGESTORE") // Redis 6.2
    void zrevrangestorebylex() {
        String key1 = tagged("key1");

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertThat(redis.zrevrangestorebylex(key1, key, Range.create("-", "c"), Limit.create(0, 4))).isEqualTo(3);
        assertThat(redis.zrange(key1, 0, 2)).isEqualTo(list("a", "b", "c"));
    }

    @Test
    @EnabledOnCommand("ZRANGESTORE") // Redis 6.2
    void zrevrangestorebyscore() {
        String key1 = tagged("key1");

        redis.zadd(key, 1.0, "a", 2.0, "b", 3.0, "c", 4.0, "d");
        assertThat(redis.zrevrangestorebyscore(key1, key, Range.from(Boundary.excluding(1), Boundary.including(2)),
                Limit.create(0, 2))).isEqualTo(1);
        assertThat(redis.zrange(key1, 0, 2)).isEqualTo(list("b"));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    void zunionstore() {
        String zset1 = tagged("zset1");
        String zset2 = tagged("zset2");

        redis.zadd(zset1, 1.0, "a", 2.0, "b");
        redis.zadd(zset2, 2.0, "a", 3.0, "b", 4.0, "c");
        assertThat(redis.zunionstore(key, zset1, zset2)).isEqualTo(3);
        assertThat(redis.zrange(key, 0, -1)).isEqualTo(list("a", "c", "b"));
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(3.0, "a"), sv(4.0, "c"), sv(5.0, "b")));

        assertThat(redis.zunionstore(key, ZStoreArgs.Builder.weights(new long[] { 2, 3 }), zset1, zset2)).isEqualTo(3);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(8.0, "a"), sv(12.0, "c"), sv(13.0, "b")));

        assertThat(redis.zunionstore(key, ZStoreArgs.Builder.weights(2, 3).sum(), zset1, zset2)).isEqualTo(3);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(8.0, "a"), sv(12.0, "c"), sv(13.0, "b")));

        assertThat(redis.zunionstore(key, ZStoreArgs.Builder.weights(2, 3).min(), zset1, zset2)).isEqualTo(3);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(2.0, "a"), sv(4.0, "b"), sv(12.0, "c")));

        assertThat(redis.zunionstore(key, ZStoreArgs.Builder.weights(2, 3).max(), zset1, zset2)).isEqualTo(3);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(6.0, "a"), sv(9.0, "b"), sv(12.0, "c")));
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    void zStoreArgs() {
        String zset1 = tagged("zset1");
        String zset2 = tagged("zset2");

        redis.zadd(zset1, 1.0, "a", 2.0, "b");
        redis.zadd(zset2, 2.0, "a", 3.0, "b", 4.0, "c");

        assertThat(redis.zinterstore(key, ZStoreArgs.Builder.sum(), zset1, zset2)).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(3.0, "a"), sv(5.0, "b")));

        assertThat(redis.zinterstore(key, ZStoreArgs.Builder.min(), zset1, zset2)).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(1.0, "a"), sv(2.0, "b")));

        assertThat(redis.zinterstore(key, ZStoreArgs.Builder.max(), zset1, zset2)).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(2.0, "a"), sv(3.0, "b")));

        assertThat(redis.zinterstore(key, ZStoreArgs.Builder.weights(new long[] { 2, 3 }), zset1, zset2)).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(8.0, "a"), sv(13.0, "b")));

        assertThat(redis.zinterstore(key, ZStoreArgs.Builder.weights(2, 3).sum(), zset1, zset2)).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(8.0, "a"), sv(13.0, "b")));

        assertThat(redis.zinterstore(key, ZStoreArgs.Builder.weights(2, 3).min(), zset1, zset2)).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(2.0, "a"), sv(4.0, "b")));

        assertThat(redis.zinterstore(key, ZStoreArgs.Builder.weights(2, 3).max(), zset1, zset2)).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(6.0, "a"), sv(9.0, "b")));
    }

    @Test
    @EnabledOnCommand("ZUNION") // Redis 6.2
    void zunion() {
        String zset1 = tagged("zset1");
        String zset2 = tagged("zset2");

        assertThat(redis.zadd(zset1, 1.0, "a")).isEqualTo(1);
        assertThat(redis.zadd(zset1, 2.0, "b")).isEqualTo(1);
        assertThat(redis.zadd(zset2, 1.0, "a")).isEqualTo(1);
        assertThat(redis.zadd(zset2, 2.0, "b")).isEqualTo(1);
        assertThat(redis.zadd(zset2, 3.0, "c")).isEqualTo(1);

        assertThat(redis.zunion(zset1, zset2)).isEqualTo(list("a", "c", "b"));
        assertThat(redis.zunionWithScores(zset1, zset2)).isEqualTo(svlist(sv(2.0, "a"), sv(3.0, "c"), sv(4.0, "b")));
    }

    @Test
    @EnabledOnCommand("ZINTER") // Redis 6.2
    void zinter() {
        String zset1 = tagged("zset1");
        String zset2 = tagged("zset2");

        assertThat(redis.zadd(zset1, 1.0, "a")).isEqualTo(1);
        assertThat(redis.zadd(zset1, 2.0, "b")).isEqualTo(1);
        assertThat(redis.zadd(zset2, 1.0, "a")).isEqualTo(1);
        assertThat(redis.zadd(zset2, 2.0, "b")).isEqualTo(1);
        assertThat(redis.zadd(zset2, 3.0, "c")).isEqualTo(1);

        assertThat(redis.zinter(zset1, zset2)).isEqualTo(list("a", "b"));
        assertThat(redis.zinterWithScores(zset1, zset2)).isEqualTo(svlist(sv(2.0, "a"), sv(4.0, "b")));
    }

    @Test
    void zStoreArgsAggregateCount() {

        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.7.225"), "AGGREGATE COUNT requires Redis 8.8");

        String s1 = tagged("s1");
        String s2 = tagged("s2");
        String s3 = tagged("s3");

        redis.zadd(s1, 1.0, "foo", 1.0, "bar");
        redis.zadd(s2, 2.0, "foo", 2.0, "bar");
        redis.zadd(s3, 3.0, "foo");

        assertThat(redis.zunionstore(key, ZStoreArgs.Builder.count(), s1, s2, s3)).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(2.0, "bar"), sv(3.0, "foo")));

        assertThat(redis.zinterstore(key, ZStoreArgs.Builder.count(), s1, s2, s3)).isEqualTo(1);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(3.0, "foo")));

        assertThat(redis.zunionstore(key, ZStoreArgs.Builder.weights(10, 5, 3).count(), s1, s2, s3)).isEqualTo(2);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(15.0, "bar"), sv(18.0, "foo")));

        assertThat(redis.zinterstore(key, ZStoreArgs.Builder.weights(10, 5, 3).count(), s1, s2, s3)).isEqualTo(1);
        assertThat(redis.zrangeWithScores(key, 0, -1)).isEqualTo(svlist(sv(18.0, "foo")));
    }

    @Test
    void zunionAggregateCount() {

        assumeTrue(RedisConditions.of(redis).hasVersionGreaterOrEqualsTo("8.7.225"), "AGGREGATE COUNT requires Redis 8.8");

        String s1 = tagged("s1");
        String s2 = tagged("s2");
        String s3 = tagged("s3");

        redis.zadd(s1, 1.0, "foo", 1.0, "bar");
        redis.zadd(s2, 2.0, "foo", 2.0, "bar");
        redis.zadd(s3, 3.0, "foo");

        assertThat(redis.zunionWithScores(ZStoreArgs.Builder.count(), s1, s2, s3))
                .isEqualTo(svlist(sv(2.0, "bar"), sv(3.0, "foo")));
        assertThat(redis.zinterWithScores(ZStoreArgs.Builder.count(), s1, s2, s3)).isEqualTo(svlist(sv(3.0, "foo")));
    }

}
