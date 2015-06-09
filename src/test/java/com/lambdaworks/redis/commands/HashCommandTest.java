// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.KeyValueStreamingAdapter;
import com.lambdaworks.redis.ListStreamingAdapter;
import com.lambdaworks.redis.MapScanCursor;
import com.lambdaworks.redis.ScanArgs;
import com.lambdaworks.redis.StreamScanCursor;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class HashCommandTest extends AbstractRedisClientTest {
    @Test
    public void hdel() throws Exception {
        Assertions.assertThat(redis.hdel(key, "one")).isEqualTo(0);
        redis.hset(key, "two", "2");
        Assertions.assertThat(redis.hdel(key, "one")).isEqualTo(0);
        redis.hset(key, "one", "1");
        Assertions.assertThat(redis.hdel(key, "one")).isEqualTo(1);
        redis.hset(key, "one", "1");
        Assertions.assertThat(redis.hdel(key, "one", "two")).isEqualTo(2);
    }

    @Test
    public void hexists() throws Exception {
        Assertions.assertThat(redis.hexists(key, "one")).isFalse();
        redis.hset(key, "two", "2");
        Assertions.assertThat(redis.hexists(key, "one")).isFalse();
        redis.hset(key, "one", "1");
        Assertions.assertThat(redis.hexists(key, "one")).isTrue();
    }

    @Test
    public void hget() throws Exception {
        Assertions.assertThat(redis.hget(key, "one")).isNull();
        redis.hset(key, "one", "1");
        Assertions.assertThat(redis.hget(key, "one")).isEqualTo("1");
    }

    @Test
    public void hgetall() throws Exception {
        Assertions.assertThat(redis.hgetall(key).isEmpty()).isTrue();
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
        Map<String, String> map = redis.hgetall(key);
        assertThat(map).hasSize(2);
        assertThat(map.get("one")).isEqualTo("1");
        assertThat(map.get("two")).isEqualTo("2");
    }

    @Test
    public void hgetallStreaming() throws Exception {

        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<String, String>();

        Assertions.assertThat(redis.hgetall(key).isEmpty()).isTrue();
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
        Assertions.assertThat(redis.hincrby(key, "one", 1)).isEqualTo(1);
        Assertions.assertThat(redis.hincrby(key, "one", -2)).isEqualTo(-1);
    }

    @Test
    public void hincrbyfloat() throws Exception {
        Assertions.assertThat(redis.hincrbyfloat(key, "one", 1.0)).isEqualTo(1.0);
        Assertions.assertThat(redis.hincrbyfloat(key, "one", -2.0)).isEqualTo(-1.0);
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
        Assertions.assertThat(redis.hkeys(key)).isEqualTo(list());
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
    public void hmget() throws Exception {
        setupHmget();
        List<String> values = redis.hmget(key, "one", "two");
        assertThat(values).hasSize(2);
        assertThat(values.containsAll(list("1", "1"))).isTrue();
    }

    private void setupHmget() {
        Assertions.assertThat(redis.hmget(key, "one", "two")).isEqualTo(list(null, null));
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
    }

    @Test
    public void hmgetStreaming() throws Exception {
        setupHmget();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.hmget(streamingAdapter, key, "one", "two");
        List<String> values = streamingAdapter.getList();
        assertThat(count.intValue()).isEqualTo(2);
        assertThat(values).hasSize(2);
        assertThat(values.containsAll(list("1", "1"))).isTrue();
    }

    @Test
    public void hmset() throws Exception {
        Map<String, String> hash = new HashMap<String, String>();
        hash.put("one", "1");
        hash.put("two", "2");
        Assertions.assertThat(redis.hmset(key, hash)).isEqualTo("OK");
        Assertions.assertThat(redis.hmget(key, "one", "two")).isEqualTo(list("1", "2"));
    }

    @Test
    public void hmsetWithNulls() throws Exception {
        Map<String, String> hash = new HashMap<String, String>();
        hash.put("one", null);
        Assertions.assertThat(redis.hmset(key, hash)).isEqualTo("OK");
        Assertions.assertThat(redis.hmget(key, "one")).isEqualTo(list(""));

        hash.put("one", "");
        Assertions.assertThat(redis.hmset(key, hash)).isEqualTo("OK");
        Assertions.assertThat(redis.hmget(key, "one")).isEqualTo(list(""));
    }

    @Test
    public void hset() throws Exception {
        Assertions.assertThat(redis.hset(key, "one", "1")).isTrue();
        Assertions.assertThat(redis.hset(key, "one", "1")).isFalse();
    }

    @Test
    public void hsetnx() throws Exception {
        redis.hset(key, "one", "1");
        Assertions.assertThat(redis.hsetnx(key, "one", "2")).isFalse();
        Assertions.assertThat(redis.hget(key, "one")).isEqualTo("1");
    }

    @Test
    public void hvals() throws Exception {
        Assertions.assertThat(redis.hvals(key)).isEqualTo(list());
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
        List<String> values = redis.hvals(key);
        assertThat(values).hasSize(2);
        assertThat(values.containsAll(list("1", "1"))).isTrue();
    }

    @Test
    public void hvalsStreaming() throws Exception {
        Assertions.assertThat(redis.hvals(key)).isEqualTo(list());
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
        assertThat(cursor.getMap()).isEqualTo(ImmutableMap.of(key, value));

        MapScanCursor<String, String> cursor2 = redis.hscan(key, cursor);

        assertThat(cursor2.getCursor()).isEqualTo("0");
        assertThat(cursor2.isFinished()).isTrue();
        assertThat(cursor2.getMap()).isEqualTo(ImmutableMap.of(key, value));

        MapScanCursor<String, String> cursor3 = redis.hscan(key, cursor, ScanArgs.Builder.limit(2));

        assertThat(cursor3.getCursor()).isEqualTo("0");
        assertThat(cursor3.isFinished()).isTrue();
        assertThat(cursor3.getMap()).isEqualTo(ImmutableMap.of(key, value));
    }

    @Test
    public void hscanStreaming() throws Exception {
        redis.hset(key, key, value);
        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<String, String>();

        StreamScanCursor cursor = redis.hscan(adapter, key, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor.getCount()).isEqualTo(1);
        assertThat(cursor.getCursor()).isEqualTo("0");
        assertThat(cursor.isFinished()).isTrue();
        assertThat(adapter.getMap()).isEqualTo(ImmutableMap.of(key, value));

        StreamScanCursor cursor2 = redis.hscan(adapter, key, cursor);

        assertThat(cursor2.getCount()).isEqualTo(1);
        assertThat(cursor2.getCursor()).isEqualTo("0");
        assertThat(cursor2.isFinished()).isTrue();

        StreamScanCursor cursor3 = redis.hscan(adapter, key, cursor, ScanArgs.Builder.limit(100).match("*"));

        assertThat(cursor3.getCount()).isEqualTo(1);
        assertThat(cursor3.getCursor()).isEqualTo("0");
        assertThat(cursor3.isFinished()).isTrue();

        StreamScanCursor cursor4 = redis.hscan(adapter, key);

        assertThat(cursor4.getCount()).isEqualTo(1);
        assertThat(cursor4.getCursor()).isEqualTo("0");
        assertThat(cursor4.isFinished()).isTrue();

    }

    @Test
    public void hscanMultiple() throws Exception {

        Map<String, String> expect = new HashMap<String, String>();
        Map<String, String> check = new HashMap<String, String>();
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

        Map<String, String> expect = new HashMap<String, String>();
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
