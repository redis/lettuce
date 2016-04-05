package com.lambdaworks.redis.masterslave;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Master-Slave connection API.
 * <p>
 * This API allows connections to Redis Master/Slave setups which run either Standalone or are managed by Redis Sentinel.
 * Master-Slave connections incorporate topology discovery and source selection for read operations using
 * {@link com.lambdaworks.redis.ReadFrom}. Regular Standalone connections using {@link RedisClient#connect()} are single-node
 * connections without balancing/topology discovery.
 * </p>
 * <p>
 * Connections can be obtained by providing the {@link RedisClient}, a {@link RedisURI} and a {@link RedisCodec}. <code>
 *   RedisClient client = RedisClient.create();
 *   StatefulRedisMasterSlaveConnection<String, String> connection = MasterSlave.connect(client,
 *                                                                      RedisURI.create("redis://localhost"),
 *                                                                      new Utf8StringCodec());
 *   // ...
 *
 *   connection.close();
 *   client.shutdown();
 *   </code>
 * </p>
 * <h3>Topology discovery</h3>
 * <p>
 * Master-Slave topologies are either static or semi-static. Redis Standalone instances with attached slaves feature no
 * failover/HA mechanism and are static setups. Redis Sentinel managed instances are controlled by Redis Sentinel and allow
 * failover (which include master promotion). The {@link MasterSlave} API supports both mechanisms. The topology is provided by
 * a {@link TopologyProvider}:
 *
 * <ul>
 * <li>{@link MasterSlaveTopologyProvider}: Topology lookup using the {@code INFO REPLICATION} output. Slaves are listed as
 * {@code slaveN=...} entries.</li>
 * <li>{@link SentinelTopologyProvider}: Topology lookup using the Redis Sentinel API. In particular {@code SENTINEL SLAVES}
 * output.</li>
 * </ul>
 *
 * The topology is discovered once during the connection phase but is not updated afterwards.
 * </p>
 *
 * @author Mark Paluch
 * @since 4.1
 */
public class MasterSlave {

    /**
     * Open a new connection to a Redis Master-Slave server/servers using the supplied {@link RedisURI} and the supplied
     * {@link RedisCodec codec} to encode/decode keys.
     *
     * @param redisClient the Redis client
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param redisURI the Redis server to connect to, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     */
    public static <K, V> StatefulRedisMasterSlaveConnection<K, V> connect(RedisClient redisClient, RedisCodec<K, V> codec,
            RedisURI redisURI) {

        StatefulRedisConnection<K, V> masterConnection = redisClient.connect(codec, redisURI);
        TopologyProvider topologyProvider;
        if (redisURI.getSentinels().isEmpty()) {
            topologyProvider = new MasterSlaveTopologyProvider(masterConnection, redisURI);
        } else {
            topologyProvider = new SentinelTopologyProvider(redisURI.getSentinelMasterId(), redisClient, redisURI);
        }

        MasterSlaveConnectionProvider<K, V> connectionProvider = new MasterSlaveConnectionProvider<>(redisClient, codec,
                masterConnection, redisURI);

        MasterSlaveTopologyRefresh refresh = new MasterSlaveTopologyRefresh(redisClient, topologyProvider);
        connectionProvider.setKnownNodes(refresh.getNodes(redisURI));

        MasterSlaveChannelWriter<K, V> channelWriter = new MasterSlaveChannelWriter<>(connectionProvider);

        StatefulRedisMasterSlaveConnectionImpl<K, V> connection = new StatefulRedisMasterSlaveConnectionImpl<>(channelWriter,
                codec, redisURI.getTimeout(), redisURI.getUnit());

        return connection;
    }

}
