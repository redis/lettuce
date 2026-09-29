package io.lettuce.examples;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;

/**
 * Connect to a Redis Enterprise database through its discovery service, which speaks the Sentinel protocol.
 * <p>
 * Redis Enterprise runs the discovery service on port 8001 of every cluster node and publishes {@code +switch-master} whenever
 * a database's endpoint moves to another node, so the connection follows the endpoint without the application re-resolving
 * anything.
 *
 * @author Redis
 */
public class ConnectToRedisEnterpriseUsingMasterReplica {

    public static void main(String[] args) {
        // Syntax: redis-sentinel://[[username:]password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId
        //
        // - The sentinel hosts are the cluster nodes on port 8001. List them all: the discovery service on any node
        // answers for every database in the cluster, so several entries give the lookup itself redundancy.
        // - The sentinelMasterId is the Redis Enterprise *database name*, not a bdb id.
        // - Credentials are the database credentials. RedisURI applies the URI userinfo to the data connection and builds
        // the per-sentinel entries without it, which is what this needs: the discovery service implements no AUTH, and
        // its HELLO rejects arguments past the protocol version.
        RedisURI uri = RedisURI.create("redis-sentinel://default:secret@node1.mycluster.redislabs.com:8001,"
                + "node2.mycluster.redislabs.com:8001,node3.mycluster.redislabs.com:8001#mydatabase");

        RedisClient redisClient = RedisClient.create();

        StatefulRedisMasterReplicaConnection<String, String> connection = MasterReplica.connect(redisClient, StringCodec.UTF8,
                uri);

        // A Redis Enterprise endpoint is served by a proxy, so the discovery service reports no replicas and the topology
        // is a single upstream node. Replica reads are therefore not available over this connection.
        connection.setReadFrom(ReadFrom.UPSTREAM);

        System.out.println("Connected to Redis Enterprise: " + connection.sync().ping());

        // Nothing further is required to survive an endpoint move - a node going into maintenance mode, a node being
        // drained or removed. The discovery service announces the new endpoint address and the connection is re-routed
        // to it.

        connection.close();
        redisClient.shutdown();
    }

}
