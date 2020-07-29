/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.cluster.topology;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.topology.TopologyComparators.SortAction;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.internal.Exceptions;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.Timeout;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Utility to refresh the cluster topology view based on {@link Partitions}.
 *
 * @author Mark Paluch
 */
class DefaultClusterTopologyRefresh implements ClusterTopologyRefresh {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultClusterTopologyRefresh.class);

    private final NodeConnectionFactory nodeConnectionFactory;

    private final ClientResources clientResources;

    public DefaultClusterTopologyRefresh(NodeConnectionFactory nodeConnectionFactory, ClientResources clientResources) {
        this.nodeConnectionFactory = nodeConnectionFactory;
        this.clientResources = clientResources;
    }

    /**
     * Load partition views from a collection of {@link RedisURI}s and return the view per {@link RedisURI}. Partitions contain
     * an ordered list of {@link RedisClusterNode}s. The sort key is latency. Nodes with lower latency come first.
     *
     * @param seed collection of {@link RedisURI}s
     * @param connectTimeout connect timeout
     * @param discovery {@code true} to discover additional nodes
     * @return mapping between {@link RedisURI} and {@link Partitions}
     */
    public CompletionStage<Map<RedisURI, Partitions>> loadViews(Iterable<RedisURI> seed, Duration connectTimeout,
            boolean discovery) {

        if (!isEventLoopActive()) {
            return CompletableFuture.completedFuture(Collections.emptyMap());
        }

        long commandTimeoutNs = getCommandTimeoutNs(seed);
        ConnectionTracker tracker = new ConnectionTracker();
        long connectionTimeout = commandTimeoutNs + connectTimeout.toNanos();
        openConnections(tracker, seed, connectionTimeout, TimeUnit.NANOSECONDS);

        CompletableFuture<NodeTopologyViews> composition = tracker.whenComplete(map -> {
            return new Connections(clientResources, map);
        }).thenCompose(connections -> {

            Requests requestedTopology = connections.requestTopology(commandTimeoutNs, TimeUnit.NANOSECONDS);
            Requests requestedClients = connections.requestClients(commandTimeoutNs, TimeUnit.NANOSECONDS);
            return CompletableFuture.allOf(requestedTopology.allCompleted(), requestedClients.allCompleted())
                    .thenCompose(ignore -> {

                        NodeTopologyViews views = getNodeSpecificViews(requestedTopology, requestedClients);

                        if (discovery && isEventLoopActive()) {

                            Set<RedisURI> allKnownUris = views.getClusterNodes();
                            Set<RedisURI> discoveredNodes = difference(allKnownUris, toSet(seed));

                            if (discoveredNodes.isEmpty()) {
                                return CompletableFuture.completedFuture(views);
                            }

                            openConnections(tracker, discoveredNodes, connectionTimeout, TimeUnit.NANOSECONDS);

                            return tracker.whenComplete(map -> {
                                return new Connections(clientResources, map).retainAll(discoveredNodes);
                            }).thenCompose(newConnections -> {

                                Requests additionalTopology = newConnections
                                        .requestTopology(commandTimeoutNs, TimeUnit.NANOSECONDS).mergeWith(requestedTopology);
                                Requests additionalClients = newConnections
                                        .requestClients(commandTimeoutNs, TimeUnit.NANOSECONDS).mergeWith(requestedClients);
                                return CompletableFuture
                                        .allOf(additionalTopology.allCompleted(), additionalClients.allCompleted())
                                        .thenApply(ignore2 -> {

                                            return getNodeSpecificViews(additionalTopology, additionalClients);
                                        });
                            });
                        }

                        return CompletableFuture.completedFuture(views);
                    }).whenComplete((ignore, throwable) -> {

                        if (throwable != null) {
                            try {
                                tracker.close();
                            } catch (Exception e) {
                                logger.debug("Cannot close ClusterTopologyRefresh connections", e);
                            }
                        }
                    }).thenCompose((it) -> tracker.close().thenApply(ignore -> it)).thenCompose(it -> {

                        if (it.isEmpty()) {
                            Exception exception = tryFail(requestedTopology, tracker, seed);
                            return Futures.failed(exception);
                        }

                        return CompletableFuture.completedFuture(it);
                    });
        });

        return composition.thenApply(NodeTopologyViews::toMap);
    }

    private Exception tryFail(Requests requestedTopology, ConnectionTracker tracker, Iterable<RedisURI> seed) {

        Map<RedisURI, String> failures = new LinkedHashMap<>();
        CannotRetrieveClusterPartitions exception = new CannotRetrieveClusterPartitions(seed, failures);

        for (RedisURI node : requestedTopology.nodes()) {

            TimedAsyncCommand<String, String, String> request = requestedTopology.getRequest(node);
            if (request == null || !request.isCompletedExceptionally()) {
                continue;
            }

            Throwable cause = getException(request);
            if (cause != null) {
                failures.put(node, getExceptionDetail(cause));
                exception.addSuppressed(cause);
            }
        }

        for (Map.Entry<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> entry : tracker.connections
                .entrySet()) {

            CompletableFuture<StatefulRedisConnection<String, String>> future = entry.getValue();

            if (!future.isDone() || !future.isCompletedExceptionally()) {
                continue;
            }

            try {
                future.join();
            } catch (CompletionException e) {

                Throwable cause = e.getCause();
                if (cause != null) {
                    failures.put(entry.getKey(), getExceptionDetail(cause));
                    exception.addSuppressed(cause);
                }
            }
        }

        return exception;
    }

    private static String getExceptionDetail(Throwable exception) {

        if (exception instanceof RedisConnectionException && exception.getCause() instanceof IOException) {
            exception = exception.getCause();
        }

        return LettuceStrings.isNotEmpty(exception.getMessage()) ? exception.getMessage() : exception.toString();
    }

    private Set<RedisURI> toSet(Iterable<RedisURI> seed) {
        return StreamSupport.stream(seed.spliterator(), false).collect(Collectors.toCollection(HashSet::new));
    }

    NodeTopologyViews getNodeSpecificViews(Requests requestedTopology, Requests requestedClients) {

        List<RedisClusterNodeSnapshot> allNodes = new ArrayList<>();

        Map<String, Long> latencies = new HashMap<>();
        Map<String, Integer> clientCountByNodeId = new HashMap<>();

        Set<RedisURI> nodes = requestedTopology.nodes();

        List<NodeTopologyView> views = new ArrayList<>();
        for (RedisURI nodeUri : nodes) {

            try {
                NodeTopologyView nodeTopologyView = NodeTopologyView.from(nodeUri, requestedTopology, requestedClients);

                if (!nodeTopologyView.isAvailable()) {
                    continue;
                }

                RedisClusterNode node = nodeTopologyView.getOwnPartition();
                if (node.getUri() == null) {
                    node.setUri(nodeUri);
                } else {
                    node.addAlias(nodeUri);
                }

                List<RedisClusterNodeSnapshot> nodeWithStats = new ArrayList<>(nodeTopologyView.getPartitions().size());

                for (RedisClusterNode partition : nodeTopologyView.getPartitions()) {

                    if (validNode(partition)) {
                        RedisClusterNodeSnapshot redisClusterNodeSnapshot = new RedisClusterNodeSnapshot(partition);
                        nodeWithStats.add(redisClusterNodeSnapshot);

                        if (partition.is(RedisClusterNode.NodeFlag.MYSELF)) {

                            // record latency for later partition ordering
                            latencies.put(partition.getNodeId(), nodeTopologyView.getLatency());
                            clientCountByNodeId.put(partition.getNodeId(), nodeTopologyView.getConnectedClients());
                        }
                    }
                }

                allNodes.addAll(nodeWithStats);

                Partitions partitions = new Partitions();
                partitions.addAll(nodeWithStats);

                nodeTopologyView.setPartitions(partitions);

                views.add(nodeTopologyView);
            } catch (CompletionException e) {
                logger.warn(String.format("Cannot retrieve partition view from %s, error: %s", nodeUri, e));
            }
        }

        for (RedisClusterNodeSnapshot node : allNodes) {
            node.setConnectedClients(clientCountByNodeId.get(node.getNodeId()));
            node.setLatencyNs(latencies.get(node.getNodeId()));
        }

        SortAction sortAction = SortAction.getSortAction();
        for (NodeTopologyView view : views) {

            sortAction.sort(view.getPartitions());
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
    private void openConnections(ConnectionTracker tracker, Iterable<RedisURI> redisURIs, long timeout, TimeUnit timeUnit) {

        for (RedisURI redisURI : redisURIs) {

            if (redisURI.getHost() == null || tracker.contains(redisURI) || !isEventLoopActive()) {
                continue;
            }

            try {
                SocketAddress socketAddress = clientResources.socketAddressResolver().resolve(redisURI);

                ConnectionFuture<StatefulRedisConnection<String, String>> connectionFuture = nodeConnectionFactory
                        .connectToNodeAsync(StringCodec.UTF8, socketAddress);

                // Note: timeout skew due to potential socket address resolution and connection work possible.

                CompletableFuture<StatefulRedisConnection<String, String>> sync = new CompletableFuture<>();
                Timeout cancelTimeout = clientResources.timer().newTimeout(it -> {

                    String message = String.format("Unable to connect to [%s]: Timeout after %s", socketAddress,
                            ExceptionFactory.formatTimeout(Duration.ofNanos(timeUnit.toNanos(timeout))));
                    sync.completeExceptionally(new RedisConnectionException(message));
                }, timeout, timeUnit);

                connectionFuture.whenComplete((connection, throwable) -> {

                    cancelTimeout.cancel();

                    if (throwable != null) {

                        Throwable throwableToUse = Exceptions.unwrap(throwable);

                        String message = String.format("Unable to connect to [%s]: %s", socketAddress,
                                throwableToUse.getMessage() != null ? throwableToUse.getMessage() : throwableToUse.toString());
                        if (throwableToUse instanceof RedisConnectionException || throwableToUse instanceof IOException) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(message, throwableToUse);
                            } else {
                                logger.warn(message);
                            }
                        } else {
                            logger.warn(message, throwableToUse);
                        }

                        sync.completeExceptionally(new RedisConnectionException(message, throwableToUse));
                    } else {
                        connection.async().clientSetname("lettuce#ClusterTopologyRefresh");

                        // avoid leaking resources
                        if (!sync.complete(connection)) {
                            connection.close();
                        }
                    }
                });

                tracker.addConnection(redisURI, sync);
            } catch (RuntimeException e) {
                logger.warn(String.format("Unable to connect to [%s]", redisURI), e);
            }
        }
    }

    private boolean isEventLoopActive() {

        EventExecutorGroup eventExecutors = clientResources.eventExecutorGroup();

        return !eventExecutors.isShuttingDown();
    }

    private static Set<RedisURI> difference(Set<RedisURI> allKnown, Set<RedisURI> seed) {

       Set<RedisURI> result = new TreeSet<>(TopologyComparators.RedisURIComparator.INSTANCE);

        for (RedisURI e : allKnown) {
            if (!seed.contains(e)) {
                result.add(e);
            }
        }

        return result;
    }

    private static long getCommandTimeoutNs(Iterable<RedisURI> redisURIs) {

        RedisURI redisURI = redisURIs.iterator().next();
        return redisURI.getTimeout().toNanos();
    }

    /**
     * Retrieve the exception from a {@link Future}.
     *
     * @param future
     * @return
     */
    private static Throwable getException(Future<?> future) {

        try {
            future.get();
        } catch (Exception e) {
            return Exceptions.bubble(e);
        }

        return null;
    }

    static class ConnectionTracker {

        private final Map<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> connections = new LinkedHashMap<>();

        public void addConnection(RedisURI uri, CompletableFuture<StatefulRedisConnection<String, String>> future) {
            CompletableFuture<StatefulRedisConnection<String, String>> existing = connections.put(uri, future);

            if(existing != null){
                System.out.println("faiiil!1");
            }
        }

        @SuppressWarnings("rawtypes")
        public CompletableFuture<Void> close() {

            CompletableFuture[] futures = connections.values().stream()
                    .map(it -> it.thenCompose(StatefulConnection::closeAsync).exceptionally(ignore -> null))
                    .toArray(CompletableFuture[]::new);

            return CompletableFuture.allOf(futures);
        }

        public boolean contains(RedisURI uri) {
            return connections.containsKey(uri);
        }

        public <T> CompletableFuture<T> whenComplete(
                Function<? super Map<RedisURI, StatefulRedisConnection<String, String>>, ? extends T> mappingFunction) {

            int expectedCount = connections.size();
            AtomicInteger latch = new AtomicInteger();
            CompletableFuture<T> continuation = new CompletableFuture<>();

            for (Map.Entry<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> entry : connections
                    .entrySet()) {

                CompletableFuture<StatefulRedisConnection<String, String>> future = entry.getValue();

                future.whenComplete((it, ex) -> {

                    if (latch.incrementAndGet() == expectedCount) {

                        try {
                            continuation.complete(mappingFunction.apply(collectConnections()));
                        } catch (RuntimeException e) {
                            continuation.completeExceptionally(e);
                        }
                    }
                });
            }

            return continuation;
        }

        protected Map<RedisURI, StatefulRedisConnection<String, String>> collectConnections() {

            Map<RedisURI, StatefulRedisConnection<String, String>> activeConnections = new LinkedHashMap<>();

            for (Map.Entry<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> entry : connections
                    .entrySet()) {

                CompletableFuture<StatefulRedisConnection<String, String>> future = entry.getValue();
                if (future.isDone() && !future.isCompletedExceptionally()) {
                    activeConnections.put(entry.getKey(), future.join());
                }
            }
            return activeConnections;
        }

    }

    @SuppressWarnings("serial")
    static class CannotRetrieveClusterPartitions extends RedisException {

        private final Map<RedisURI, String> failure;

        public CannotRetrieveClusterPartitions(Iterable<RedisURI> seedNodes, Map<RedisURI, String> failure) {
            super(String.format("Cannot retrieve cluster partitions from %s", seedNodes));
            this.failure = failure;
        }

        @Override
        public String getMessage() {

            StringJoiner joiner = new StringJoiner(SystemPropertyUtil.get("line.separator", "\n"));

            if (!failure.isEmpty()) {

                joiner.add(super.getMessage()).add("");
                joiner.add("Details:");

                for (Map.Entry<RedisURI, String> entry : failure.entrySet()) {
                    joiner.add(String.format("\t[%s]: %s", entry.getKey(), entry.getValue()));
                }

                joiner.add("");
            }

            return joiner.toString();
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }

    }

}
