// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.lambdaworks.redis.api.sync.RedisCommands;

public abstract class AbstractRedisClientTest extends AbstractTest {

    protected static RedisClient client;
    protected RedisCommands<String, String> redis;

    @BeforeClass
    public static void setupClient() {
        client = DefaultRedisClient.get();
    }

    protected static RedisClient newRedisClient() {
        return new RedisClient(host, port);
    }

    protected RedisCommands<String, String> connect() {
        RedisCommands<String, String> connect = client.connect().sync();
        return connect;
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
        if (redis != null) {
            redis.close();
        }
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
                    FastShutdown.shutdown(client);
                }
            } finally {

                redis.configSet("requirepass", "");
            }
        }
    }
}
