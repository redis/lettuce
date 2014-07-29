// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class HashCommandTest extends AbstractCommandTest {
    @Test
    public void hdel() throws Exception {
        assertEquals(0, (long) redis.hdel(key, "one"));
        redis.hset(key, "two", "2");
        assertEquals(0, (long) redis.hdel(key, "one"));
        redis.hset(key, "one", "1");
        assertEquals(1, (long) redis.hdel(key, "one"));
        redis.hset(key, "one", "1");
        assertEquals(2, (long) redis.hdel(key, "one", "two"));
    }

    @Test
    public void hexists() throws Exception {
        assertFalse(redis.hexists(key, "one"));
        redis.hset(key, "two", "2");
        assertFalse(redis.hexists(key, "one"));
        redis.hset(key, "one", "1");
        assertTrue(redis.hexists(key, "one"));
    }

    @Test
    public void hget() throws Exception {
        assertNull(redis.hget(key, "one"));
        redis.hset(key, "one", "1");
        assertEquals("1", redis.hget(key, "one"));
    }

    @Test
    public void hgetall() throws Exception {
        assertTrue(redis.hgetall(key).isEmpty());
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
        Map<String, String> map = redis.hgetall(key);
        assertEquals(2, map.size());
        assertEquals("1", map.get("one"));
        assertEquals("2", map.get("two"));
    }

    @Test
    public void hgetallStreaming() throws Exception {

        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<String, String>();

        assertTrue(redis.hgetall(key).isEmpty());
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
        Long count = redis.hgetall(adapter, key);
        Map<String, String> map = adapter.getMap();
        assertEquals(2, count.intValue());
        assertEquals(2, map.size());
        assertEquals("1", map.get("one"));
        assertEquals("2", map.get("two"));
    }

    @Test
    public void hincrby() throws Exception {
        assertEquals(1, (long) redis.hincrby(key, "one", 1));
        assertEquals(-1, (long) redis.hincrby(key, "one", -2));
    }

    @Test
    public void hincrbyfloat() throws Exception {
        assertEquals(1.0, redis.hincrbyfloat(key, "one", 1.0), 0.1);
        assertEquals(-1.0, redis.hincrbyfloat(key, "one", -2.0), 0.1);
    }

    @Test
    public void hkeys() throws Exception {
        setup();
        List<String> keys = redis.hkeys(key);
        assertEquals(2, keys.size());
        assertTrue(keys.containsAll(list("one", "two")));
    }

    @Test
    public void hkeysStreaming() throws Exception {
        setup();
        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();

        Long count = redis.hkeys(streamingAdapter, key);
        assertEquals(2, count.longValue());

        List<String> keys = streamingAdapter.getList();
        assertEquals(2, keys.size());
        assertTrue(keys.containsAll(list("one", "two")));
    }
    private void setup()
    {
        assertEquals(list(), redis.hkeys(key));
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
    }

    @Test
    public void hlen() throws Exception {
        assertEquals(0, (long) redis.hlen(key));
        redis.hset(key, "one", "1");
        assertEquals(1, (long) redis.hlen(key));
    }

    @Test
    public void hmget() throws Exception {
        setupHmget();
        List<String> values = redis.hmget(key, "one", "two");
        assertEquals(2, values.size());
        assertTrue(values.containsAll(list("1", "1")));
    }

    private void setupHmget() {
        assertEquals(list(null, null), redis.hmget(key, "one", "two"));
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
    }

    @Test
    public void hmgetStreaming() throws Exception {
        setupHmget();

        ListStreamingAdapter<String> streamingAdapter = new ListStreamingAdapter<String>();
        Long count = redis.hmget(streamingAdapter, key, "one", "two");
        List<String> values = streamingAdapter.getList();
        assertEquals(2, count.intValue());
        assertEquals(2, values.size());
        assertTrue(values.containsAll(list("1", "1")));
    }

    @Test
    public void hmset() throws Exception {
        Map<String, String> hash = new HashMap<String, String>();
        hash.put("one", "1");
        hash.put("two", "2");
        assertEquals("OK", redis.hmset(key, hash));
        assertEquals(list("1", "2"), redis.hmget(key, "one", "two"));
    }

    @Test
    public void hset() throws Exception {
        assertEquals(true, redis.hset(key, "one", "1"));
        assertEquals(false, redis.hset(key, "one", "1"));
    }

    @Test
    public void hsetnx() throws Exception {
        redis.hset(key, "one", "1");
        assertFalse(redis.hsetnx(key, "one", "2"));
        assertEquals("1", redis.hget(key, "one"));
    }

    @Test
    public void hvals() throws Exception {
        assertEquals(list(), redis.hvals(key));
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
        List<String> values = redis.hvals(key);
        assertEquals(2, values.size());
        assertTrue(values.containsAll(list("1", "1")));
    }

    @Test
    public void hvalsStreaming() throws Exception {
        assertEquals(list(), redis.hvals(key));
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");

        ListStreamingAdapter<String> channel = new ListStreamingAdapter();
        Long count = redis.hvals(channel, key);
        assertEquals(2, count.intValue());
        assertEquals(2, channel.getList().size());
        assertTrue(channel.getList().containsAll(list("1", "1")));
    }

    @Test
    public void hscan() throws Exception {
        redis.hset(key, key, value);
        MapScanCursor<String, String> cursor = redis.hscan(key);

        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());
        assertEquals(ImmutableMap.of(key, value), cursor.getMap());

        MapScanCursor<String, String> cursor2 = redis.hscan(key, cursor);

        assertEquals("0", cursor2.getCursor());
        assertTrue(cursor2.isFinished());
        assertEquals(ImmutableMap.of(key, value), cursor2.getMap());

        MapScanCursor<String, String> cursor3 = redis.hscan(key, cursor, ScanArgs.Builder.count(2));

        assertEquals("0", cursor3.getCursor());
        assertTrue(cursor3.isFinished());
        assertEquals(ImmutableMap.of(key, value), cursor3.getMap());

    }

    @Test
    public void hscanStreaming() throws Exception {
        redis.hset(key, key, value);
        KeyValueStreamingAdapter<String, String> adapter = new KeyValueStreamingAdapter<String, String>();

        StreamScanCursor cursor = redis.hscan(adapter, key, ScanArgs.Builder.count(100).match("*"));

        assertEquals(1, cursor.getCount());
        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());
        assertEquals(ImmutableMap.of(key, value), adapter.getMap());

        StreamScanCursor cursor2 = redis.hscan(adapter, key, cursor);

        assertEquals(1, cursor2.getCount());
        assertEquals("0", cursor2.getCursor());
        assertTrue(cursor2.isFinished());

        StreamScanCursor cursor3 = redis.hscan(adapter, key, cursor, ScanArgs.Builder.count(100).match("*"));

        assertEquals(1, cursor3.getCount());
        assertEquals("0", cursor3.getCursor());
        assertTrue(cursor3.isFinished());

   }

    @Test
    public void hscanMultiple() throws Exception {

        Map<String, String> expect = new HashMap<String, String>();
        Map<String, String> check = new HashMap<String, String>();
        setup100KeyValues(expect);

        MapScanCursor<String, String> cursor = redis.hscan(key, ScanArgs.Builder.count(5));

        assertNotNull(cursor.getCursor());
        assertEquals(100, cursor.getMap().size());

        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());

        check.putAll(cursor.getMap());

        while (!cursor.isFinished()) {
            cursor = redis.hscan(key, cursor);
            check.putAll(cursor.getMap());
        }

        assertEquals(expect, check);
    }

    @Test
    public void hscanMatch() throws Exception {

        Map<String, String> expect = new HashMap<String, String>();
        setup100KeyValues(expect);

        MapScanCursor<String, String> cursor = redis.hscan(key, ScanArgs.Builder.count(100).match("key1*"));

        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());

        assertEquals(11, cursor.getMap().size());
    }

    protected void setup100KeyValues(Map<String, String> expect) {
        for (int i = 0; i < 100; i++) {
            expect.put(key + i, value + 1);
        }

        redis.hmset(key, expect);
    }
}
