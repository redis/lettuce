package io.lettuce.examples;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

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
