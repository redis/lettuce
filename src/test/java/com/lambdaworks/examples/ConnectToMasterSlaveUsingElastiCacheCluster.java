package com.lambdaworks.examples;

import java.util.Arrays;
import java.util.List;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.masterslave.MasterSlave;
import com.lambdaworks.redis.masterslave.StatefulRedisMasterSlaveConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToMasterSlaveUsingElastiCacheCluster {

    public static void main(String[] args) {
        // Syntax: redis://[password@]host[:port][/databaseNumber]
        RedisClient redisClient = RedisClient.create();

        List<RedisURI> nodes = Arrays.asList(RedisURI.create("redis://host1"),
                RedisURI.create("redis://host2"),
                RedisURI.create("redis://host3"));

        StatefulRedisMasterSlaveConnection<String, String> connection = MasterSlave
                .connect(redisClient, new Utf8StringCodec(), nodes);
        connection.setReadFrom(ReadFrom.MASTER_PREFERRED);

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }
}
