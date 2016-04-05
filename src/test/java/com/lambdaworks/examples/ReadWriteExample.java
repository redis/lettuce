package com.lambdaworks.examples;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;

/**
 * @author Mark Paluch
 */
public class ReadWriteExample {

    public static void main(String[] args) {
        // Syntax: redis://[password@]host[:port][/databaseNumber]
        RedisClient redisClient = RedisClient.create(RedisURI.create("redis://password@localhost:6379/0"));
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        System.out.println("Connected to Redis");
        RedisCommands<String, String> sync = connection.sync();

        sync.set("foo", "bar");
        String value = sync.get("foo");
        System.out.println(value);

        connection.close();
        redisClient.shutdown();
    }
}
