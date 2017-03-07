/*
 * Copyright 2011-2017 the original author or authors.
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
package io.lettuce.core.cluster;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.ClusterNodeConnectionFactory.ConnectionKey;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
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
class PooledClusterConnectionProvider<K, V> implements ClusterConnectionProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PooledClusterConnectionProvider.class);

    // Contains NodeId-identified and HostAndPort-identified connections.
    private final Object stateLock = new Object();
    private final boolean debugEnabled = logger.isDebugEnabled();
    private final StatefulRedisConnection<K, V> writers[] = new StatefulRedisConnection[SlotHash.SLOT_COUNT];
    private final StatefulRedisConnection<K, V> readers[][] = new StatefulRedisConnection[SlotHash.SLOT_COUNT][];
    private final RedisClusterClient redisClusterClient;
    private final ClusterNodeConnectionFactory<K, V> connectionFactory;
    private final RedisChannelWriter clusterWriter;
    private final RedisCodec<K, V> redisCodec;
    private final SynchronizingClusterConnectionProvider<K, V> connectionProvider;

    private Partitions partitions;
    private boolean autoFlushCommands = true;
    private ReadFrom readFrom;

    public PooledClusterConnectionProvider(RedisClusterClient redisClusterClient, RedisChannelWriter clusterWriter,
            RedisCodec<K, V> redisCodec) {

        this.redisCodec = redisCodec;
        this.redisClusterClient = redisClusterClient;
        this.clusterWriter = clusterWriter;
        this.connectionFactory = new NodeConnectionPostProcessor(getConnectionFactory(redisClusterClient));
        this.connectionProvider = new SynchronizingClusterConnectionProvider<>(this.connectionFactory);
    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding", "rawtypes" })
    public StatefulRedisConnection<K, V> getConnection(Intent intent, int slot) {
        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ", " + slot + ")");
        }

        try {
            if (intent == Intent.READ && readFrom != null) {
                return getReadConnection(slot);
            }
            return getWriteConnection(slot);
        } catch (RedisException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RedisException(e);
        }
    }

    private StatefulRedisConnection<K, V> getWriteConnection(int slot) {

        StatefulRedisConnection<K, V> writer;// avoid races when reconfiguring partitions.
        synchronized (stateLock) {
            writer = writers[slot];
        }

        if (writer == null) {
            RedisClusterNode partition = partitions.getPartitionBySlot(slot);
            if (partition == null) {
                throw new RedisException("Cannot determine a partition for slot " + slot + " (Partitions: " + partitions + ")");
            }

            // Use always host and port for slot-oriented operations. We don't want to get reconnected on a different
            // host because the nodeId can be handled by a different host.
            RedisURI uri = partition.getUri();
            ConnectionKey key = new ConnectionKey(Intent.WRITE, uri.getHost(), uri.getPort());
            return writers[slot] = connectionProvider.getConnection(key);

        }
        return writer;
    }

    protected StatefulRedisConnection<K, V> getReadConnection(int slot) {

        StatefulRedisConnection<K, V> readerCandidates[];// avoid races when reconfiguring partitions.
        synchronized (stateLock) {
            readerCandidates = readers[slot];
        }

        if (readerCandidates == null) {
            RedisClusterNode master = partitions.getPartitionBySlot(slot);
            if (master == null) {
                throw new RedisException("Cannot determine a partition to read for slot " + slot + " (Partitions: "
                        + partitions + ")");
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
                throw new RedisException("Cannot determine a partition to read for slot " + slot + " (Partitions: "
                        + partitions + ") with setting " + readFrom);
            }

            readerCandidates = getReadFromConnections(selection);
            readers[slot] = readerCandidates;
        }

        // try working connections at first
        for (StatefulRedisConnection<K, V> readerCandidate : readerCandidates) {
            if (!readerCandidate.isOpen()) {
                continue;
            }
            return readerCandidate;
        }

        // fall-back to the first connection for same behavior as writing
        return readerCandidates[0];
    }

    private StatefulRedisConnection<K, V>[] getReadFromConnections(List<RedisNodeDescription> selection) {

        StatefulRedisConnection<K, V>[] readerCandidates;
        // Use always host and port for slot-oriented operations. We don't want to get reconnected on a different
        // host because the nodeId can be handled by a different host.

        readerCandidates = new StatefulRedisConnection[selection.size()];

        for (int i = 0; i < selection.size(); i++) {
            RedisNodeDescription redisClusterNode = selection.get(i);

            RedisURI uri = redisClusterNode.getUri();
            ConnectionKey key = new ConnectionKey(redisClusterNode.getRole() == RedisInstance.Role.MASTER ? Intent.WRITE
                    : Intent.READ, uri.getHost(), uri.getPort());

            readerCandidates[i] = connectionProvider.getConnection(key);
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

        ConnectionKey key = new ConnectionKey(intent, nodeId);
        return connectionProvider.getConnection(key);
    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding", "rawtypes" })
    public StatefulRedisConnection<K, V> getConnection(Intent intent, String host, int port) {

        try {
            if (debugEnabled) {
                logger.debug("getConnection(" + intent + ", " + host + ", " + port + ")");
            }

            if (validateClusterNodeMembership()) {
                RedisClusterNode redisClusterNode = getPartition(host, port);

                if (redisClusterNode == null) {
                    HostAndPort hostAndPort = HostAndPort.of(host, port);
                    throw invalidConnectionPoint(hostAndPort.toString());
                }
            }

            ConnectionKey key = new ConnectionKey(intent, host, port);
            return connectionProvider.getConnection(key);
        } catch (RedisException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RedisException(e);
        }
    }

    protected RedisClusterNode getPartition(String host, int port) {

        for (RedisClusterNode partition : partitions) {
            RedisURI uri = partition.getUri();
            if (port == uri.getPort() && host.equals(uri.getHost())) {
                return partition;
            }
        }
        return null;
    }

    @Override
    public void close() {

        resetFastConnectionCache();

        connectionProvider.close();
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

        if (connectionKey.host != null && getPartition(connectionKey.host, connectionKey.port) != null) {
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

    private static RuntimeException invalidConnectionPoint(String message) {
        return new IllegalArgumentException("Connection to " + message
                + " not allowed. This connection point is not known in the cluster view");
    }

    boolean validateClusterNodeMembership() {
        return redisClusterClient.getClusterClientOptions() == null
                || redisClusterClient.getClusterClientOptions().isValidateClusterNodeMembership();
    }

    /**
     * @return a factory {@link Function}
     */
    protected ClusterNodeConnectionFactory<K, V> getConnectionFactory(RedisClusterClient redisClusterClient) {
        return new DefaultClusterNodeConnectionFactory<>(redisClusterClient, redisCodec, clusterWriter);
    }

    private class NodeConnectionPostProcessor implements ClusterNodeConnectionFactory<K, V> {

        private final ClusterNodeConnectionFactory<K, V> delegate;

        public NodeConnectionPostProcessor(ClusterNodeConnectionFactory<K, V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void setPartitions(Partitions partitions) {
            this.delegate.setPartitions(partitions);
        }

        @Override
        public ConnectionFuture<StatefulRedisConnection<K, V>> apply(ConnectionKey key) {

            if (key.nodeId != null) {
                if (getPartitions().getPartitionByNodeId(key.nodeId) == null) {
                    throw invalidConnectionPoint("node id " + key.nodeId);
                }
            }

            if (key.host != null) {

                if (validateClusterNodeMembership()) {
                    if (getPartition(key.host, key.port) == null) {
                        throw invalidConnectionPoint(key.host + ":" + key.port);
                    }
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

        public DefaultClusterNodeConnectionFactory(RedisClusterClient redisClusterClient, RedisCodec<K, V> redisCodec,
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
