// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import static org.junit.Assert.*;

public class SetCommandTest extends AbstractCommandTest {
    @Test
    public void sadd() throws Exception {
        assertEquals(1, (long) redis.sadd(key, "a"));
        assertEquals(0, (long) redis.sadd(key, "a"));
        assertEquals(set("a"), redis.smembers(key));
        assertEquals(2, (long) redis.sadd(key, "b", "c"));
        assertEquals(set("a", "b", "c"), redis.smembers(key));
    }

    @Test
    public void scard() throws Exception {
        assertEquals(0, (long) redis.scard(key));
        redis.sadd(key, "a");
        assertEquals(1, (long) redis.scard(key));
    }

    @Test
    public void sdiff() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(set("b", "d"), redis.sdiff("key1", "key2", "key3"));
    }

    @Test
    public void sdiffstore() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(2, (long) redis.sdiffstore("newset", "key1", "key2", "key3"));
        assertEquals(set("b", "d"), redis.smembers("newset"));
    }

    @Test
    public void sinter() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(set("c"), redis.sinter("key1", "key2", "key3"));
    }

    @Test
    public void sinterstore() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(1, (long) redis.sinterstore("newset", "key1", "key2", "key3"));
        assertEquals(set("c"), redis.smembers("newset"));
    }

    @Test
    public void sismember() throws Exception {
        assertFalse(redis.sismember(key, "a"));
        redis.sadd(key, "a");
        assertTrue(redis.sismember(key, "a"));
    }

    @Test
    public void smove() throws Exception {
        redis.sadd(key, "a", "b", "c");
        assertFalse(redis.smove(key, "key1", "d"));
        assertTrue(redis.smove(key, "key1", "a"));
        assertEquals(set("b", "c"), redis.smembers(key));
        assertEquals(set("a"), redis.smembers("key1"));
    }

    @Test
    public void smembers() throws Exception {
        redis.sadd(key, "a");
        redis.sadd(key, "b");
        assertTrue(set("a", "b").equals(redis.smembers(key)));
    }

    @Test
    public void spop() throws Exception {
        assertNull(redis.spop(key));
        redis.sadd(key, "a", "b", "c");
        String rand = redis.spop(key);
        assertTrue(set("a", "b", "c").contains(rand));
        assertFalse(redis.smembers(key).contains(rand));
    }

    @Test
    public void srandmember() throws Exception {
        assertNull(redis.spop(key));
        redis.sadd(key, "a", "b", "c");
        assertTrue(set("a", "b", "c").contains(redis.srandmember(key)));
        assertEquals(set("a", "b", "c"), redis.smembers(key));
    }

    @Test
    public void srem() throws Exception {
        redis.sadd(key, "a", "b", "c");
        assertEquals(0, (long) redis.srem(key, "d"));
        assertEquals(1, (long) redis.srem(key, "b"));
        assertEquals(set("a", "c"), redis.smembers(key));
        assertEquals(2, (long) redis.srem(key, "a", "c"));
        assertEquals(set(), redis.smembers(key));
    }

    @Test
    public void sunion() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(set("a", "b", "c", "d", "e"), redis.sunion("key1", "key2", "key3"));
    }

    @Test
    public void sunionstore() throws Exception {
        redis.sadd("key1", "a", "b", "c", "d");
        redis.sadd("key2", "c");
        redis.sadd("key3", "a", "c", "e");
        assertEquals(5, (long) redis.sunionstore("newset", "key1", "key2", "key3"));
        assertEquals(set("a", "b", "c", "d", "e"), redis.smembers("newset"));
    }
}
