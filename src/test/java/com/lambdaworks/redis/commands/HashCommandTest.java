// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.security.Key;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.lambdaworks.redis.*;

public class HashCommandTest extends AbstractRedisClientTest {

    @Test
    public void hdel() throws Exception {
        assertThat(redis.hdel(key, "one")).isEqualTo(0);
        redis.hset(key, "two", "2");
        assertThat(redis.hdel(key, "one")).isEqualTo(0);
        redis.hset(key, "one", "1");
        assertThat(redis.hdel(key, "one")).isEqualTo(1);
        redis.hset(key, "one", "1");
        assertThat(redis.hdel(key, "one", "two")).isEqualTo(2);
    }

    @Test
    public void hexists() throws Exception {
        assertThat(redis.hexists(key, "one")).isFalse();
        redis.hset(key, "two", "2");
        assertThat(redis.hexists(key, "one")).isFalse();
        redis.hset(key, "one", "1");
        assertThat(redis.hexists(key, "one")).isTrue();
    }

    @Test
    public void hget() throws Exception {
        assertThat(redis.hget(key, "one")).isNull();
        redis.hset(key, "one", "1");
        assertThat(redis.hget(key, "one")).isEqualTo("1");
    }

    @Test
    public void hgetall() throws Exception {
        assertThat(redis.hgetall(key).isEmpty()).isTrue();

        redis.hset(key, "zero", "0");
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");

        Map<String, String> map = redis.hgetall(key);

        assertThat(map).hasSize(3);
        assertThat(map.keySet()).containsExactly("zero", "one", "two");
    }

