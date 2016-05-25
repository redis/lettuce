package com.lambdaworks.examples;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.masterslave.MasterSlave;
import com.lambdaworks.redis.masterslave.StatefulRedisMasterSlaveConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToMasterSlaveUsingRedisSentinel {

    public static void main(String[] args) {
        // Syntax: redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId
        RedisClient redisClient = RedisClient.create();

        StatefulRedisMasterSlaveConnection<String, String> connection = MasterSlave.connect(redisClient, new Utf8StringCodec(),
                RedisURI.create("redis-sentinel://localhost:26379,localhost:26380/0#mymaster"));
        connection.setReadFrom(ReadFrom.MASTER_PREFERRED);

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }
}
