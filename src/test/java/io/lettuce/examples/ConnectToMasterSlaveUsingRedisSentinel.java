package io.lettuce.examples;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToMasterSlaveUsingRedisSentinel {

    public static void main(String[] args) {
        // Syntax: redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId
        RedisClient redisClient = RedisClient.create();

        StatefulRedisMasterReplicaConnection<String, String> connection = MasterReplica.connect(redisClient, StringCodec.UTF8,
                RedisURI.create("redis-sentinel://localhost:26379,localhost:26380/0#mymaster"));
        connection.setReadFrom(ReadFrom.UPSTREAM_PREFERRED);

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }

}