    @Test
    public void hgetallStreaming() throws Exception {

        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<String, String>();

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
    public void hincrby() throws Exception {
        assertThat(redis.hincrby(key, "one", 1)).isEqualTo(1);
        assertThat(redis.hincrby(key, "one", -2)).isEqualTo(-1);
    }

    @Test
    public void hincrbyfloat() throws Exception {
        assertThat(redis.hincrbyfloat(key, "one", 1.0)).isEqualTo(1.0);
        assertThat(redis.hincrbyfloat(key, "one", -2.0)).isEqualTo(-1.0);
        assertThat(redis.hincrbyfloat(key, "one", 1.23)).isEqualTo(0.23, offset(0.001));
    }

    @Test
    public void hkeys() throws Exception {
        setup();
        List<String> keys = redis.hkeys(key);
        assertThat(keys).hasSize(2);
        assertThat(keys.containsAll(list("one", "two"))).isTrue();
    }

    @Test
    public void hkeysStreaming() throws Exception {
        setup();
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();

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
    public void hlen() throws Exception {
        assertThat((long) redis.hlen(key)).isEqualTo(0);
        redis.hset(key, "one", "1");
        assertThat((long) redis.hlen(key)).isEqualTo(1);
    }

    @Test
    public void hstrlen() throws Exception {
        assertThat((long) redis.hstrlen(key, "one")).isEqualTo(0);
        redis.hset(key, "one", value);
        assertThat((long) redis.hstrlen(key, "one")).isEqualTo(value.length());
    }

    @Test
    public void hmget() throws Exception {
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
    public void hmgetStreaming() throws Exception {
        setupHmget();

        KeyValueStreamingAdapter<String, String> streamingAdapter = new KeyValueStreamingAdapter<>();
        Long count = redis.hmget(streamingAdapter, key, "one", "two");
        Map<String, String> values = streamingAdapter.getMap();
        assertThat(count.intValue()).isEqualTo(2);
        assertThat(values).hasSize(2);
        assertThat(values).containsEntry("one", "1").containsEntry("two", "2");
    }

    @Test
    public void hmset() throws Exception {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("one", "1");
        hash.put("two", "2");
        assertThat(redis.hmset(key, hash)).isEqualTo("OK");
        assertThat(redis.hmget(key, "one", "two")).isEqualTo(list(kv("one", "1"), kv("two", "2")));
    }

    @Test
    public void hmsetWithNulls() throws Exception {
        Map<String, String> hash = new LinkedHashMap<>();
        hash.put("one", null);
        assertThat(redis.hmset(key, hash)).isEqualTo("OK");
        assertThat(redis.hmget(key, "one")).isEqualTo(list(kv("one", "")));

        hash.put("one", "");
        assertThat(redis.hmset(key, hash)).isEqualTo("OK");
        assertThat(redis.hmget(key, "one")).isEqualTo(list(kv("one", "")));
    }

    @Test
    public void hset() throws Exception {
        assertThat(redis.hset(key, "one", "1")).isTrue();
        assertThat(redis.hset(key, "one", "1")).isFalse();
    }

    @Test
    public void hsetnx() throws Exception {
        redis.hset(key, "one", "1");
        assertThat(redis.hsetnx(key, "one", "2")).isFalse();
        assertThat(redis.hget(key, "one")).isEqualTo("1");
    }

    @Test
    public void hvals() throws Exception {
        assertThat(redis.hvals(key)).isEqualTo(list());
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
        List<String> values = redis.hvals(key);
        assertThat(values).hasSize(2);
        assertThat(values.containsAll(list("1", "1"))).isTrue();
    }

    @Test
    public void hvalsStreaming() throws Exception {
        assertThat(redis.hvals(key)).isEqualTo(list());
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");

        ListStreamingAdapter<String> channel = new ListStreamingAdapter<String>();
        Long count = redis.hvals(channel, key);
        assertThat(count.intValue()).isEqualTo(2);
        assertThat(channel.getList()).hasSize(2);
        assertThat(channel.getList().containsAll(list("1", "1"))).isTrue();
    }

    @Test
    public void hscan() throws Exception {
        redis.hset(key, key, value);
        MapScanCursor<String, String> cursor = redis.hscan(key);

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getMap()).isEqualTo(Collections.singletonMap(key, value));
    }

    @Test
    public void hscanWithCursor() throws Exception {
        redis.hset(key, key, value);

        MapScanCursor<String, String> cursor = redis.hscan(key, ScanCursor.INITIAL);

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getMap()).isEqualTo(Collections.singletonMap(key, value));
    }

    @Test
    public void hscanWithCursorAndArgs() throws Exception {
        redis.hset(key, key, value);

        MapScanCursor<String, String> cursor = redis.hscan(key, ScanCursor.INITIAL, ScanArgs.Builder.limit(2));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(cursor.getMap()).isEqualTo(Collections.singletonMap(key, value));
    }

    @Test
    public void hscanStreaming() throws Exception {
        redis.hset(key, key, value);
        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<String, String>();

        StreamScanCursor cursor = redis.hscan(adapter, key, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getMap()).isEqualTo(Collections.singletonMap(key, value));
    }

    @Test
    public void hscanStreamingWithCursor() throws Exception {
        redis.hset(key, key, value);
        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<String, String>();

        StreamScanCursor cursor = redis.hscan(adapter, key, ScanCursor.INITIAL);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    public void hscanStreamingWithCursorAndArgs() throws Exception {
        redis.hset(key, key, value);
        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<String, String>();

        StreamScanCursor cursor3 = redis.hscan(adapter, key, ScanCursor.INITIAL, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor3.getCount()).isEqualTo(1);
        assertThat(cursor3.getCursor()).isEqualTo("0");
        assertThat(cursor3.isFinished()).isTrue();
    }

    @Test
    public void hscanStreamingWithArgs() throws Exception {
        redis.hset(key, key, value);
        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<String, String>();

        StreamScanCursor cursor = redis.hscan(adapter, key);

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
    }

    @Test
    public void hscanMultiple() throws Exception {

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
    public void hscanMatch() throws Exception {

        Map<String, String> expect = new LinkedHashMap<>();
        setup100KeyValues(expect);

        MapScanCursor<String, String> cursor = redis.hscan(key, ScanArgs.Builder.limit(100).match("key1*"));

        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();

        assertThat(cursor.getMap()).hasSize(11);
    }

    protected void setup100KeyValues(Map<String, String> expect) {
        for (int i = 0; i < 100; i++) {
            expect.put(key + i, value + 1);
        }

        redis.hmset(key, expect);
    }
}
