package com.lambdaworks.examples;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.resource.DefaultClientResources;
import com.lambdaworks.redis.resource.DirContextDnsResolver;

/**
 * @author Mark Paluch
 */
public class ConnectToElastiCacheMaster {

    public static void main(String[] args) {
        // Syntax: redis://[password@]host[:port][/databaseNumber]

        DefaultClientResources clientResources = new DefaultClientResources.Builder() //
                .dnsResolver(new DirContextDnsResolver()) // Does not cache DNS lookups
                .build();

        RedisClient redisClient = RedisClient.create(clientResources, "redis://password@localhost:6379/0");
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }
}
