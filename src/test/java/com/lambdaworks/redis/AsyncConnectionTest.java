// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AsyncConnectionTest extends AbstractCommandTest {
    private RedisAsyncConnection<String,String> async;

    @Before
    public void openAsyncConnection() throws Exception {
        async = redis.getAsyncConnection();
    }

    @Test
    public void booleanCommand() throws Exception {
        assertNull(async.set(key, value));
        assertNull(async.exists(key));
        assertEquals(list("OK", true), async.flush());
    }

    @Test
    public void dateCommand() throws Exception {
        Date date = redis.lastsave();
        assertNull(async.lastsave());
        assertEquals(list(date), async.flush());
    }

    @Test
    public void doubleCommand() throws Exception {
        assertNull(async.zadd(key, 1.2, value));
        assertNull(async.zscore(key, value));
        assertEquals(list(1L, 1.2), async.flush());
    }

    @Test
    public void integerCommand() throws Exception {
        assertNull(async.incrby(key, 3));
        assertEquals(list(3L), async.flush());
    }

    @Test
    public void keyListCommand() throws Exception {
        assertNull(async.hset(key, "one", "1"));
        assertNull(async.hkeys(key));
        assertEquals(list(true, list("one")), async.flush());
    }

    @Test
    public void mapCommand() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("one", "1");
        assertNull(async.hmset(key, map));
        assertNull(async.hgetall(key));
        assertEquals(list("OK", map), async.flush());
    }

    @Test
    public void multiCommand() throws Exception {
        assertEquals("OK", async.multi());
        assertNull(async.set(key, value));
        assertNull(async.get(key));
        assertEquals(list("OK", value), async.exec());
        assertEquals(list("OK", value), async.flush());
    }

    @Test
    public void scoredValueListCommand() throws Exception {
        assertNull(async.zadd(key, 1.0, "a"));
        assertNull(async.zadd(key, 2.0, "b"));
        assertNull(async.zrangeWithScores(key, 0, -1));
        assertEquals(list(1L, 1L, svlist(sv(1.0, "a"), sv(2.0, "b"))), async.flush());
    }

    @Test
    public void stringCommand() throws Exception {
        assertNull(async.set(key, value));
        assertNull(async.get(key));
        assertEquals(list("OK", value), async.flush());
    }

    @Test
    public void valueCommand() throws Exception {
        assertNull(async.set(key, value));
        assertNull(async.get(key));
        assertEquals(list("OK", value), async.flush());
    }

    @Test
    public void valueListCommand() throws Exception {
        assertNull(async.rpush(key, "a"));
        assertNull(async.rpush(key, "b"));
        assertNull(async.lrange(key, 0, -1));
        assertEquals(list(1L, 2L, list("a", "b")), async.flush());
    }

    @Test
    public void valueSetOutput() throws Exception {
        assertNull(async.sadd(key, "a"));
        assertNull(async.sadd(key, "b"));
        assertNull(async.smembers(key));
        assertEquals(list(1L, 1L, set("a", "b")), async.flush());
    }

    @Test
    public void discard() throws Exception {
        assertEquals("OK", async.multi());
        assertEquals("OK", async.discard());
    }
}
