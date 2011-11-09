// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

public class StringCommandTest extends AbstractCommandTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void append() throws Exception {
        assertEquals(value.length(), (long) redis.append(key, value));
        assertEquals(value.length() + 1, (long) redis.append(key, "X"));
    }

    @Test
    public void get() throws Exception {
        assertNull(redis.get(key));
        redis.set(key, value);
        assertEquals(value, redis.get(key));
    }

    @Test
    public void getbit() throws Exception {
        assertEquals(0, (long) redis.getbit(key, 0));
        redis.setbit(key, 0, 1);
        assertEquals(1, (long) redis.getbit(key, 0));
    }

    @Test
    public void getrange() throws Exception {
        assertEquals("", redis.getrange(key, 0, -1));
        redis.set(key, "foobar");
        assertEquals("oba", redis.getrange(key, 2, 4));
        assertEquals("bar", redis.getrange(key, 3, -1));
    }

    @Test
    public void getset() throws Exception {
        assertNull(redis.getset(key, value));
        assertEquals(value, redis.getset(key, "two"));
        assertEquals("two", redis.get(key));
    }

    @Test
    public void mget() throws Exception {
        assertEquals(list((String) null), redis.mget(key));
        redis.set("one", "1");
        redis.set("two", "2");
        assertEquals(list("1", "2"), redis.mget("one", "two"));
    }

    @Test
    public void mset() throws Exception {
        assertEquals(list(null, null), redis.mget("one", "two"));
        Map<String, String> map = new HashMap<String, String>();
        map.put("one", "1");
        map.put("two", "2");
        assertEquals("OK", redis.mset(map));
        assertEquals(list("1", "2"), redis.mget("one", "two"));
    }

    @Test
    public void msetnx() throws Exception {
        redis.set("one", "1");
        Map<String, String> map = new HashMap<String, String>();
        map.put("one", "1");
        map.put("two", "2");
        assertFalse(redis.msetnx(map));
        redis.del("one");
        assertTrue(redis.msetnx(map));
        assertEquals("2", redis.get("two"));
    }

    @Test
    public void set() throws Exception {
        assertNull(redis.get(key));
        assertEquals("OK", redis.set(key, value));
        assertEquals(value, redis.get(key));
    }

    @Test
    public void setbit() throws Exception {
        assertEquals(0, (long) redis.setbit(key, 0, 1));
        assertEquals(1, (long) redis.setbit(key, 0, 0));
    }

    @Test
    public void setex() throws Exception {
        assertEquals("OK", redis.setex(key, 10, value));
        assertEquals(value, redis.get(key));
        assertTrue(redis.ttl(key) >= 9);
    }

    @Test
    public void setnx() throws Exception {
        assertTrue(redis.setnx(key, value));
        assertFalse(redis.setnx(key, value));
    }

    @Test
    public void setrange() throws Exception {
        assertEquals("foo".length(), (long) redis.setrange(key, 0, "foo"));
        assertEquals(6, (long) redis.setrange(key, 3, "bar"));
        assertEquals("foobar", redis.get(key));
    }

    @Test
    public void strlen() throws Exception {
        assertEquals(0, (long) redis.strlen(key));
        redis.set(key, value);
        assertEquals(value.length(), (long) redis.strlen(key));
    }
}
