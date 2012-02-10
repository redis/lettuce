// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

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
        assertEquals(list(), redis.hkeys(key));
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
        List<String> keys = redis.hkeys(key);
        assertEquals(2, keys.size());
        assertTrue(keys.containsAll(list("one", "two")));
    }

    @Test
    public void hlen() throws Exception {
        assertEquals(0, (long) redis.hlen(key));
        redis.hset(key, "one", "1");
        assertEquals(1, (long) redis.hlen(key));
    }

    @Test
    public void hmget() throws Exception {
        assertEquals(list(null, null), redis.hmget(key, "one", "two"));
        redis.hset(key, "one", "1");
        redis.hset(key, "two", "2");
        List<String> values = redis.hmget(key, "one", "two");
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
}
