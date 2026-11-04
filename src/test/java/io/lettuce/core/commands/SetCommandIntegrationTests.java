/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.commands;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.ListStreamingAdapter;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.condition.RedisConditions;

/**
 * @author Will Glozer
 * @author Mark Paluch
 * @author dengliming
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SetCommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    protected SetCommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void sadd() {
        assertThat(redis.sadd(key, "a")).isEqualTo(1L);
        assertThat(redis.sadd(key, "a")).isEqualTo(0);
        assertThat(redis.smembers(key)).isEqualTo(set("a"));
        assertThat(redis.sadd(key, "b", "c")).isEqualTo(2);
        assertThat(redis.smembers(key)).isEqualTo(set("a", "b", "c"));
    }

    @Test
    void scard() {
        assertThat((long) redis.scard(key)).isEqualTo(0);
        redis.sadd(key, "a");
        assertThat((long) redis.scard(key)).isEqualTo(1);
    }

    @Test
    public void sdiff() {
        setupSet();
        assertThat(redis.sdiff("key1", "key2", "key3")).isEqualTo(set("b", "d"));
    }

    @Test
    public void sdiffStreaming() {
        setupSet();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();

        Long count = redis.sdiff(streamingAdapter, "key1", "key2", "key3");
        assertThat(count.intValue()).isEqualTo(2);
        assertThat(streamingAdapter.getList()).containsOnly("b", "d");
    }

    @Test
    @EnabledOnCommand("SDIFFCARD") // Redis 8.10
    public void sdiffcard() {
        setupSet();
        assertThat(redis.sdiffcard("key1", "key3")).isEqualTo(2);
        assertThat(redis.sdiffcard(list("key1", "key2", "key3"))).isEqualTo(2);
        assertThat(redis.sdiffcard("missing", "key1")).isEqualTo(0);
        assertThat(redis.sdiffcard("key1", "missing")).isEqualTo(4);
    }

    @Test
    @EnabledOnCommand("SDIFFCARD") // Redis 8.10
    public void sdiffcardWithLimit() {
        setupSet();
        assertThat(redis.sdiffcard("key1", "key3", SDiffCardArgs.Builder.limit(1))).isEqualTo(1);
        assertThat(redis.sdiffcard("key1", "key3", SDiffCardArgs.Builder.limit(100))).isEqualTo(2);
        assertThat(redis.sdiffcard("key1", "key3", SDiffCardArgs.Builder.limit(0))).isEqualTo(2);
    }

    @Test
    public void sdiffstore() {
        setupSet();
        assertThat(redis.sdiffstore("newset", "key1", "key2", "key3")).isEqualTo(2);
        assertThat(redis.smembers("newset")).containsOnly("b", "d");
    }

    @Test
    public void sinter() {
        setupSet();
        assertThat(redis.sinter("key1", "key2", "key3")).isEqualTo(set("c"));
    }

    @Test
    @EnabledOnCommand("SINTERCARD") // Redis 7.0
    public void sintercard() {
        setupSet();
        assertThat(redis.sintercard("key1", "key3")).isEqualTo(2);
        assertThat(redis.sintercard(1, "key1", "key3")).isEqualTo(1);
    }

    @Test
    public void sinterStreaming() {
        setupSet();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();
        Long count = redis.sinter(streamingAdapter, "key1", "key2", "key3");

        assertThat(count.intValue()).isEqualTo(1);
        assertThat(streamingAdapter.getList()).containsExactly("c");
    }

    @Test
    public void sinterstore() {
        setupSet();
        assertThat(redis.sinterstore("newset", "key1", "key2", "key3")).isEqualTo(1);
        assertThat(redis.smembers("newset")).containsExactly("c");
    }

    @Test
    void sismember() {
        assertThat(redis.sismember(key, "a")).isFalse();
        redis.sadd(key, "a");
        assertThat(redis.sismember(key, "a")).isTrue();
    }

    @Test
    public void smove() {
        redis.sadd(key, "a", "b", "c");
        assertThat(redis.smove(key, "key1", "d")).isFalse();
        assertThat(redis.smove(key, "key1", "a")).isTrue();
        assertThat(redis.smembers(key)).isEqualTo(set("b", "c"));
        assertThat(redis.smembers("key1")).isEqualTo(set("a"));
    }

    @Test
    void smembers() {
        setupSet();
        assertThat(redis.smembers(key)).isEqualTo(set("a", "b", "c"));
    }

    @Test
    void smembersStreaming() {
        setupSet();
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();
        Long count = redis.smembers(streamingAdapter, key);
        assertThat(count.longValue()).isEqualTo(3);
        assertThat(streamingAdapter.getList()).containsOnly("a", "b", "c");
    }

    @Test
    @EnabledOnCommand("SMISMEMBER")
    void smismember() {
        assertThat(redis.smismember(key, "a")).isEqualTo(list(false));
        redis.sadd(key, "a");
        assertThat(redis.smismember(key, "a")).isEqualTo(list(true));
        assertThat(redis.smismember(key, "b", "a")).isEqualTo(list(false, true));
    }

    @Test
    void spop() {
        assertThat(redis.spop(key)).isNull();
        redis.sadd(key, "a", "b", "c");
        String rand = redis.spop(key);
        assertThat(set("a", "b", "c").contains(rand)).isTrue();
        assertThat(redis.smembers(key).contains(rand)).isFalse();
    }

    @Test
    void spopMultiple() {

        assumeTrue(RedisConditions.of(redis).hasCommandArity("SPOP", -2));

        assertThat(redis.spop(key)).isNull();
        redis.sadd(key, "a", "b", "c");
        Set<String> rand = redis.spop(key, 2);
        assertThat(rand).hasSize(2);
        assertThat(set("a", "b", "c").containsAll(rand)).isTrue();
    }

    @Test
    void srandmember() {
        assertThat(redis.spop(key)).isNull();
        redis.sadd(key, "a", "b", "c", "d");
        assertThat(set("a", "b", "c", "d").contains(redis.srandmember(key))).isTrue();
        assertThat(redis.smembers(key)).isEqualTo(set("a", "b", "c", "d"));
        List<String> rand = redis.srandmember(key, 3);
        assertThat(rand).hasSize(3);
        assertThat(set("a", "b", "c", "d").containsAll(rand)).isTrue();
        List<String> randWithDuplicates = redis.srandmember(key, -10);
        assertThat(randWithDuplicates).hasSize(10);
    }

    @Test
    void srandmemberStreaming() {
        assertThat(redis.spop(key)).isNull();
        redis.sadd(key, "a", "b", "c", "d");

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();

        Long count = redis.srandmember(streamingAdapter, key, 2);

        assertThat(count.longValue()).isEqualTo(2);

        assertThat(set("a", "b", "c", "d").containsAll(streamingAdapter.getList())).isTrue();

    }

    @Test
    void srem() {
        redis.sadd(key, "a", "b", "c");
        assertThat(redis.srem(key, "d")).isEqualTo(0);
        assertThat(redis.srem(key, "b")).isEqualTo(1);
        assertThat(redis.smembers(key)).isEqualTo(set("a", "c"));
        assertThat(redis.srem(key, "a", "c")).isEqualTo(2);
        assertThat(redis.smembers(key)).isEqualTo(set());
    }

    @Test
    void sremEmpty() {
        assertThatThrownBy(() -> redis.srem(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sremNulls() {
        assertThatThrownBy(() -> redis.srem(key, new String[0])).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void sunion() {
        setupSet();
        assertThat(redis.sunion("key1", "key2", "key3")).isEqualTo(set("a", "b", "c", "d", "e"));
    }

    @Test
    void sunionEmpty() {
        assertThatThrownBy(() -> redis.sunion()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void sunionStreaming() {
        setupSet();

        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        Long count = redis.sunion(adapter, "key1", "key2", "key3");

        assertThat(count.longValue()).isEqualTo(5);

        assertThat(new TreeSet<>(adapter.getList())).isEqualTo(new TreeSet<>(list("c", "a", "b", "e", "d")));
    }

    @Test
    @EnabledOnCommand("SUNIONCARD") // Redis 8.10
    public void sunioncard() {
        setupSet();
        assertThat(redis.sunioncard("key1", "key3")).isEqualTo(5);
        assertThat(redis.sunioncard(list("key1", "key2", "key3"))).isEqualTo(5);
        assertThat(redis.sunioncard("key1", "missing")).isEqualTo(4);
    }

    @Test
    @EnabledOnCommand("SUNIONCARD") // Redis 8.10
    public void sunioncardApprox() {
        setupSet();
        assertThat(redis.sunioncard("key1", "key3", SUnionCardArgs.Builder.approx())).isEqualTo(5);
        assertThat(redis.sunioncard("key1", "key3", SUnionCardArgs.Builder.approx().limit(3))).isLessThanOrEqualTo(3);
    }

    @Test
    @EnabledOnCommand("SUNIONCARD") // Redis 8.10
    public void sunioncardWithLimit() {
        setupSet();
        assertThat(redis.sunioncard("key1", "key3", SUnionCardArgs.Builder.limit(3))).isEqualTo(3);
        assertThat(redis.sunioncard("key1", "key3", SUnionCardArgs.Builder.limit(100))).isEqualTo(5);
        assertThat(redis.sunioncard("key1", "key3", SUnionCardArgs.Builder.limit(0))).isEqualTo(5);
    }

    @Test
    public void sunionstore() {
        setupSet();
        assertThat(redis.sunionstore("newset", "key1", "key2", "key3")).isEqualTo(5);
        assertThat(redis.smembers("newset")).isEqualTo(set("a", "b", "c", "d", "e"));
    }

    @Test
    void sscan() {
        redis.sadd(key, value);
        ValueScanCursor<String> cursor = redis.sscan(key);

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getValues()).isEqualTo(list(value));
    }

    @Test
    void sscanWithCursor() {
        redis.sadd(key, value);
        ValueScanCursor<String> cursor = redis.sscan(key, ScanCursor.INITIAL);

        assertThat(cursor.getValues()).hasSize(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    void sscanWithCursorAndArgs() {
        redis.sadd(key, value);

        ValueScanCursor<String> cursor = redis.sscan(key, ScanCursor.INITIAL, ScanArgs.Builder.limit(5));

        assertThat(cursor.getValues()).hasSize(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

    }

    @Test
    void sscanStreaming() {
        redis.sadd(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.sscan(adapter, key);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getList()).isEqualTo(list(value));
    }

    @Test
    void sscanStreamingWithCursor() {
        redis.sadd(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.sscan(adapter, key, ScanCursor.INITIAL);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    void sscanStreamingWithCursorAndArgs() {
        redis.sadd(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.sscan(adapter, key, ScanCursor.INITIAL, ScanArgs.Builder.limit(5));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    void sscanStreamingArgs() {
        redis.sadd(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        StreamScanCursor cursor = redis.sscan(adapter, key, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getList()).isEqualTo(list(value));
    }

    @Test
    void sscanMultiple() {

        Set<String> expect = new HashSet<>();
        Set<String> check = new HashSet<>();
        setup129KeyValues(expect);

        ValueScanCursor<String> cursor = redis.sscan(key, ScanArgs.Builder.limit(5));

        assertThat(cursor.getCursor()).isNotNull().isNotEqualTo("0");
        assertThat(cursor.isFinished()).isFalse();

        check.addAll(cursor.getValues());

        while (!cursor.isFinished()) {
            cursor = redis.sscan(key, cursor);
            check.addAll(cursor.getValues());
        }

        assertThat(new TreeSet<>(check)).isEqualTo(new TreeSet<>(expect));
    }

    @Test
    void scanMatch() {

        Set<String> expect = new HashSet<>();
        setup129KeyValues(expect);

        ValueScanCursor<String> cursor = redis.sscan(key, ScanArgs.Builder.limit(200).match("value1*"));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

        assertThat(cursor.getValues()).hasSize(40);
    }

    void setup129KeyValues(Set<String> expect) {
        // Redis 7.0 introduce listpack, and `set-max-listpack-entries` is 128
        // so we add 129 elements to convert it to hashtable
        for (int i = 0; i < 129; i++) {
            redis.sadd(key, value + i);
            expect.add(value + i);
        }
    }

    private void setupSet() {
        redis.sadd(key, "a", "b", "c");
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
    }

    // Set member expiration (Redis 8.12)

    @Test
    @EnabledOnCommand("SEXPIRE")
    void sexpire() {
        redis.sadd(key, "a", "b");
        assertThat(redis.sexpire(key, 60, "a", "nosuch")).containsExactly(1L, -2L);
        assertThat(redis.sexpire("nokey", 60, "a")).containsExactly(-2L);
        assertThat(redis.sexpire(key, 0, "b")).containsExactly(2L);
        assertThat(redis.sismember(key, "b")).isFalse();
    }

    @Test
    @EnabledOnCommand("SEXPIRE")
    void sexpireDuration() {
        redis.sadd(key, "a");
        assertThat(redis.sexpire(key, Duration.ofSeconds(60), "a")).containsExactly(1L);
        assertThat(redis.sttl(key, "a").get(0)).isBetween(55L, 60L);
    }

    @Test
    @EnabledOnCommand("SEXPIRE")
    void sexpireExpireArgs() {
        assertThat(redis.saddex(key, SAddExArgs.Builder.ex(Duration.ofSeconds(100)), "a")).isEqualTo(1L);
        redis.sadd(key, "b");
        assertThat(redis.sexpire(key, 50, ExpireArgs.Builder.nx(), "a", "b", "nosuch")).containsExactly(0L, 1L, -2L);
        assertThat(redis.sexpire(key, 50, ExpireArgs.Builder.xx(), "a", "b")).containsExactly(1L, 1L);
        assertThat(redis.sexpire(key, 10, ExpireArgs.Builder.gt(), "a")).containsExactly(0L);
        assertThat(redis.sexpire(key, Duration.ofSeconds(10), ExpireArgs.Builder.lt(), "a")).containsExactly(1L);
        assertThat(redis.sttl(key, "a", "b")).containsExactly(10L, 50L);
        assertThat(redis.spersist(key, "b")).containsExactly(1L);
        assertThat(redis.sexpire(key, 10, ExpireArgs.Builder.xx(), "b")).containsExactly(0L);
        assertThat(redis.sexpire(key, 10, ExpireArgs.Builder.gt(), "b")).containsExactly(0L);
        assertThat(redis.sexpire(key, 10, ExpireArgs.Builder.lt(), "b")).containsExactly(1L);
    }

    @Test
    @EnabledOnCommand("SPEXPIRE")
    void spexpire() {
        redis.sadd(key, "a", "b");
        assertThat(redis.spexpire(key, 60_000, "a")).containsExactly(1L);
        assertThat(redis.spttl(key, "a").get(0)).isBetween(55_000L, 60_000L);
        assertThat(redis.spexpire(key, Duration.ofSeconds(120), ExpireArgs.Builder.gt(), "a", "b")).containsExactly(1L, 0L);
        assertThat(redis.spttl(key, "a").get(0)).isBetween(115_000L, 120_000L);
    }

    @Test
    @EnabledOnCommand("SEXPIREAT")
    void sexpireat() {
        redis.sadd(key, "a", "b", "c");
        assertThat(redis.sexpireat(key, Instant.now().plusSeconds(60), "a")).containsExactly(1L);
        assertThat(redis.sexpireat(key, new Date(System.currentTimeMillis() + 120_000), ExpireArgs.Builder.xx(), "a", "b"))
                .containsExactly(1L, 0L);
        assertThat(redis.sttl(key, "a").get(0)).isBetween(115L, 120L);
        assertThat(redis.sexpireat(key, 1, "b")).containsExactly(2L);
        assertThat(redis.sexpireat(key, Instant.now().minusSeconds(1), "c")).containsExactly(2L);
        assertThat(redis.smembers(key)).isEqualTo(set("a"));
        assertThat(redis.sexpireat("nokey", 1, "a")).containsExactly(-2L);
    }

    @Test
    @EnabledOnCommand("SPEXPIREAT")
    void spexpireat() {
        redis.sadd(key, "a", "b");
        long base = System.currentTimeMillis() + 60_000;
        assertThat(redis.spexpireat(key, base, "a")).containsExactly(1L);
        assertThat(redis.spexpiretime(key, "a")).containsExactly(base);
        assertThat(redis.spexpireat(key, Instant.ofEpochMilli(base + 1000), ExpireArgs.Builder.gt(), "a")).containsExactly(1L);
        assertThat(redis.spexpireat(key, new Date(base + 2000), ExpireArgs.Builder.lt(), "a")).containsExactly(0L);
        assertThat(redis.spexpiretime(key, "a")).containsExactly(base + 1000);
        assertThat(redis.spexpireat(key, 1L, "b")).containsExactly(2L);
    }

    @Test
    @EnabledOnCommand("STTL")
    void sttl() {
        redis.sadd(key, "a", "b");
        redis.sexpire(key, 60, "a");
        List<Long> ttl = redis.sttl(key, "a", "b", "nosuch");
        assertThat(ttl).hasSize(3);
        assertThat(ttl.get(0)).isBetween(55L, 60L);
        assertThat(ttl.get(1)).isEqualTo(-1L);
        assertThat(ttl.get(2)).isEqualTo(-2L);
        assertThat(redis.sttl("nokey", "a")).containsExactly(-2L);
    }

    @Test
    @EnabledOnCommand("SPTTL")
    void spttl() {
        redis.sadd(key, "a", "b");
        redis.spexpire(key, 60_000, "a");
        List<Long> pttl = redis.spttl(key, "a", "b", "nosuch");
        assertThat(pttl).hasSize(3);
        assertThat(pttl.get(0)).isBetween(55_000L, 60_000L);
        assertThat(pttl.get(1)).isEqualTo(-1L);
        assertThat(pttl.get(2)).isEqualTo(-2L);
    }

    @Test
    @EnabledOnCommand("SEXPIRETIME")
    void sexpiretime() {
        redis.sadd(key, "a", "b");
        long base = (System.currentTimeMillis() / 1000) * 1000 + 60_000;
        redis.spexpireat(key, base, "a");
        assertThat(redis.sexpiretime(key, "a", "b", "nosuch")).containsExactly(base / 1000, -1L, -2L);
        assertThat(redis.spexpiretime(key, "a", "b", "nosuch")).containsExactly(base, -1L, -2L);
        assertThat(redis.sexpiretime("nokey", "a")).containsExactly(-2L);
    }

    @Test
    @EnabledOnCommand("SPERSIST")
    void spersist() {
        redis.sadd(key, "a", "b");
        redis.sexpire(key, 60, "a");
        assertThat(redis.spersist(key, "a", "b", "nosuch")).containsExactly(1L, -1L, -2L);
        assertThat(redis.sttl(key, "a")).containsExactly(-1L);
        assertThat(redis.spersist("nokey", "a")).containsExactly(-2L);
    }

    @Test
    @EnabledOnCommand("SADDEX")
    void saddex() {
        assertThat(redis.saddex(key, "a", "b")).isEqualTo(1L);
        assertThat(redis.smembers(key)).isEqualTo(set("a", "b"));
        assertThat(redis.sttl(key, "a", "b")).containsExactly(-1L, -1L);
        assertThat(redis.saddex(key, "a", "b")).isEqualTo(0L);
        assertThat(redis.saddex(key, "c")).isEqualTo(1L);
    }

    @Test
    @EnabledOnCommand("SADDEX")
    void saddexWithExpiration() {
        assertThat(redis.saddex(key, SAddExArgs.Builder.ex(Duration.ofSeconds(60)), "a")).isEqualTo(1L);
        assertThat(redis.sttl(key, "a").get(0)).isBetween(55L, 60L);
        redis.saddex(key, SAddExArgs.Builder.keepttl(), "a");
        assertThat(redis.sttl(key, "a").get(0)).isBetween(55L, 60L);
        assertThat(redis.saddex(key, "a")).isEqualTo(1L);
        assertThat(redis.sttl(key, "a")).containsExactly(-1L);
        assertThat(redis.saddex(key, SAddExArgs.Builder.px(Duration.ofSeconds(60)), "a")).isEqualTo(1L);
        assertThat(redis.spttl(key, "a").get(0)).isBetween(55_000L, 60_000L);
        long base = (System.currentTimeMillis() / 1000) * 1000 + 120_000;
        assertThat(redis.saddex(key, SAddExArgs.Builder.exAt(Instant.ofEpochMilli(base)), "a")).isEqualTo(1L);
        assertThat(redis.sexpiretime(key, "a")).containsExactly(base / 1000);
        assertThat(redis.saddex(key, SAddExArgs.Builder.pxAt(Instant.ofEpochMilli(base + 500)), "a")).isEqualTo(1L);
        assertThat(redis.spexpiretime(key, "a")).containsExactly(base + 500);
    }

    @Test
    @EnabledOnCommand("SADDEX")
    void saddexConditions() {
        redis.sadd(key, "a");
        assertThat(redis.saddex(key, SAddExArgs.Builder.mnx(), "a", "b")).isEqualTo(0L);
        assertThat(redis.smembers(key)).isEqualTo(set("a"));
        assertThat(redis.saddex(key, SAddExArgs.Builder.mnx().ex(Duration.ofSeconds(60)), "b", "c")).isEqualTo(1L);
        assertThat(redis.smembers(key)).isEqualTo(set("a", "b", "c"));
        assertThat(redis.saddex(key, SAddExArgs.Builder.mxx(), "a", "nosuch")).isEqualTo(0L);
        assertThat(redis.saddex(key, SAddExArgs.Builder.mxx().ex(Duration.ofSeconds(120)), "a", "b")).isEqualTo(1L);
        assertThat(redis.sttl(key, "a").get(0)).isBetween(115L, 120L);
        assertThat(redis.saddex("nokey", SAddExArgs.Builder.mxx(), "a")).isEqualTo(0L);
        assertThat(redis.exists("nokey")).isEqualTo(0L);
    }

    @Test
    @EnabledOnCommand("SADDEX")
    void saddexPastExpirationDeletes() {
        redis.sadd(key, "a");
        assertThat(redis.saddex(key, SAddExArgs.Builder.pxAt(Instant.ofEpochMilli(1)), "a", "new")).isEqualTo(1L);
        assertThat(redis.smembers(key)).isEmpty();
        assertThat(redis.saddex(key, SAddExArgs.Builder.pxAt(Instant.ofEpochMilli(1)), "a", "new")).isEqualTo(0L);
    }

    @Test
    @EnabledOnCommand("SADDEX")
    void saddexConflictingOptions() {
        assertThatThrownBy(() -> redis.saddex(key, SAddExArgs.Builder.ex(Duration.ofSeconds(1)).px(Duration.ofSeconds(1)), "a"))
                .isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageStartingWith("ERR Only one of EX, PX, EXAT, PXAT or KEEPTTL");
        assertThatThrownBy(() -> redis.saddex(key, SAddExArgs.Builder.mnx().mxx(), "a"))
                .isInstanceOf(RedisCommandExecutionException.class).hasMessageStartingWith("ERR Only one of MXX or MNX");
    }

    @Test
    @EnabledOnCommand("SEXPIRE")
    void sexpireWrongType() {
        redis.set(key, value);
        assertThatThrownBy(() -> redis.sexpire(key, 60, "a")).isInstanceOf(RedisCommandExecutionException.class)
                .hasMessageContaining("WRONGTYPE");
    }

    @Test
    @EnabledOnCommand("SPEXPIRE")
    void sexpireLogicallyExpiredMemberIsAbsent() {
        redis.sadd(key, "a", "b");
        assertThat(redis.spexpire(key, 1, "a")).containsExactly(1L);
        await().until(() -> redis.sttl(key, "a").equals(list(-2L)));
        assertThat(redis.sismember(key, "a")).isFalse();
        assertThat(redis.spersist(key, "a")).containsExactly(-2L);
        assertThat(redis.smembers(key)).isEqualTo(set("b"));
    }

    @Test
    void sexpireEmptyMembers() {
        assertThatThrownBy(() -> redis.sexpire(key, 60)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.sexpire(key, 60, (ExpireArgs) null, "a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.sttl(key)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.saddex(key)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> redis.saddex(key, (SAddExArgs) null, "a")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @EnabledOnCommand("SEXPIRE")
    void sexpireSingleMember() {
        redis.sadd(key, "a", "z");
        assertThat(redis.sexpire(key, 60, "a")).containsExactly(1L);
        assertThat(redis.sexpire(key, Duration.ofSeconds(120), ExpireArgs.Builder.gt(), "a")).containsExactly(1L);
        assertThat(redis.sttl(key, "a").get(0)).isBetween(115L, 120L);
        assertThat(redis.spexpireat(key, Instant.now().plusSeconds(300), "a")).containsExactly(1L);
        assertThat(redis.spttl(key, "a").get(0)).isBetween(295_000L, 300_000L);
        assertThat(redis.spersist(key, "a")).containsExactly(1L);
        assertThat(redis.saddex(key, SAddExArgs.Builder.ex(Duration.ofSeconds(60)), "z")).isEqualTo(1L);
        assertThat(redis.sexpiretime(key, "z")).hasSize(1);
        assertThat(redis.saddex(key, "y")).isEqualTo(1L);
    }

}
