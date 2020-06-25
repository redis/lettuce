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
package io.lettuce.core.cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterNodeConnectionFactory.ConnectionKey;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.AsyncConnectionProvider;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Connection provider with built-in connection caching.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class PooledClusterConnectionProvider<K, V> implements ClusterConnectionProvider, AsyncClusterConnectionProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PooledClusterConnectionProvider.class);

    // Contains NodeId-identified and HostAndPort-identified connections.
    private final Object stateLock = new Object();

    private final boolean debugEnabled = logger.isDebugEnabled();

    private final CompletableFuture<StatefulRedisConnection<K, V>> writers[] = new CompletableFuture[SlotHash.SLOT_COUNT];

    private final CompletableFuture<StatefulRedisConnection<K, V>> readers[][] = new CompletableFuture[SlotHash.SLOT_COUNT][];

    private final RedisClusterClient redisClusterClient;

    private final ClusterNodeConnectionFactory<K, V> connectionFactory;

    private final RedisChannelWriter clusterWriter;

    private final ClusterEventListener clusterEventListener;

    private final RedisCodec<K, V> redisCodec;

    private final AsyncConnectionProvider<ConnectionKey, StatefulRedisConnection<K, V>, ConnectionFuture<StatefulRedisConnection<K, V>>> connectionProvider;

    private Partitions partitions;

    private boolean autoFlushCommands = true;

    private ReadFrom readFrom;

    public PooledClusterConnectionProvider(RedisClusterClient redisClusterClient, RedisChannelWriter clusterWriter,
            RedisCodec<K, V> redisCodec, ClusterEventListener clusterEventListener) {

        this.redisCodec = redisCodec;
        this.redisClusterClient = redisClusterClient;
        this.clusterWriter = clusterWriter;
        this.clusterEventListener = clusterEventListener;
        this.connectionFactory = new NodeConnectionPostProcessor(getConnectionFactory(redisClusterClient));
        this.connectionProvider = new AsyncConnectionProvider<>(this.connectionFactory);
    }

    @Override
    public StatefulRedisConnection<K, V> getConnection(Intent intent, int slot) {

        try {
            return getConnectionAsync(intent, slot).get();
        } catch (RedisException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RedisCommandInterruptedException(e);
        } catch (ExecutionException e) {
            throw new RedisException(e.getCause());
        } catch (RuntimeException e) {
            throw new RedisException(e);
        }
    }

    @Override
    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(Intent intent, int slot) {

        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ", " + slot + ")");
        }

        if (intent == Intent.READ && readFrom != null && readFrom != ReadFrom.MASTER) {
            return getReadConnection(slot);
        }

        return getWriteConnection(slot).toCompletableFuture();
    }

    private CompletableFuture<StatefulRedisConnection<K, V>> getWriteConnection(int slot) {

        CompletableFuture<StatefulRedisConnection<K, V>> writer;// avoid races when reconfiguring partitions.
        synchronized (stateLock) {
            writer = writers[slot];
        }

        if (writer == null) {
            RedisClusterNode partition = partitions.getPartitionBySlot(slot);
            if (partition == null) {
                clusterEventListener.onUncoveredSlot(slot);
                return Futures.failed(new PartitionSelectorException("Cannot determine a partition for slot " + slot + ".",
                        partitions.clone()));
            }

            // Use always host and port for slot-oriented operations. We don't want to get reconnected on a different
            // host because the nodeId can be handled by a different host.
            RedisURI uri = partition.getUri();
            ConnectionKey key = new ConnectionKey(Intent.WRITE, uri.getHost(), uri.getPort());

            ConnectionFuture<StatefulRedisConnection<K, V>> future = getConnectionAsync(key);

            return future.thenApply(connection -> {

                synchronized (stateLock) {
                    if (writers[slot] == null) {
                        writers[slot] = CompletableFuture.completedFuture(connection);
                    }
                }

                return connection;
            }).toCompletableFuture();
        }

        return writer;
    }

    private CompletableFuture<StatefulRedisConnection<K, V>> getReadConnection(int slot) {

        CompletableFuture<StatefulRedisConnection<K, V>> readerCandidates[];// avoid races when reconfiguring partitions.

        boolean cached = true;

        synchronized (stateLock) {
            readerCandidates = readers[slot];
        }

        if (readerCandidates == null) {

            RedisClusterNode master = partitions.getPartitionBySlot(slot);
            if (master == null) {
                clusterEventListener.onUncoveredSlot(slot);
                return Futures.failed(new PartitionSelectorException(
                        String.format("Cannot determine a partition to read for slot %d.", slot), partitions.clone()));
            }

            List<RedisNodeDescription> candidates = getReadCandidates(master);
            List<RedisNodeDescription> selection = readFrom.select(new ReadFrom.Nodes() {

                @Override
                public List<RedisNodeDescription> getNodes() {
                    return candidates;
                }

                @Override
                public Iterator<RedisNodeDescription> iterator() {
                    return candidates.iterator();
                }

            });

            if (selection.isEmpty()) {
                clusterEventListener.onUncoveredSlot(slot);
                return Futures.failed(new PartitionSelectorException(
                        String.format("Cannot determine a partition to read for slot %d with setting %s.", slot, readFrom),
                        partitions.clone()));
            }

            readerCandidates = getReadFromConnections(selection);
            cached = false;
        }

        CompletableFuture<StatefulRedisConnection<K, V>> selectedReaderCandidates[] = readerCandidates;

        if (cached) {

            return CompletableFuture.allOf(readerCandidates).thenCompose(v -> {

                boolean orderSensitive = isOrderSensitive(selectedReaderCandidates);

                if (!orderSensitive) {

                    CompletableFuture<StatefulRedisConnection<K, V>> candidate = findRandomActiveConnection(
                            selectedReaderCandidates, Function.identity());

                    if (candidate != null) {
                        return candidate;
                    }
                }

                for (CompletableFuture<StatefulRedisConnection<K, V>> candidate : selectedReaderCandidates) {

                    if (candidate.join().isOpen()) {
                        return candidate;
                    }
                }

                return selectedReaderCandidates[0];
            });
        }

        CompletableFuture<StatefulRedisConnection<K, V>[]> filteredReaderCandidates = new CompletableFuture<>();

        CompletableFuture.allOf(readerCandidates).thenApply(v -> selectedReaderCandidates)
                .whenComplete((candidates, throwable) -> {

                    if (throwable == null) {
                        filteredReaderCandidates.complete(getConnections(candidates));
                        return;
                    }

                    StatefulRedisConnection<K, V>[] connections = getConnections(selectedReaderCandidates);

                    if (connections.length == 0) {
                        filteredReaderCandidates.completeExceptionally(throwable);
                        return;
                    }

                    filteredReaderCandidates.complete(connections);
                });

        return filteredReaderCandidates.thenApply(statefulRedisConnections -> {

            boolean orderSensitive = isOrderSensitive(statefulRedisConnections);

            CompletableFuture<StatefulRedisConnection<K, V>> toCache[] = new CompletableFuture[statefulRedisConnections.length];

            for (int i = 0; i < toCache.length; i++) {
                toCache[i] = CompletableFuture.completedFuture(statefulRedisConnections[i]);
            }
            synchronized (stateLock) {
                readers[slot] = toCache;
            }

            if (!orderSensitive) {

                StatefulRedisConnection<K, V> candidate = findRandomActiveConnection(selectedReaderCandidates,
                        CompletableFuture::join);

                if (candidate != null) {
                    return candidate;
                }
            }

            for (StatefulRedisConnection<K, V> candidate : statefulRedisConnections) {
                if (candidate.isOpen()) {
                    return candidate;
                }
            }

            return statefulRedisConnections[0];
        });
    }

    private boolean isOrderSensitive(Object[] connections) {
        return OrderingReadFromAccessor.isOrderSensitive(readFrom) || connections.length == 1;
    }

    private static <T, E extends StatefulConnection<?, ?>> T findRandomActiveConnection(
            CompletableFuture<E>[] selectedReaderCandidates, Function<CompletableFuture<E>, T> mappingFunction) {

        // Perform up to two attempts for random nodes.
        for (int i = 0; i < Math.min(2, selectedReaderCandidates.length); i++) {

            int index = ThreadLocalRandom.current().nextInt(selectedReaderCandidates.length);
            CompletableFuture<E> candidateFuture = selectedReaderCandidates[index];

            if (candidateFuture.isDone() && !candidateFuture.isCompletedExceptionally()) {

                E candidate = candidateFuture.join();

                if (candidate.isOpen()) {
                    return mappingFunction.apply(candidateFuture);
                }
            }
        }
        return null;
    }

    private StatefulRedisConnection<K, V>[] getConnections(
            CompletableFuture<StatefulRedisConnection<K, V>>[] selectedReaderCandidates) {

        List<StatefulRedisConnection<K, V>> connections = new ArrayList<>(selectedReaderCandidates.length);

        for (CompletableFuture<StatefulRedisConnection<K, V>> candidate : selectedReaderCandidates) {

            try {
                connections.add(candidate.join());
            } catch (Exception o_O) {
            }
        }

        StatefulRedisConnection<K, V>[] result = new StatefulRedisConnection[connections.size()];
        connections.toArray(result);
        return result;
    }

    private CompletableFuture<StatefulRedisConnection<K, V>>[] getReadFromConnections(List<RedisNodeDescription> selection) {

        // Use always host and port for slot-oriented operations. We don't want to get reconnected on a different
        // host because the nodeId can be handled by a different host.

        CompletableFuture<StatefulRedisConnection<K, V>>[] readerCandidates = new CompletableFuture[selection.size()];

        for (int i = 0; i < selection.size(); i++) {

            RedisNodeDescription redisClusterNode = selection.get(i);

            RedisURI uri = redisClusterNode.getUri();
            ConnectionKey key = new ConnectionKey(
                    redisClusterNode.getRole() == RedisInstance.Role.MASTER ? Intent.WRITE : Intent.READ, uri.getHost(),
                    uri.getPort());

            readerCandidates[i] = getConnectionAsync(key).toCompletableFuture();
        }

        return readerCandidates;
    }

    private List<RedisNodeDescription> getReadCandidates(RedisClusterNode master) {

        return partitions.stream() //
                .filter(partition -> isReadCandidate(master, partition)) //
                .collect(Collectors.toList());
    }

    private boolean isReadCandidate(RedisClusterNode master, RedisClusterNode partition) {
        return master.getNodeId().equals(partition.getNodeId()) || master.getNodeId().equals(partition.getSlaveOf());
    }

    @Override
    public StatefulRedisConnection<K, V> getConnection(Intent intent, String nodeId) {

        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ", " + nodeId + ")");
        }

        return getConnection(new ConnectionKey(intent, nodeId));
    }

    @Override
    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(Intent intent, String nodeId) {

        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ", " + nodeId + ")");
        }

        return getConnectionAsync(new ConnectionKey(intent, nodeId)).toCompletableFuture();
    }

    protected ConnectionFuture<StatefulRedisConnection<K, V>> getConnectionAsync(ConnectionKey key) {

        ConnectionFuture<StatefulRedisConnection<K, V>> connectionFuture = connectionProvider.getConnection(key);
        CompletableFuture<StatefulRedisConnection<K, V>> result = new CompletableFuture<>();

        connectionFuture.handle((connection, throwable) -> {

            if (throwable != null) {

                result.completeExceptionally(RedisConnectionException.create(connectionFuture.getRemoteAddress(), throwable));
            } else {
                result.complete(connection);
            }

            return null;
        });

        return ConnectionFuture.from(connectionFuture.getRemoteAddress(), result);
    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding", "rawtypes" })
    public StatefulRedisConnection<K, V> getConnection(Intent intent, String host, int port) {

        try {
            beforeGetConnection(intent, host, port);

            return getConnection(new ConnectionKey(intent, host, port));
        } catch (RedisException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RedisException(e);
        }
    }

    private StatefulRedisConnection<K, V> getConnection(ConnectionKey key) {

        ConnectionFuture<StatefulRedisConnection<K, V>> future = getConnectionAsync(key);

        try {
            return future.join();
        } catch (CompletionException e) {
            throw RedisConnectionException.create(future.getRemoteAddress(), e.getCause());
        }
    }

    @Override
    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(Intent intent, String host, int port) {

        try {
            beforeGetConnection(intent, host, port);

            return connectionProvider.getConnection(new ConnectionKey(intent, host, port)).toCompletableFuture();
        } catch (RedisException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RedisException(e);
        }
    }

    private void beforeGetConnection(Intent intent, String host, int port) {

        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ", " + host + ", " + port + ")");
        }

        RedisClusterNode redisClusterNode = partitions.getPartition(host, port);

        if (redisClusterNode == null) {
            clusterEventListener.onUnknownNode();

            if (validateClusterNodeMembership()) {
                HostAndPort hostAndPort = HostAndPort.of(host, port);
                throw connectionAttemptRejected(hostAndPort.toString());
            }
        }
    }

    @Override
    public void close() {
        closeAsync().join();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {

        resetFastConnectionCache();

        return connectionProvider.close();
    }

    @Override
    public void reset() {
        connectionProvider.forEach(StatefulRedisConnection::reset);
    }

    /**
     * Synchronize on {@code stateLock} to initiate a happens-before relation and clear the thread caches of other threads.
     *
     * @param partitions the new partitions.
     */
    @Override
    public void setPartitions(Partitions partitions) {

        boolean reconfigurePartitions = false;

        synchronized (stateLock) {
            if (this.partitions != null) {
                reconfigurePartitions = true;
            }
            this.partitions = partitions;
            this.connectionFactory.setPartitions(partitions);
        }

        if (reconfigurePartitions) {
            reconfigurePartitions();
        }
    }

    protected Partitions getPartitions() {
        return partitions;
    }

    private void reconfigurePartitions() {

        resetFastConnectionCache();

        if (redisClusterClient.expireStaleConnections()) {
            closeStaleConnections();
        }
    }

    /**
     * Close stale connections.
     */
    @Override
    public void closeStaleConnections() {

        logger.debug("closeStaleConnections() count before expiring: {}", getConnectionCount());

        connectionProvider.forEach((key, connection) -> {
            if (isStale(key)) {
                connectionProvider.close(key);
            }
        });

        logger.debug("closeStaleConnections() count after expiring: {}", getConnectionCount());
    }

    private boolean isStale(ConnectionKey connectionKey) {

        if (connectionKey.nodeId != null && partitions.getPartitionByNodeId(connectionKey.nodeId) != null) {
            return false;
        }

        if (connectionKey.host != null && partitions.getPartition(connectionKey.host, connectionKey.port) != null) {
            return false;
        }

        return true;
    }

    /**
     * Set auto-flush on all commands. Synchronize on {@code stateLock} to initiate a happens-before relation and clear the
     * thread caches of other threads.
     *
     * @param autoFlush state of autoFlush.
     */
    @Override
    public void setAutoFlushCommands(boolean autoFlush) {

        synchronized (stateLock) {
            this.autoFlushCommands = autoFlush;
        }

        connectionProvider.forEach(connection -> connection.setAutoFlushCommands(autoFlush));
    }

    @Override
    public void flushCommands() {
        connectionProvider.forEach(StatefulConnection::flushCommands);
    }

    @Override
    public void setReadFrom(ReadFrom readFrom) {

        synchronized (stateLock) {
            this.readFrom = readFrom;
            Arrays.fill(readers, null);
        }
    }

    @Override
    public ReadFrom getReadFrom() {
        return this.readFrom;
    }

    /**
     *
     * @return number of connections.
     */
    long getConnectionCount() {
        return connectionProvider.getConnectionCount();
    }

    /**
     * Reset the internal connection cache. This is necessary because the {@link Partitions} have no reference to the connection
     * cache.
     *
     * Synchronize on {@code stateLock} to initiate a happens-before relation and clear the thread caches of other threads.
     */
    private void resetFastConnectionCache() {

        synchronized (stateLock) {
            Arrays.fill(writers, null);
            Arrays.fill(readers, null);
        }
    }

    private static RuntimeException connectionAttemptRejected(String message) {

        return new UnknownPartitionException(
                "Connection to " + message + " not allowed. This partition is not known in the cluster view.");
    }

    private boolean validateClusterNodeMembership() {
        return redisClusterClient.getClusterClientOptions() == null
                || redisClusterClient.getClusterClientOptions().isValidateClusterNodeMembership();
    }

    /**
     * @return a factory {@link Function}.
     */
    protected ClusterNodeConnectionFactory<K, V> getConnectionFactory(RedisClusterClient redisClusterClient) {
        return new DefaultClusterNodeConnectionFactory<>(redisClusterClient, redisCodec, clusterWriter);
    }

    class NodeConnectionPostProcessor implements ClusterNodeConnectionFactory<K, V> {

        private final ClusterNodeConnectionFactory<K, V> delegate;

        NodeConnectionPostProcessor(ClusterNodeConnectionFactory<K, V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void setPartitions(Partitions partitions) {
            this.delegate.setPartitions(partitions);
        }

        @Override
        public ConnectionFuture<StatefulRedisConnection<K, V>> apply(ConnectionKey key) {

            if (key.nodeId != null && getPartitions().getPartitionByNodeId(key.nodeId) == null) {
                clusterEventListener.onUnknownNode();
                throw connectionAttemptRejected("node id " + key.nodeId);
            }

            if (key.host != null && partitions.getPartition(key.host, key.port) == null) {
                clusterEventListener.onUnknownNode();
                if (validateClusterNodeMembership()) {
                    throw connectionAttemptRejected(key.host + ":" + key.port);
                }
            }

            ConnectionFuture<StatefulRedisConnection<K, V>> connection = delegate.apply(key);

            LettuceAssert.notNull(connection, "Connection is null. Check ConnectionKey because host and nodeId are null.");

            if (key.intent == Intent.READ) {

                connection = connection.thenCompose(c -> {

                    RedisFuture<String> stringRedisFuture = c.async().readOnly();
                    return stringRedisFuture.thenApply(s -> c).whenCompleteAsync((s, throwable) -> {
                        if (throwable != null) {
                            c.close();
                        }
                    });
                });
            }

            connection = connection.thenApply(c -> {
                synchronized (stateLock) {
                    c.setAutoFlushCommands(autoFlushCommands);
                }
                return c;
            });

            return connection;
        }

    }

    static class DefaultClusterNodeConnectionFactory<K, V> extends AbstractClusterNodeConnectionFactory<K, V> {

        private final RedisClusterClient redisClusterClient;

        private final RedisCodec<K, V> redisCodec;

        private final RedisChannelWriter clusterWriter;

        DefaultClusterNodeConnectionFactory(RedisClusterClient redisClusterClient, RedisCodec<K, V> redisCodec,
                RedisChannelWriter clusterWriter) {

            super(redisClusterClient.getResources());
            this.redisClusterClient = redisClusterClient;
            this.redisCodec = redisCodec;
            this.clusterWriter = clusterWriter;
        }

        @Override
        public ConnectionFuture<StatefulRedisConnection<K, V>> apply(ConnectionKey key) {

            if (key.nodeId != null) {
                // NodeId connections do not provide command recovery due to cluster reconfiguration
                return redisClusterClient.connectToNodeAsync(redisCodec, key.nodeId, null, getSocketAddressSupplier(key));
            }

            // Host and port connections do provide command recovery due to cluster reconfiguration
            return redisClusterClient.connectToNodeAsync(redisCodec, key.host + ":" + key.port, clusterWriter,
                    getSocketAddressSupplier(key));
        }

    }

}
