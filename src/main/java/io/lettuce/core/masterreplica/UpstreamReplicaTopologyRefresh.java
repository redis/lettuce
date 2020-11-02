/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.masterreplica;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

import reactor.core.publisher.Mono;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Utility to refresh the Master-Replica topology view based on {@link RedisNodeDescription}.
 *
 * @author Mark Paluch
 */
class UpstreamReplicaTopologyRefresh {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(UpstreamReplicaTopologyRefresh.class);

    private static final StringCodec CODEC = StringCodec.UTF8;

    private final NodeConnectionFactory nodeConnectionFactory;

    private final TopologyProvider topologyProvider;

    private final ScheduledExecutorService eventExecutors;

    UpstreamReplicaTopologyRefresh(RedisClient client, TopologyProvider topologyProvider) {
        this(new RedisClientNodeConnectionFactory(client), client.getResources().eventExecutorGroup(), topologyProvider);
    }

    UpstreamReplicaTopologyRefresh(NodeConnectionFactory nodeConnectionFactory, ScheduledExecutorService eventExecutors,
            TopologyProvider topologyProvider) {

        this.nodeConnectionFactory = nodeConnectionFactory;
        this.eventExecutors = eventExecutors;
        this.topologyProvider = topologyProvider;
    }

    /**
     * Load master replica nodes. Result contains an ordered list of {@link RedisNodeDescription}s. The sort key is the latency.
     * Nodes with lower latency come first.
     *
     * @param seed collection of {@link RedisURI}s
     * @return mapping between {@link RedisURI} and {@link Partitions}
     */
    public Mono<List<RedisNodeDescription>> getNodes(RedisURI seed) {

        CompletableFuture<List<RedisNodeDescription>> future = topologyProvider.getNodesAsync();

        Mono<List<RedisNodeDescription>> initialNodes = Mono.fromFuture(future).doOnNext(nodes -> {
            applyAuthenticationCredentials(nodes, seed);
        });

        return initialNodes.map(this::getConnections)
                .flatMap(asyncConnections -> asyncConnections.asMono(seed.getTimeout(), eventExecutors))
                .flatMap(connections -> {

                    Requests requests = connections.requestPing();

                    CompletionStage<List<RedisNodeDescription>> nodes = requests.getOrTimeout(seed.getTimeout(),
                            eventExecutors);

                    return Mono.fromCompletionStage(nodes).flatMap(it -> ResumeAfter.close(connections).thenEmit(it));
                });
    }

    /*
     * Establish connections asynchronously.
     */
    private AsyncConnections getConnections(Iterable<RedisNodeDescription> nodes) {

        List<RedisNodeDescription> nodeList = LettuceLists.newList(nodes);
        AsyncConnections connections = new AsyncConnections(nodeList);

        for (RedisNodeDescription node : nodeList) {

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
                        connection.async().clientSetname("lettuce#MasterReplicaTopologyRefresh");
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

    private static void applyAuthenticationCredentials(List<RedisNodeDescription> nodes, RedisURI seed) {

        for (RedisNodeDescription node : nodes) {
            node.getUri().applyAuthentication(seed);
        }
    }

}
