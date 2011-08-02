// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.Test;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ServerCommandTest extends AbstractCommandTest {
    @Test
    public void bgrewriteaof() throws Exception {
        String msg = "Background append only file rewriting started";
        assertEquals(msg, redis.bgrewriteaof());
    }

    @Test
    public void bgsave() throws Exception {
        String msg = "Background saving started";
        assertEquals(msg, redis.bgsave());
    }

    @Test
    public void clientKill() throws Exception {
        Pattern p = Pattern.compile("addr=(\\S+)");
        Matcher m = p.matcher(redis.clientList());
        assertTrue(m.lookingAt());
        assertEquals("OK", redis.clientKill(m.group(1)));
    }

    @Test
    public void clientList() throws Exception {
        assertTrue(redis.clientList().contains("addr="));
    }

    @Test
    public void configGet() throws Exception {
        assertEquals(list("maxmemory", "0"), redis.configGet("maxmemory"));
    }

    @Test
    public void configResetstat() throws Exception {
        redis.get(key);
        redis.get(key);
        assertEquals("OK", redis.configResetstat());
        assertTrue(redis.info().contains("keyspace_misses:0"));
    }

    @Test
    public void configSet() throws Exception {
        String maxmemory = redis.configGet("maxmemory").get(1);
        assertEquals("OK", redis.configSet("maxmemory", "1024"));
        assertEquals("1024", redis.configGet("maxmemory").get(1));
        redis.configSet("maxmemory", maxmemory);
    }

    @Test
    public void dbsize() throws Exception {
        assertEquals(0, (long) redis.dbsize());
        redis.set(key, value);
        assertEquals(1, (long) redis.dbsize());
    }

    @Test
    public void debugObject() throws Exception {
        redis.set(key, value);
        redis.debugObject(key);
    }

    @Test
    public void flushall() throws Exception {
        redis.set(key, value);
        assertEquals("OK", redis.flushall());
        assertNull(redis.get(key));
    }

    @Test
    public void flushdb() throws Exception {
        redis.set(key, value);
        redis.select(1);
        redis.set(key, value + "X");
        assertEquals("OK", redis.flushdb());
        assertNull(redis.get(key));
        redis.select(0);
        assertEquals(value, redis.get(key));
    }

    @Test
    public void info() throws Exception {
        assertTrue(redis.info().contains("redis_version"));
    }

    @Test
    public void lastsave() throws Exception {
        Date start = new Date(System.currentTimeMillis() / 1000);
        assertTrue(start.compareTo(redis.lastsave()) <= 0);
    }

    @Test
    public void save() throws Exception {
        assertEquals("OK", redis.save());
    }

    @Test
    public void slaveof() throws Exception {
        assertEquals("OK", redis.slaveof("localhost", 0));
        redis.slaveofNoOne();
    }

    @Test
    public void slaveofNoOne() throws Exception {
        assertEquals("OK", redis.slaveofNoOne());
    }

    @Test
    public void sync() throws Exception {
        assertTrue(redis.sync().startsWith("REDIS"));
    }
}
