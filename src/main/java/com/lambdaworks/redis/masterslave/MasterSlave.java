package com.lambdaworks.redis.masterslave;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RedisNodeDescription;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.*;

/**
 * Master-Slave connection API.
 * <p>
 * This API allows connections to Redis Master/Slave setups which run either Standalone or are managed by Redis Sentinel.
 * Master-Slave connections incorporate topology discovery and source selection for read operations using
 * {@link com.lambdaworks.redis.ReadFrom}. Regular Standalone connections using {@link RedisClient#connect()} are single-node
 * connections without balancing/topology discovery.
 * </p>
 * <p>
 *
 * Connections can be obtained by providing the {@link RedisClient}, a {@link RedisURI} and a {@link RedisCodec}.
 * 
 * <pre>
 *  &#064;code
 *   RedisClient client = RedisClient.create();
 *   StatefulRedisMasterSlaveConnection<String, String> connection = MasterSlave.connect(client,
 *                                                                      RedisURI.create("redis://localhost"),
 *                                                                      new Utf8StringCodec());
 *   // ...
 *
 *   connection.close();
 *   client.shutdown();
 *   }
 * </pre>
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
 * <li>{@link SentinelTopologyProvider}: Topology lookup using the Redis Sentinel API. In particular {@code SENTINEL MASTER} and
 * {@code SENTINEL SLAVES} output.</li>
 * </ul>
 *
 * <p>
 * Topology updates
 * </p>
 * <ul>
 * <li>Standalone Master/Slave: Performs a one-time topology lookup which remains static afterwards</li>
 * <li>Redis Sentinel: Subscribes to all Sentinels and listens for Pub/Sub messages to trigger topology refreshing</li>
 * </ul>
 * </p>
 *
 * @author Mark Paluch
 * @since 4.1
 */
public class MasterSlave {

    private final static InternalLogger LOG = InternalLoggerFactory.getInstance(MasterSlave.class);

    /**
     * Open a new connection to a Redis Master-Slave server/servers using the supplied {@link RedisURI} and the supplied
     * {@link RedisCodec codec} to encode/decode keys.
     * <p>
     * This {@link MasterSlave} performs auto-discovery of nodes using either Redis Sentinel or Master/Slave. A {@link RedisURI}
     * can point to either a master or a slave host.
     * </p>
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

        if (isSentinel(redisURI)) {
            return connectSentinel(redisClient, codec, redisURI);
        } else {
            return connectMasterSlave(redisClient, codec, redisURI);
        }
    }

    private static <K, V> StatefulRedisMasterSlaveConnection<K, V> connectSentinel(RedisClient redisClient,
            RedisCodec<K, V> codec, RedisURI redisURI) {

        TopologyProvider topologyProvider = new SentinelTopologyProvider(redisURI.getSentinelMasterId(), redisClient, redisURI);
        SentinelTopologyRefresh sentinelTopologyRefresh = new SentinelTopologyRefresh(redisClient,
                redisURI.getSentinelMasterId(), redisURI.getSentinels());

        MasterSlaveTopologyRefresh refresh = new MasterSlaveTopologyRefresh(redisClient, topologyProvider);
        MasterSlaveConnectionProvider<K, V> connectionProvider = new MasterSlaveConnectionProvider<>(redisClient, codec,
                redisURI, Collections.emptyMap());

        connectionProvider.setKnownNodes(refresh.getNodes(redisURI));

        MasterSlaveChannelWriter<K, V> channelWriter = new MasterSlaveChannelWriter<>(connectionProvider);
        StatefulRedisMasterSlaveConnectionImpl<K, V> connection = new StatefulRedisMasterSlaveConnectionImpl<>(channelWriter,
                codec, redisURI.getTimeout(), redisURI.getUnit());

        Runnable runnable = () -> {
            try {

                LOG.debug("Refreshing topology");
                List<RedisNodeDescription> nodes = refresh.getNodes(redisURI);

                LOG.debug("New topology: {}", nodes);
                connectionProvider.setKnownNodes(nodes);
            } catch (Exception e) {
                LOG.error("Error during background refresh", e);
            }
        };

        try {
            connection.registerCloseables(new ArrayList<>(), sentinelTopologyRefresh);
            sentinelTopologyRefresh.bind(runnable);
        } catch (RuntimeException e) {

            connection.close();
            throw e;
        }

        return connection;
    }

    private static <K, V> StatefulRedisMasterSlaveConnection<K, V> connectMasterSlave(RedisClient redisClient,
            RedisCodec<K, V> codec, RedisURI redisURI) {

        Map<RedisURI, StatefulRedisConnection<K, V>> initialConnections = new HashMap<>();

        try {

            StatefulRedisConnection<K, V> nodeConnection = redisClient.connect(codec, redisURI);
            initialConnections.put(redisURI, nodeConnection);

            TopologyProvider topologyProvider = new MasterSlaveTopologyProvider(nodeConnection, redisURI);

            List<RedisNodeDescription> nodes = topologyProvider.getNodes();
            RedisNodeDescription node = getConnectedNode(redisURI, nodes);

            if (node.getRole() != RedisInstance.Role.MASTER) {

                RedisNodeDescription master = lookupMaster(nodes);
                nodeConnection = redisClient.connect(codec, master.getUri());
                initialConnections.put(master.getUri(), nodeConnection);
                topologyProvider = new MasterSlaveTopologyProvider(nodeConnection, master.getUri());
            }

            MasterSlaveTopologyRefresh refresh = new MasterSlaveTopologyRefresh(redisClient, topologyProvider);
            MasterSlaveConnectionProvider<K, V> connectionProvider = new MasterSlaveConnectionProvider<>(redisClient, codec,
                    redisURI, initialConnections);

            connectionProvider.setKnownNodes(refresh.getNodes(redisURI));

            MasterSlaveChannelWriter<K, V> channelWriter = new MasterSlaveChannelWriter<>(connectionProvider);

            StatefulRedisMasterSlaveConnectionImpl<K, V> connection = new StatefulRedisMasterSlaveConnectionImpl<>(
                    channelWriter, codec, redisURI.getTimeout(), redisURI.getUnit());

            return connection;

        } catch (RuntimeException e) {
            for (StatefulRedisConnection<K, V> connection : initialConnections.values()) {
                connection.close();
            }
            throw e;
        }
    }

    private static RedisNodeDescription lookupMaster(List<RedisNodeDescription> nodes) {

        Optional<RedisNodeDescription> first = nodes.stream().filter(n -> n.getRole() == RedisInstance.Role.MASTER).findFirst();
        return first.orElseThrow(() -> new IllegalStateException("Cannot lookup master from " + nodes));
    }

    private static RedisNodeDescription getConnectedNode(RedisURI redisURI, List<RedisNodeDescription> nodes) {

        Optional<RedisNodeDescription> first = nodes.stream().filter(n -> equals(redisURI, n)).findFirst();
        return first.orElseThrow(
                () -> new IllegalStateException("Cannot lookup node descriptor for connected node at " + redisURI));
    }

    private static boolean equals(RedisURI redisURI, RedisNodeDescription node) {
        return node.getUri().getHost().equals(redisURI.getHost()) && node.getUri().getPort() == redisURI.getPort();
    }

    private static boolean isSentinel(RedisURI redisURI) {
        return !redisURI.getSentinels().isEmpty();
    }

}
