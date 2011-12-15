// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ListCommandTest extends AbstractCommandTest {
    @Test
    public void blpop() throws Exception {
        redis.rpush("two", "2", "3");
        assertEquals(kv("two", "2"), redis.blpop(1, "one", "two"));
    }

    @Test
    public void blpopTimeout() throws Exception {
        redis.setTimeout(10, TimeUnit.SECONDS);
        assertNull(redis.blpop(1, key));
    }

    @Test
    public void brpop() throws Exception {
        redis.rpush("two", "2", "3");
        assertEquals(kv("two", "3"), redis.brpop(1, "one", "two"));
    }

    @Test
    public void brpoplpush() throws Exception {
        redis.rpush("one", "1", "2");
        redis.rpush("two", "3", "4");
        assertEquals("2", redis.brpoplpush(1, "one", "two"));
        assertEquals(list("1"), redis.lrange("one", 0, -1));
        assertEquals(list("2", "3", "4"), redis.lrange("two", 0, -1));
    }

    @Test
    public void brpoplpushTimeout() throws Exception {
        assertNull(redis.brpoplpush(1, "one", "two"));
    }

    @Test
    public void lindex() throws Exception {
        assertNull(redis.lindex(key, 0));
        redis.rpush(key, "one");
        assertEquals("one", redis.lindex(key, 0));
    }

    @Test
    public void linsert() throws Exception {
        assertEquals(0, (long) redis.linsert(key, false, "one", "two"));
        redis.rpush(key, "one");
        redis.rpush(key, "three");
        assertEquals(3, (long) redis.linsert(key, true, "three", "two"));
        assertEquals(list("one", "two", "three"), redis.lrange(key, 0, -1));
    }

    @Test
    public void llen() throws Exception {
        assertEquals(0, (long) redis.llen(key));
        redis.lpush(key, "one");
        assertEquals(1, (long) redis.llen(key));
    }

    @Test
    public void lpop() throws Exception {
        assertNull(redis.lpop(key));
        redis.rpush(key, "one", "two");
        assertEquals("one", redis.lpop(key));
        assertEquals(list("two"), redis.lrange(key, 0, -1));
    }

    @Test
    public void lpush() throws Exception {
        assertEquals(1, (long) redis.lpush(key, "two"));
        assertEquals(2, (long) redis.lpush(key, "one"));
        assertEquals(list("one", "two"), redis.lrange(key, 0, -1));
        assertEquals(4, (long) redis.lpush(key, "three", "four"));
        assertEquals(list("four", "three", "one", "two"), redis.lrange(key, 0, -1));
    }

    @Test
    public void lpushx() throws Exception {
        assertEquals(0, (long) redis.lpushx(key, "two"));
        redis.lpush(key, "two");
        assertEquals(2, (long) redis.lpushx(key, "one"));
        assertEquals(list("one", "two"), redis.lrange(key, 0, -1));
    }

    @Test
    public void lrange() throws Exception {
        assertTrue(redis.lrange(key, 0, 10).isEmpty());
        redis.rpush(key, "one", "two", "three");
        List<String> range = redis.lrange(key, 0, 1);
        assertEquals(2, range.size());
        assertEquals("one", range.get(0));
        assertEquals("two", range.get(1));
        assertEquals(3, redis.lrange(key, 0, -1).size());
    }

    @Test
    public void lrem() throws Exception {
        assertEquals(0, (long) redis.lrem(key, 0, value));

        redis.rpush(key, "1", "2", "1", "2", "1");
        assertEquals(1, (long) redis.lrem(key, 1, "1"));
        assertEquals(list("2", "1", "2", "1"), redis.lrange(key, 0, -1));

        redis.lpush(key, "1");
        assertEquals(1, (long) redis.lrem(key, -1, "1"));
        assertEquals(list("1", "2", "1", "2"), redis.lrange(key, 0, -1));

        redis.lpush(key, "1");
        assertEquals(3, (long) redis.lrem(key, 0, "1"));
        assertEquals(list("2", "2"), redis.lrange(key, 0, -1));
    }

    @Test
    public void lset() throws Exception {
        redis.rpush(key, "one", "two", "three");
        assertEquals("OK", redis.lset(key, 2, "san"));
        assertEquals(list("one", "two", "san"), redis.lrange(key, 0, -1));
    }

    @Test
    public void ltrim() throws Exception {
        redis.rpush(key, "1", "2", "3", "4", "5", "6");
        assertEquals("OK", redis.ltrim(key, 0, 3));
        assertEquals(list("1", "2", "3", "4"), redis.lrange(key, 0, -1));
        assertEquals("OK", redis.ltrim(key, -2, -1));
        assertEquals(list("3", "4"), redis.lrange(key, 0, -1));
    }

    @Test
    public void rpop() throws Exception {
        assertNull(redis.rpop(key));
        redis.rpush(key, "one", "two");
        assertEquals("two", redis.rpop(key));
        assertEquals(list("one"), redis.lrange(key, 0, -1));
    }

    @Test
    public void rpoplpush() throws Exception {
        assertNull(redis.rpoplpush("one", "two"));
        redis.rpush("one", "1", "2");
        redis.rpush("two", "3", "4");
        assertEquals("2", redis.rpoplpush("one", "two"));
        assertEquals(list("1"), redis.lrange("one", 0, -1));
        assertEquals(list("2", "3", "4"), redis.lrange("two", 0, -1));
    }

    @Test
    public void rpush() throws Exception {
        assertEquals(1, (long) redis.rpush(key, "one"));
        assertEquals(2, (long) redis.rpush(key, "two"));
        assertEquals(list("one", "two"), redis.lrange(key, 0, -1));
        assertEquals(4, (long) redis.rpush(key, "three", "four"));
        assertEquals(list("one", "two", "three", "four"), redis.lrange(key, 0, -1));
    }

    @Test
    public void rpushx() throws Exception {
        assertEquals(0, (long) redis.rpushx(key, "one"));
        redis.rpush(key, "one");
        assertEquals(2, (long) redis.rpushx(key, "two"));
        assertEquals(list("one", "two"), redis.lrange(key, 0, -1));
    }
}
