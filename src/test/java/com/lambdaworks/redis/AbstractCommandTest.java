// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import org.junit.After;
import org.junit.Before;

import java.util.*;

public abstract class AbstractCommandTest {
    public static final String host = "localhost";
    public static final int    port = 6379;

    public static final String authHost = host;
    public static final int    authPort = 6380;
    public static final String passwd   = "passwd";

    protected RedisClient client = new RedisClient(host, port);
    protected RedisConnection<String, String> redis;
    protected String key   = "key";
    protected String value = "value";

    @Before
    public final void openConnection() throws Exception {
        redis = client.connect();
        redis.flushall();
    }

    @After
    public final void closeConnection() throws Exception {
        redis.close();
    }

    protected void rpush(String key, String... values) {
        for (String v : values) {
            redis.rpush(key, v);
        }
    }

    protected void saddAll(String key, String... members) {
        for (String m : members) {
            redis.sadd(key, m);
        }
    }

    protected void zaddAll(String key, Object... members) {
        for (int i = 0; i < members.length; i += 2) {
            Double score = (Double) members[i];
            String value = (String) members[i + 1];
            redis.zadd(key, score, value);
        }
    }

    protected List<String> list(String... args) {
        return Arrays.asList(args);
    }

    protected List<Object> list(Object... args) {
        return Arrays.asList(args);
    }

    protected List<ScoredValue<String>> svlist(ScoredValue<String>... args) {
        return Arrays.asList(args);
    }

    protected ScoredValue<String> sv(double score, String value) {
        return new ScoredValue<String>(score, value);
    }

    protected Set<String> set(String... args) {
        return new HashSet<String>(Arrays.asList(args));
    }
}
