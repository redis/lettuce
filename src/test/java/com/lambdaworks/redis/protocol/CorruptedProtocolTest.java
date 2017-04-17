package com.lambdaworks.redis.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI.Builder;
import com.lambdaworks.redis.api.sync.RedisCommands;

public class CorruptedProtocolTest {

    private MockRedisServer mockRedisServer;

    private RedisClient redisClient;

    private RedisCommands<String, String> connection;

    @Before
    public void before() {

        mockRedisServer = new MockRedisServer();
        mockRedisServer.startServer();
        redisClient = RedisClient.create(Builder.redis("localhost", mockRedisServer.port()).build());
        redisClient.setOptions(ClientOptions.builder().autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS).build());
        connection = redisClient.connect().sync();
    }

    @After
    public void after() {

        redisClient.shutdown();
        mockRedisServer.close();
    }

    @Test
    public void normalGetTest() {

        mockRedisServer.setResponses(
                "$-1\r\n",
                "$0\r\n\r\n",
                "$6\r\nfoobar\r\n");

        assertNull(connection.get("test1"));
        assertEquals("", connection.get("test2"));
        assertEquals("foobar", connection.get("test3"));
    }

    @Test(expected = RedisException.class)
    public void badProtocolFirstTimeTest() {

        mockRedisServer.setResponses("\r\nfoobar\r\n"); // simulate a corrupted case.

        connection.get("test4");
    }

    @Test
    public void badProtocolAndNextRequestTest() throws Exception {

        mockRedisServer.setResponses(
                "$3\r\n123\r\n",
                "\r\nfoobar\r\n"); // simulate a corrupted case.

        assertEquals("123", connection.get("test5"));
        try {
            connection.get("test6");
            fail();
        } catch (RedisException e) {
            // expected.
        }

        mockRedisServer.setResponses("$6\r\nfoobar\r\n");

        assertEquals("foobar", connection.get("test7"));
    }
}
