// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Ignore;
import org.junit.Test;

import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RoleParser;

public class ServerCommandTest extends AbstractCommandTest {
    @Test
    public void bgrewriteaof() throws Exception {
        String msg = "Background append only file rewriting";
        assertThat(redis.bgrewriteaof(), containsString(msg));
    }

    @Test
    public void bgsave() throws Exception {
        while (redis.info().contains("aof_rewrite_in_progress:1")) {
            Thread.sleep(100);
        }
        String msg = "Background saving started";
        assertEquals(msg, redis.bgsave());
    }

    @Test
    public void clientGetSetname() throws Exception {
        assertNull(redis.clientGetname());
        assertEquals("OK", redis.clientSetname("test"));
        assertEquals("test", redis.clientGetname());
        assertEquals("OK", redis.clientSetname(""));
        assertNull(redis.clientGetname());
    }

    @Test
    public void clientPause() throws Exception {
        assertEquals("OK", redis.clientPause(1000));
    }

    @Test
    public void clientKill() throws Exception {
        Pattern p = Pattern.compile(".*addr=([^ ]+).*");
        String clients = redis.clientList();
        Matcher m = p.matcher(clients);

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
    public void configRewrite() throws Exception {

        String result = redis.configRewrite();
        assertEquals("OK", result);
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
    @Ignore("This test will kill your redis server, therefore it's disabled by default")
    public void debugSegfault() throws Exception {
        redis.debugSegfault();
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
        assertTrue(redis.info("server").contains("redis_version"));
    }

    @Test
    public void lastsave() throws Exception {
        Date start = new Date(System.currentTimeMillis() / 1000);
        assertTrue(start.compareTo(redis.lastsave()) <= 0);
    }

    @Test
    public void save() throws Exception {

        while (redis.info().contains("aof_rewrite_in_progress:1")) {
            Thread.sleep(100);
        }
        assertEquals("OK", redis.save());
    }

    @Test
    public void slaveof() throws Exception {
        assertEquals("OK", redis.slaveof("localhost", 0));
        redis.slaveofNoOne();
    }

    @Test
    public void role() throws Exception {

        RedisClient redisClient = new RedisClient("localhost", 6480);
        RedisAsyncConnection<String, String> connection = redisClient.connectAsync();
        try {

            RedisFuture<List<Object>> role = connection.role();
            List<Object> objects = role.get();

            assertEquals("master", objects.get(0));
            assertEquals(Long.class, objects.get(1).getClass());

            RedisInstance redisInstance = RoleParser.parse(objects);
            assertEquals(RedisInstance.Role.MASTER, redisInstance.getRole());
        } finally {
            connection.close();
            redisClient.shutdown();
        }
    }

    @Test
    public void slaveofNoOne() throws Exception {
        assertEquals("OK", redis.slaveofNoOne());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void slowlog() throws Exception {
        long start = System.currentTimeMillis() / 1000;

        assertEquals("OK", redis.configSet("slowlog-log-slower-than", "1"));
        assertEquals("OK", redis.slowlogReset());
        redis.set(key, value);

        List<Object> log = redis.slowlogGet();
        assertEquals(2, log.size());

        List<Object> entry = (List<Object>) log.get(0);
        assertEquals(4, entry.size());
        assertTrue(entry.get(0) instanceof Long);
        assertTrue((Long) entry.get(1) >= start);
        assertTrue(entry.get(2) instanceof Long);
        assertEquals(list("SET", key, value), entry.get(3));

        entry = (List<Object>) log.get(1);
        assertEquals(4, entry.size());
        assertTrue(entry.get(0) instanceof Long);
        assertTrue((Long) entry.get(1) >= start);
        assertTrue(entry.get(2) instanceof Long);
        assertEquals(list("SLOWLOG", "RESET"), entry.get(3));

        assertEquals(1, redis.slowlogGet(1).size());
        assertEquals(4, (long) redis.slowlogLen());

        redis.configSet("slowlog-log-slower-than", "0");
    }

    @Test
    public void sync() throws Exception {
        assertTrue(redis.sync().startsWith("REDIS"));
    }

    @Test
    public void migrate() throws Exception {
        redis.set(key, value);

        String result = redis.migrate("localhost", port + 1, key, 0, 10);
        assertEquals("OK", result);
    }
}
