package com.lambdaworks.examples;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisURI;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 18.06.15 09:17
 */
public class ReadWriteExample {

    public static void main(String[] args) {
        // Syntax: redis://[password@]host[:port][/databaseNumber]
        RedisClient redisClient = new RedisClient(RedisURI.create("redis://password@localhost:6379/0"));
        RedisConnection<String, String> connection = redisClient.connect();
        System.out.println("Connected to Redis");

        connection.set("foo", "bar");
        String value = connection.get("foo");
        System.out.println(value);

        connection.close();
        redisClient.shutdown();
    }
}
