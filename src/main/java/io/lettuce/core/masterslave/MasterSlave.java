/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.masterslave;

import java.util.*;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Master-Slave connection API.
 * <p>
 * This API allows connections to Redis Master/Slave setups which run either in a static Master/Slave setup or are managed by
 * Redis Sentinel. Master-Slave connections can discover topologies and select a source for read operations using
 * {@link io.lettuce.core.ReadFrom}.
 * </p>
 * <p>
 *
 * Connections can be obtained by providing the {@link RedisClient}, a {@link RedisURI} and a {@link RedisCodec}.
 *
 * <pre class="code">
 * RedisClient client = RedisClient.create();
 * StatefulRedisMasterSlaveConnection&lt;String, String&gt; connection = MasterSlave.connect(client,
 *         RedisURI.create(&quot;redis://localhost&quot;), StringCodec.UTF8);
 * // ...
 *
 * connection.close();
 * client.shutdown();
 * </pre>
 *
 * </p>
 * <h3>Topology Discovery</h3>
 * <p>
 * Master-Slave topologies are either static or semi-static. Redis Standalone instances with attached slaves provide no
 * failover/HA mechanism. Redis Sentinel managed instances are controlled by Redis Sentinel and allow failover (which include
 * master promotion). The {@link MasterSlave} API supports both mechanisms. The topology is provided by a
 * {@link TopologyProvider}:
 *
 * <ul>
 * <li>{@link MasterSlaveTopologyProvider}: Dynamic topology lookup using the {@code INFO REPLICATION} output. Slaves are listed
 * as {@code slaveN=...} entries. The initial connection can either point to a master or a slave and the topology provider will
 * discover nodes. The connection needs to be re-established outside of lettuce in a case of Master/Slave failover or topology
 * changes.</li>
 * <li>{@link StaticMasterSlaveTopologyProvider}: Topology is defined by the list of {@link RedisURI URIs} and the {@code ROLE}
 * output. MasterSlave uses only the supplied nodes and won't discover additional nodes in the setup. The connection needs to be
 * re-established outside of lettuce in a case of Master/Slave failover or topology changes.</li>
 * <li>{@link SentinelTopologyProvider}: Dynamic topology lookup using the Redis Sentinel API. In particular,
 * {@code SENTINEL MASTER} and {@code SENTINEL SLAVES} output. Master/Slave failover is handled by lettuce.</li>
 * </ul>
 *
 * <p>
 * Topology Updates
 * </p>
 * <ul>
 * <li>Standalone Master/Slave: Performs a one-time topology lookup which remains static afterward</li>
 * <li>Redis Sentinel: Subscribes to all Sentinels and listens for Pub/Sub messages to trigger topology refreshing</li>
 * </ul>
 * </p>
 *
 * @author Mark Paluch
 * @since 4.1
 */
public class MasterSlave {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(MasterSlave.class);

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

        LettuceAssert.notNull(redisClient, "RedisClient must not be null");
        LettuceAssert.notNull(codec, "RedisCodec must not be null");
        LettuceAssert.notNull(redisURI, "RedisURI must not be null");

        if (isSentinel(redisURI)) {
            return connectSentinel(redisClient, codec, redisURI);
        } else {
            return connectMasterSlave(redisClient, codec, redisURI);
        }
    }

    /**
     * Open a new connection to a Redis Master-Slave server/servers using the supplied {@link RedisURI} and the supplied
     * {@link RedisCodec codec} to encode/decode keys.
     * <p>
     * This {@link MasterSlave} performs auto-discovery of nodes if the URI is a Redis Sentinel URI. Master/Slave URIs will be
     * treated as static topology and no additional hosts are discovered in such case. Redis Standalone Master/Slave will
     * discover the roles of the supplied {@link RedisURI URIs} and issue commands to the appropriate node.
     * </p>
     *
     * @param redisClient the Redis client
     * @param codec Use this codec to encode/decode keys and values, must not be {@literal null}
     * @param redisURIs the Redis server to connect to, must not be {@literal null}
     * @param <K> Key type
     * @param <V> Value type
     * @return A new connection
     */
    public static <K, V> StatefulRedisMasterSlaveConnection<K, V> connect(RedisClient redisClient, RedisCodec<K, V> codec,
            Iterable<RedisURI> redisURIs) {

        LettuceAssert.notNull(redisClient, "RedisClient must not be null");
        LettuceAssert.notNull(codec, "RedisCodec must not be null");
        LettuceAssert.notNull(redisURIs, "RedisURIs must not be null");

        List<RedisURI> uriList = LettuceLists.newList(redisURIs);
        LettuceAssert.isTrue(!uriList.isEmpty(), "RedisURIs must not be empty");

        if (isSentinel(uriList.get(0))) {
            return connectSentinel(redisClient, codec, uriList.get(0));
        } else {
            return connectStaticMasterSlave(redisClient, codec, uriList);
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
                codec, redisURI.getTimeout());

        connection.setOptions(redisClient.getOptions());

        Runnable runnable = () -> {
            try {

                LOG.debug("Refreshing topology");
                List<RedisNodeDescription> nodes = refresh.getNodes(redisURI);

                if (nodes.isEmpty()) {
                    LOG.warn("Topology refresh returned no nodes from {}", redisURI);
                }

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
                    channelWriter, codec, redisURI.getTimeout());

            connection.setOptions(redisClient.getOptions());

            return connection;

        } catch (RuntimeException e) {
            for (StatefulRedisConnection<K, V> connection : initialConnections.values()) {
                connection.close();
            }
            throw e;
        }
    }

    private static <K, V> StatefulRedisMasterSlaveConnection<K, V> connectStaticMasterSlave(RedisClient redisClient,
            RedisCodec<K, V> codec, Iterable<RedisURI> redisURIs) {

        Map<RedisURI, StatefulRedisConnection<K, V>> initialConnections = new HashMap<>();

        try {
            TopologyProvider topologyProvider = new StaticMasterSlaveTopologyProvider(redisClient, redisURIs);

            RedisURI seedNode = redisURIs.iterator().next();

            MasterSlaveTopologyRefresh refresh = new MasterSlaveTopologyRefresh(redisClient, topologyProvider);
            MasterSlaveConnectionProvider<K, V> connectionProvider = new MasterSlaveConnectionProvider<>(redisClient, codec,
                    seedNode, initialConnections);

            List<RedisNodeDescription> nodes = refresh.getNodes(seedNode);
            if (nodes.isEmpty()) {
                throw new RedisException(String.format("Cannot determine topology from %s", redisURIs));
            }

            connectionProvider.setKnownNodes(nodes);

            MasterSlaveChannelWriter<K, V> channelWriter = new MasterSlaveChannelWriter<>(connectionProvider);

            StatefulRedisMasterSlaveConnectionImpl<K, V> connection = new StatefulRedisMasterSlaveConnectionImpl<>(
                    channelWriter, codec, seedNode.getTimeout());

            connection.setOptions(redisClient.getOptions());

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
        return first.orElseThrow(() -> new IllegalStateException("Cannot lookup node descriptor for connected node at "
                + redisURI));
    }

    private static boolean equals(RedisURI redisURI, RedisNodeDescription node) {
        return node.getUri().getHost().equals(redisURI.getHost()) && node.getUri().getPort() == redisURI.getPort();
    }

    private static boolean isSentinel(RedisURI redisURI) {
        return !redisURI.getSentinels().isEmpty();
    }

}
