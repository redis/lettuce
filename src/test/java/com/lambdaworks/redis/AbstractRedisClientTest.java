// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.lambdaworks.redis.api.sync.RedisCommands;

public abstract class AbstractRedisClientTest extends AbstractTest {

    protected static RedisClient client;
    protected RedisCommands<String, String> redis;

    @BeforeClass
    public static void setupClient() {
        client = newRedisClient();
    }

    @AfterClass
    public static void shutdownClient() {
        client.shutdown(0, 0, TimeUnit.MILLISECONDS);
    }

    protected static RedisClient newRedisClient() {
        return new RedisClient(host, port);
    }

    protected RedisCommands<String, String> connect() {
        return client.connect();
    }

    @Before
    public void openConnection() throws Exception {
        client.setOptions(new ClientOptions.Builder().build());
        redis = connect();
        redis.flushall();
        redis.flushdb();
    }

    @After
    public void closeConnection() throws Exception {
        redis.close();
    }

    public abstract class WithPasswordRequired {
        protected abstract void run(RedisClient client) throws Exception;

        public WithPasswordRequired() throws Exception {
            try {
                redis.configSet("requirepass", passwd);
                redis.auth(passwd);

                RedisClient client = newRedisClient();
                try {
                    run(client);
                } finally {
                    client.shutdown(0, 0, TimeUnit.MILLISECONDS);
                }
            } finally {

                redis.configSet("requirepass", "");
            }
        }
    }
}
