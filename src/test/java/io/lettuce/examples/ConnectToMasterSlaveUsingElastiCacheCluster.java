package io.lettuce.examples;

import java.util.Arrays;
import java.util.List;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;

/**
 * @author Mark Paluch
 */
public class ConnectToMasterSlaveUsingElastiCacheCluster {

    public static void main(String[] args) {

        // Syntax: redis://[password@]host[:port][/databaseNumber]
        RedisClient redisClient = RedisClient.create();

        List<RedisURI> nodes = Arrays.asList(RedisURI.create("redis://host1"), RedisURI.create("redis://host2"),
                RedisURI.create("redis://host3"));

        StatefulRedisMasterReplicaConnection<String, String> connection = MasterReplica.connect(redisClient, StringCodec.UTF8,
                nodes);
        connection.setReadFrom(ReadFrom.UPSTREAM_PREFERRED);

        System.out.println("Connected to Redis");

        connection.close();
        redisClient.shutdown();
    }

}
