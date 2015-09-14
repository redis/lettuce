// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.DefaultEventLoopGroupProvider;
import com.lambdaworks.redis.resource.EventLoopGroupProvider;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;

public abstract class AbstractCommandTest {
    public static final String host = TestSettings.host();
    public static final int port = TestSettings.port();
    public static final String passwd = TestSettings.password();

    protected static RedisClient client;
    protected Logger log = Logger.getLogger(getClass());
    protected RedisConnection<String, String> redis;
    protected String key = "key";
    protected String value = "value";

    protected static EventLoopGroupProvider keepAlive = new DefaultEventLoopGroupProvider(10) {
        @Override
        public Promise<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout, TimeUnit unit) {
            DefaultPromise<Boolean> result = new DefaultPromise<Boolean>(ImmediateEventExecutor.INSTANCE);
            result.setSuccess(true);

            return result;
        }
    };

    protected static DefaultClientResources resources = new DefaultClientResources.Builder().eventLoopGroupProvider(keepAlive)
            .build();
    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    resources.shutdown().get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @BeforeClass
    public static void setupClient() {
        client = getRedisClient();
    }

    protected static RedisClient getRedisClient() {
        return RedisClient.create(resources, RedisURI.Builder.redis(host, port).build());
    }

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(client);
    }

    @Before
    public void openConnection() throws Exception {
        client.setOptions(new ClientOptions.Builder().build());
        redis = client.connect();
        redis.flushall();
        redis.flushdb();
    }

    @After
    public void closeConnection() throws Exception {
        redis.close();
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

    protected KeyValue<String, String> kv(String key, String value) {
        return new KeyValue<String, String>(key, value);
    }

    protected ScoredValue<String> sv(double score, String value) {
        return new ScoredValue<String>(score, value);
    }

    protected Set<String> set(String... args) {
        return new HashSet<String>(Arrays.asList(args));
    }

    public abstract class WithPasswordRequired {
        protected abstract void run(RedisClient client) throws Exception;

        public WithPasswordRequired() throws Exception {
            try {
                redis.configSet("requirepass", passwd);
                redis.auth(passwd);

                RedisClient client = getRedisClient();
                try {
                    run(client);
                } finally {
                    FastShutdown.shutdown(client);
                }
            } finally {

                redis.configSet("requirepass", "");
            }
        }
    }
}
