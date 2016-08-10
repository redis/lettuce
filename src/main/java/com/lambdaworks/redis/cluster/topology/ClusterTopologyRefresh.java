package com.lambdaworks.redis.cluster.topology;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.SocketAddressResolver;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Utility to refresh the cluster topology view based on {@link Partitions}.
 *
 * @author Mark Paluch
 */
public class ClusterTopologyRefresh {

    static final Utf8StringCodec CODEC = new Utf8StringCodec();
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterTopologyRefresh.class);

    private final NodeConnectionFactory nodeConnectionFactory;
    private final ClientResources clientResources;

    public ClusterTopologyRefresh(NodeConnectionFactory nodeConnectionFactory, ClientResources clientResources) {
        this.nodeConnectionFactory = nodeConnectionFactory;
        this.clientResources = clientResources;
    }

    /**
     * Load partition views from a collection of {@link RedisURI}s and return the view per {@link RedisURI}. Partitions contain
     * an ordered list of {@link RedisClusterNode}s. The sort key is latency. Nodes with lower latency come first.
     *
     * @param seed collection of {@link RedisURI}s
     * @param discovery {@literal true} to discover additional nodes
     * @return mapping between {@link RedisURI} and {@link Partitions}
     */
    public Map<RedisURI, Partitions> loadViews(Iterable<RedisURI> seed, boolean discovery) {

        Connections connections = getConnections(seed);
        Requests requestedTopology = connections.requestTopology();
        Requests requestedClients = connections.requestClients();

        long commandTimeoutNs = getCommandTimeoutNs(seed);

        try {
            NodeTopologyViews nodeSpecificViews = getNodeSpecificViews(requestedTopology, requestedClients, commandTimeoutNs);

            if (discovery) {
                Set<RedisURI> allKnownUris = nodeSpecificViews.getClusterNodes();
                Set<RedisURI> discoveredNodes = difference(allKnownUris, toSet(seed));

                if (!discoveredNodes.isEmpty()) {
                    Connections discoveredConnections = getConnections(discoveredNodes);
                    connections = connections.mergeWith(discoveredConnections);

                    requestedTopology = requestedTopology.mergeWith(discoveredConnections.requestTopology());
                    requestedClients = requestedClients.mergeWith(discoveredConnections.requestClients());

                    nodeSpecificViews = getNodeSpecificViews(requestedTopology, requestedClients, commandTimeoutNs);

                    return nodeSpecificViews.toMap();
                }
            }

            return nodeSpecificViews.toMap();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        } finally {
            connections.close();
        }
    }

    private Set<RedisURI> toSet(Iterable<RedisURI> seed) {
        return StreamSupport.stream(seed.spliterator(), false).collect(Collectors.toCollection(HashSet::new));
    }

    NodeTopologyViews getNodeSpecificViews(Requests requestedTopology, Requests requestedClients, long commandTimeoutNs)
            throws InterruptedException {

        List<RedisClusterNodeSnapshot> allNodes = new ArrayList<>();

        Map<String, Long> latencies = new HashMap<>();
        Map<String, Integer> clientCountByNodeId = new HashMap<>();

        long waitTime = requestedTopology.await(commandTimeoutNs, TimeUnit.NANOSECONDS);
        requestedClients.await(commandTimeoutNs - waitTime, TimeUnit.NANOSECONDS);

        Set<RedisURI> nodes = requestedTopology.nodes();

        List<NodeTopologyView> views = new ArrayList<>();
        for (RedisURI node : nodes) {

            try {
                NodeTopologyView nodeTopologyView = NodeTopologyView.from(node, requestedTopology, requestedClients);

                if (!nodeTopologyView.isAvailable()) {
                    continue;
                }

                List<RedisClusterNodeSnapshot> nodeWithStats = nodeTopologyView.getPartitions() //
                        .stream() //
                        .filter(ClusterTopologyRefresh::validNode) //
                        .map(RedisClusterNodeSnapshot::new).collect(Collectors.toList());

                for (RedisClusterNodeSnapshot partition : nodeWithStats) {

                    if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {

                        if (partition.getUri() == null) {
                            partition.setUri(node);
                        }

                        // record latency for later partition ordering
                        latencies.put(partition.getNodeId(), nodeTopologyView.getLatency());
                        clientCountByNodeId.put(partition.getNodeId(), nodeTopologyView.getConnectedClients());
                    }
                }

                allNodes.addAll(nodeWithStats);

                Partitions partitions = new Partitions();
                partitions.addAll(nodeWithStats);

                nodeTopologyView.setPartitions(partitions);

                views.add(nodeTopologyView);
            } catch (ExecutionException e) {
                logger.warn(String.format("Cannot retrieve partition view from %s, error: %s", node, e));
            }
        }

        for (RedisClusterNodeSnapshot node : allNodes) {
            node.setConnectedClients(clientCountByNodeId.get(node.getNodeId()));
            node.setLatencyNs(latencies.get(node.getNodeId()));
        }

        for (NodeTopologyView view : views) {
            Collections.sort(view.getPartitions().getPartitions(), TopologyComparators.LatencyComparator.INSTANCE);
            view.getPartitions().updateCache();
        }

        return new NodeTopologyViews(views);
    }

    private static boolean validNode(RedisClusterNode redisClusterNode) {

        if (redisClusterNode.is(RedisClusterNode.NodeFlag.NOADDR)) {
            return false;
        }

        if (redisClusterNode.getUri() == null || redisClusterNode.getUri().getPort() == 0
                || LettuceStrings.isEmpty(redisClusterNode.getUri().getHost())) {
            return false;
        }

        return true;
    }

    /*
     * Open connections where an address can be resolved.
     */
    private Connections getConnections(Iterable<RedisURI> redisURIs) {

        Connections connections = new Connections();

        for (RedisURI redisURI : redisURIs) {
            if (redisURI.getHost() == null || connections.connectedNodes().contains(redisURI)) {
                continue;
            }

            try {
                SocketAddress socketAddress = SocketAddressResolver.resolve(redisURI, clientResources.dnsResolver());
                StatefulRedisConnection<String, String> connection = nodeConnectionFactory.connectToNode(CODEC, socketAddress);
                connection.async().clientSetname("lettuce#ClusterTopologyRefresh");

                connections.addConnection(redisURI, connection);
            } catch (RedisConnectionException e) {

                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage(), e);
                } else {
                    logger.warn(e.getMessage());
                }
            } catch (RuntimeException e) {
                logger.warn(String.format("Cannot connect to %s", redisURI), e);
            }
        }
        return connections;
    }

    /**
     * Resolve a {@link RedisURI} from a map of cluster views by {@link Partitions} as key
     *
     * @param map the map
     * @param partitions the key
     * @return a {@link RedisURI} or null
     */
    public RedisURI getViewedBy(Map<RedisURI, Partitions> map, Partitions partitions) {

        for (Map.Entry<RedisURI, Partitions> entry : map.entrySet()) {
            if (entry.getValue() == partitions) {
                return entry.getKey();
            }
        }

        return null;
    }

    private static <E> Set<E> difference(Set<E> set1, Set<E> set2) {

        Set<E> result = set1.stream().filter(e -> !set2.contains(e)).collect(Collectors.toSet());
        result.addAll(set2.stream().filter(e -> !set1.contains(e)).collect(Collectors.toList()));

        return result;
    }

    private long getCommandTimeoutNs(Iterable<RedisURI> redisURIs) {

        RedisURI redisURI = redisURIs.iterator().next();
        return redisURI.getUnit().toNanos(redisURI.getTimeout());
    }
}
