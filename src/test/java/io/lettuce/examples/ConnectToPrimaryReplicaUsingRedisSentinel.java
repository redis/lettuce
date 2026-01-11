package io.lettuce.examples;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.primaryreplica.PrimaryReplica;
import io.lettuce.core.primaryreplica.StatefulRedisPrimaryReplicaConnection;

/**
 * @author yeong0jae
 * @since 7.3
 */
public class ConnectToPrimaryReplicaUsingRedisSentinel {

    public static void main(String[] args) {
        // Syntax: redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelPrimaryId
        RedisClient redisClient = RedisClient.create();

        StatefulRedisPrimaryReplicaConnection<String, String> connection = PrimaryReplica.connect(redisClient, StringCodec.UTF8,
                RedisURI.create("redis-sentinel://localhost:26379,localhost:26380/0#myprimary"));
        connection.setReadFrom(ReadFrom.PRIMARY_PREFERRED);

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }

}
