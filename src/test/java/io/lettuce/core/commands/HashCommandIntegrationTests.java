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
import static org.assertj.core.api.Assertions.offset;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.test.KeyValueStreamingAdapter;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.ListStreamingAdapter;
import io.lettuce.test.condition.EnabledOnCommand;

/**
 * Integration tests for {@link io.lettuce.core.api.sync.RedisHashCommands}.
 *
 * @author Will Glozer
 * @author Mark Paluch
 * @author Hodur Heidarsson
 */
@ExtendWith(LettuceExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HashCommandIntegrationTests extends TestSupport {

    private final RedisCommands<String, String> redis;

    @Inject
    protected HashCommandIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }

    @BeforeEach
    void setUp() {
        this.redis.flushall();
    }

    @Test
    void hdel() {
        assertThat(redis.hdel(key, "one")).isEqualTo(0);
        redis.hset(key, "two", "2");
        assertThat(redis.hdel(key, "one")).isEqualTo(0);
        redis.hset(key, "one", "1");
        assertThat(redis.hdel(key, "one")).isEqualTo(1);
        redis.hset(key, "one", "1");
        assertThat(redis.hdel(key, "one", "two")).isEqualTo(2);
    }

    @Test
    void hexists() {
        assertThat(redis.hexists(key, "one")).isFalse();
        redis.hset(key, "two", "2");
        assertThat(redis.hexists(key, "one")).isFalse();
        redis.hset(key, "one", "1");
        assertThat(redis.hexists(key, "one")).isTrue();
    }

    @Test
    void hget() {
        assertThat(redis.hget(key, "one")).isNull();
        redis.hset(key, "one", "1");
        assertThat(redis.hget(key, "one")).isEqualTo("1");
    }

    @Test
    public void hgetall() {
        assertThat(redis.hgetall(key).isEmpty()).isTrue();

        redis.hset(key, "zero", "0");
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");

        Map<String, String> map = redis.hgetall(key);

        assertThat(map).hasSize(3);
        assertThat(map.keySet()).containsExactly("zero", "one", "two");
    }

    @Test
    public void hgetallStreaming() {

        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<>();

        assertThat(redis.hgetall(key).isEmpty()).isTrue();
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
        Long count = redis.hgetall(adapter, key);
        Map<String, String> map = adapter.getMap();
        assertThat(count.intValue()).isEqualTo(2);
        assertThat(map).hasSize(2);
        assertThat(map.get("one")).isEqualTo("1");
        assertThat(map.get("two")).isEqualTo("2");
    }

    @Test
    void hincrby() {
        assertThat(redis.hincrby(key, "one", 1)).isEqualTo(1);
        assertThat(redis.hincrby(key, "one", -2)).isEqualTo(-1);
    }

    @Test
    void hincrbyfloat() {
        assertThat(redis.hincrbyfloat(key, "one", 1.0)).isEqualTo(1.0);
        assertThat(redis.hincrbyfloat(key, "one", -2.0)).isEqualTo(-1.0);
        assertThat(redis.hincrbyfloat(key, "one", 1.23)).isEqualTo(0.23, offset(0.001));
    }

    @Test
    void hkeys() {
        setup();
        List<String> keys = redis.hkeys(key);
        assertThat(keys).hasSize(2);
        assertThat(keys.containsAll(list("one", "two"))).isTrue();
    }

    @Test
    void hkeysStreaming() {
        setup();
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<>();

        Long count = redis.hkeys(streamingAdapter, key);
        assertThat(count.longValue()).isEqualTo(2);

        List<String> keys = streamingAdapter.getList();
        assertThat(keys).hasSize(2);
        assertThat(keys.containsAll(list("one", "two"))).isTrue();
    }

    private void setup() {
        assertThat(redis.hkeys(key)).isEqualTo(list());
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
    }

    @Test
    void hlen() {
        assertThat((long) redis.hlen(key)).isEqualTo(0);
        redis.hset(key, "one", "1");
        assertThat((long) redis.hlen(key)).isEqualTo(1);
    }

    @Test
    @EnabledOnCommand("HSTRLEN")
    void hstrlen() {

        assertThat((long) redis.hstrlen(key, "one")).isEqualTo(0);
        redis.hset(key, "one", value);
        assertThat((long) redis.hstrlen(key, "one")).isEqualTo(value.length());
    }

    @Test
    void hmget() {
        setupHmget();
        List<KeyValue<String, String>> values = redis.hmget(key, "one", "two");
        assertThat(values).hasSize(2);
        assertThat(values.containsAll(list(kv("one", "1"), kv("two", "2")))).isTrue();
    }

    private void setupHmget() {
        assertThat(redis.hmget(key, "one", "two")).isEqualTo(list(KeyValue.empty("one"), KeyValue.empty("two")));
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
    }

    @Test
    void hmgetStreaming() {
        setupHmget();

        KeyValueStreamingAdapter<String, String> streamingAdapter = new KeyValueStreamingAdapter<>();
        Long count = redis.hmget(streamingAdapter, key, "one", "two");
        Map<String, String> values = streamingAdapter.getMap();
        assertThat(count.intValue()).isEqualTo(2);
        assertThat(values).hasSize(2);
        assertThat(values).containsEntry("one", "1").containsEntry("two", "2");
    }

    @Test
    void hmset() {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("one", "1");
        hash.put("two", "2");
        assertThat(redis.hmset(key, hash)).isEqualTo("OK");
        assertThat(redis.hmget(key, "one", "two")).isEqualTo(list(kv("one", "1"), kv("two", "2")));
    }

    @Test
    void hmsetWithNulls() {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("one", null);
        assertThat(redis.hmset(key, hash)).isEqualTo("OK");
        assertThat(redis.hmget(key, "one")).isEqualTo(list(kv("one", "")));

        hash.put("one", "");
        assertThat(redis.hmset(key, hash)).isEqualTo("OK");
        assertThat(redis.hmget(key, "one")).isEqualTo(list(kv("one", "")));
    }

    @Test
    void hset() {
        assertThat(redis.hset(key, "one", "1")).isTrue();
        assertThat(redis.hset(key, "one", "1")).isFalse();
    }

    @Test
    @EnabledOnCommand("UNLINK") // version guard for Redis 4
    void hsetMap() {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("two", "2");
        hash.put("three", "3");
        assertThat(redis.hset(key, hash)).isEqualTo(2);

        hash.put("two", "second");
        assertThat(redis.hset(key, hash)).isEqualTo(0);
        assertThat(redis.hget(key, "two")).isEqualTo("second");
    }

    @Test
    void hsetnx() {
        redis.hset(key, "one", "1");
        assertThat(redis.hsetnx(key, "one", "2")).isFalse();
        assertThat(redis.hget(key, "one")).isEqualTo("1");
    }

    @Test
    void hvals() {
        assertThat(redis.hvals(key)).isEqualTo(list());
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
        List<String> values = redis.hvals(key);
        assertThat(values).hasSize(2);
        assertThat(values.containsAll(list("1", "1"))).isTrue();
    }

    @Test
    void hvalsStreaming() {
        assertThat(redis.hvals(key)).isEqualTo(list());
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");

        ListStreamingAdapter<String> channel = new ListStreamingAdapter<>();
        Long count = redis.hvals(channel, key);
        assertThat(count.intValue()).isEqualTo(2);
        assertThat(channel.getList()).hasSize(2);
        assertThat(channel.getList().containsAll(list("1", "1"))).isTrue();
    }

    @Test
    void hscan() {
        redis.hset(key, key, value);
        MapScanCursor<String, String> cursor = redis.hscan(key);

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getMap()).isEqualTo(Collections.singletonMap(key, value));
    }

    @Test
    void hscanWithCursor() {
        redis.hset(key, key, value);

        MapScanCursor<String, String> cursor = redis.hscan(key, ScanCursor.INITIAL);

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getMap()).isEqualTo(Collections.singletonMap(key, value));
    }

    @Test
    void hscanWithCursorAndArgs() {
        redis.hset(key, key, value);

        MapScanCursor<String, String> cursor = redis.hscan(key, ScanCursor.INITIAL, ScanArgs.Builder.limit(2));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getMap()).isEqualTo(Collections.singletonMap(key, value));
    }

    @Test
    void hscanStreaming() {
        redis.hset(key, key, value);
        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<>();

        StreamScanCursor cursor = redis.hscan(adapter, key, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getMap()).isEqualTo(Collections.singletonMap(key, value));
    }

    @Test
    void hscanStreamingWithCursor() {
        redis.hset(key, key, value);
        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<>();

        StreamScanCursor cursor = redis.hscan(adapter, key, ScanCursor.INITIAL);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    void hscanStreamingWithCursorAndArgs() {
        redis.hset(key, key, value);
        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<>();

        StreamScanCursor cursor3 = redis.hscan(adapter, key, ScanCursor.INITIAL, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor3.getCount()).isEqualTo(1);
        assertThat(cursor3.getCursor()).isEqualTo("0");
        assertThat(cursor3.isFinished()).isTrue();
    }

    @Test
    void hscanStreamingWithArgs() {
        redis.hset(key, key, value);
        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<>();

        StreamScanCursor cursor = redis.hscan(adapter, key);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    void hscanMultiple() {

        Map<String, String> expect = new LinkedHashMap<>();
        Map<String, String> check = new LinkedHashMap<>();
        setup100KeyValues(expect);

        MapScanCursor<String, String> cursor = redis.hscan(key, ScanArgs.Builder.limit(5));

        assertThat(cursor.getCursor()).isNotNull();
        assertThat(cursor.getMap()).hasSize(100);

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

        check.putAll(cursor.getMap());

        while (!cursor.isFinished()) {
            cursor = redis.hscan(key, cursor);
            check.putAll(cursor.getMap());
        }

        assertThat(check).isEqualTo(expect);
    }

    @Test
    void hscanMatch() {

        Map<String, String> expect = new LinkedHashMap<>();
        setup100KeyValues(expect);

        MapScanCursor<String, String> cursor = redis.hscan(key, ScanArgs.Builder.limit(100).match("key1*"));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

        assertThat(cursor.getMap()).hasSize(11);
    }

    void setup100KeyValues(Map<String, String> expect) {
        for (int i = 0; i < 100; i++) {
            expect.put(key + i, value + 1);
        }

        redis.hmset(key, expect);
    }
}
