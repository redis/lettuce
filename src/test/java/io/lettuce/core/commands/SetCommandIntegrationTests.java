/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
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
    void sdiff() {
        setupSet();
        assertThat(redis.sdiff("key1", "key2", "key3")).isEqualTo(set("b", "d"));
    }

    @Test
    void sdiffStreaming() {
        setupSet();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();

        Long count = redis.sdiff(streamingAdapter, "key1", "key2", "key3");
        assertThat(count.intValue()).isEqualTo(2);
        assertThat(streamingAdapter.getList()).containsOnly("b", "d");
    }

    @Test
    void sdiffstore() {
        setupSet();
        assertThat(redis.sdiffstore("newset", "key1", "key2", "key3")).isEqualTo(2);
        assertThat(redis.smembers("newset")).containsOnly("b", "d");
    }

    @Test
    void sinter() {
        setupSet();
        assertThat(redis.sinter("key1", "key2", "key3")).isEqualTo(set("c"));
    }

    @Test
    void sinterStreaming() {
        setupSet();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();
        Long count = redis.sinter(streamingAdapter, "key1", "key2", "key3");

        assertThat(count.intValue()).isEqualTo(1);
        assertThat(streamingAdapter.getList()).containsExactly("c");
    }

    @Test
    void sinterstore() {
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
    void smove() {
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
        assertThatThrownBy(() -> redis.srem(key, new String[0])).isInstanceOf(IllegalArgumentException. class);
    }

    @Test
    void sunion() {
        setupSet();
        assertThat(redis.sunion("key1", "key2", "key3")).isEqualTo(set("a", "b", "c", "d", "e"));
    }

    @Test
    void sunionEmpty() {
        assertThatThrownBy(() -> redis.sunion()).isInstanceOf(IllegalArgumentException. class);
    }

    @Test
    void sunionStreaming() {
        setupSet();

        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<>();

        Long count = redis.sunion(adapter, "key1", "key2", "key3");

        assertThat(count.longValue()).isEqualTo(5);

        assertThat(new TreeSet<>(adapter.getList())).isEqualTo(new TreeSet<>(list("c", "a", "b", "e", "d")));
    }

    @Test
    void sunionstore() {
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
        setup100KeyValues(expect);

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
        setup100KeyValues(expect);

        ValueScanCursor<String> cursor = redis.sscan(key, ScanArgs.Builder.limit(200).match("value1*"));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

        assertThat(cursor.getValues()).hasSize(11);
    }

    void setup100KeyValues(Set<String> expect) {
        for (int i = 0; i < 100; i++) {
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

}
