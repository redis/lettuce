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
package io.lettuce.core.cluster.topology;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.SocketAddressResolver;
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

        long commandTimeoutNs = getCommandTimeoutNs(seed);

        Connections connections = null;
        try {
            connections = getConnections(seed).get(commandTimeoutNs, TimeUnit.NANOSECONDS);

            Requests requestedTopology = connections.requestTopology();
            Requests requestedClients = connections.requestClients();

            NodeTopologyViews nodeSpecificViews = getNodeSpecificViews(requestedTopology, requestedClients, commandTimeoutNs);

            if (discovery) {

                Set<RedisURI> allKnownUris = nodeSpecificViews.getClusterNodes();
                Set<RedisURI> discoveredNodes = difference(allKnownUris, toSet(seed));

                if (!discoveredNodes.isEmpty()) {
                    Connections discoveredConnections = getConnections(discoveredNodes).optionalGet(commandTimeoutNs,
                            TimeUnit.NANOSECONDS);
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
            if (connections != null) {
                connections.close();
            }
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

                nodeWithStats.stream() //
                        .filter(partition -> partition.is(RedisClusterNode.NodeFlag.MYSELF)) //
                        .forEach(partition -> {

                            if (partition.getUri() == null) {
                                partition.setUri(node);
                            }

                            // record latency for later partition ordering
                                latencies.put(partition.getNodeId(), nodeTopologyView.getLatency());
                                clientCountByNodeId.put(partition.getNodeId(), nodeTopologyView.getConnectedClients());
                            });

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
            view.getPartitions().getPartitions().sort(TopologyComparators.LatencyComparator.INSTANCE);
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
    private AsyncConnections getConnections(Iterable<RedisURI> redisURIs) throws InterruptedException {

        AsyncConnections connections = new AsyncConnections();

        for (RedisURI redisURI : redisURIs) {
            if (redisURI.getHost() == null || connections.connectedNodes().contains(redisURI)) {
                continue;
            }

            try {
                SocketAddress socketAddress = SocketAddressResolver.resolve(redisURI, clientResources.dnsResolver());

                ConnectionFuture<StatefulRedisConnection<String, String>> connectionFuture = nodeConnectionFactory
                        .connectToNodeAsync(CODEC, socketAddress);

                CompletableFuture<StatefulRedisConnection<String, String>> sync = new CompletableFuture<>();

                connectionFuture.whenComplete((connection, throwable) -> {

                    if (throwable != null) {

                        String message = String.format("Unable to connect to %s", socketAddress);
                        if (throwable instanceof RedisConnectionException) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(throwable.getMessage(), throwable);
                            } else {
                                logger.warn(throwable.getMessage());
                            }
                        } else {
                            logger.warn(message, throwable);
                        }

                        sync.completeExceptionally(new RedisConnectionException(message, throwable));
                    } else {
                        connection.async().clientSetname("lettuce#ClusterTopologyRefresh");
                        sync.complete(connection);
                    }
                });

                connections.addConnection(redisURI, sync);
            } catch (RuntimeException e) {
                logger.warn(String.format("Unable to connect to %s", redisURI), e);
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
        return redisURI.getTimeout().toNanos();
    }
}
