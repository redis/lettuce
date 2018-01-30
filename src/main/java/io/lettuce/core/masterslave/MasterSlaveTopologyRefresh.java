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

import static io.lettuce.core.masterslave.MasterSlaveUtils.findNodeByUri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Utility to refresh the Master-Slave topology view based on {@link RedisNodeDescription}.
 *
 * @author Mark Paluch
 */
class MasterSlaveTopologyRefresh {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MasterSlaveTopologyRefresh.class);
    private static final StringCodec CODEC = StringCodec.UTF8;

    private final NodeConnectionFactory nodeConnectionFactory;
    private final TopologyProvider topologyProvider;

    MasterSlaveTopologyRefresh(RedisClient client, TopologyProvider topologyProvider) {
        this(new ReflectiveNodeConnectionFactory(client), topologyProvider);
    }

    MasterSlaveTopologyRefresh(NodeConnectionFactory nodeConnectionFactory, TopologyProvider topologyProvider) {

        this.nodeConnectionFactory = nodeConnectionFactory;
        this.topologyProvider = topologyProvider;
    }

    /**
     * Load master slave nodes. Result contains an ordered list of {@link RedisNodeDescription}s. The sort key is the latency.
     * Nodes with lower latency come first.
     *
     * @param seed collection of {@link RedisURI}s
     * @return mapping between {@link RedisURI} and {@link Partitions}
     */
    public List<RedisNodeDescription> getNodes(RedisURI seed) {

        List<RedisNodeDescription> nodes = topologyProvider.getNodes();

        addPasswordIfNeeded(nodes, seed);

        AsyncConnections asyncConnections = getConnections(nodes);
        Connections connections = null;

        try {
            connections = asyncConnections.get(seed.getTimeout().toNanos(), TimeUnit.NANOSECONDS);
            Requests requestedPing = connections.requestPing();

            return getNodeSpecificViews(requestedPing, nodes, seed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        } finally {
            if (connections != null) {
                connections.close();
            }
        }
    }

    private void addPasswordIfNeeded(List<RedisNodeDescription> nodes, RedisURI seed) {

        if (seed.getPassword() != null && seed.getPassword().length != 0) {
            for (RedisNodeDescription node : nodes) {
                node.getUri().setPassword(new String(seed.getPassword()));
            }
        }
    }

    private List<RedisNodeDescription> getNodeSpecificViews(Requests requestedPing, List<RedisNodeDescription> nodes,
            RedisURI seed) throws InterruptedException {

        List<RedisNodeDescription> result = new ArrayList<>();

        long timeout = seed.getTimeout().toNanos();
        Map<RedisNodeDescription, Long> latencies = new HashMap<>();

        requestedPing.await(timeout, TimeUnit.NANOSECONDS);

        for (RedisNodeDescription node : nodes) {

            TimedAsyncCommand<String, String, String> future = requestedPing.getRequest(node.getUri());

            if (future == null || !future.isDone()) {
                continue;
            }

            RedisNodeDescription redisNodeDescription = findNodeByUri(nodes, node.getUri());
            latencies.put(redisNodeDescription, future.duration());
            result.add(redisNodeDescription);
        }

        TopologyComparators.LatencyComparator comparator = new TopologyComparators.LatencyComparator(latencies);

        result.sort(comparator);

        return result;
    }

    /*
     * Establish connections asynchronously.
     */
    private AsyncConnections getConnections(Iterable<RedisNodeDescription> nodes) {

        AsyncConnections connections = new AsyncConnections();

        for (RedisNodeDescription node : nodes) {

            RedisURI redisURI = node.getUri();
            String message = String.format("Unable to connect to %s", redisURI);
            try {
                CompletableFuture<StatefulRedisConnection<String, String>> connectionFuture = nodeConnectionFactory
                        .connectToNodeAsync(CODEC, redisURI);

                CompletableFuture<StatefulRedisConnection<String, String>> sync = new CompletableFuture<>();

                connectionFuture.whenComplete((connection, throwable) -> {

                    if (throwable != null) {

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
                        connection.async().clientSetname("lettuce#MasterSlaveTopologyRefresh");
                        sync.complete(connection);
                    }
                });

                connections.addConnection(redisURI, sync);
            } catch (RuntimeException e) {
                logger.warn(String.format(message, redisURI), e);
            }
        }

        return connections;
    }
}
