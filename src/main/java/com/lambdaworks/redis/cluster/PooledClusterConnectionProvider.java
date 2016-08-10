package com.lambdaworks.redis.cluster;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.HostAndPort;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceLists;
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RedisNodeDescription;
import com.lambdaworks.redis.resource.SocketAddressResolver;

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
    private final Map<ConnectionKey, StatefulRedisConnection<K, V>> connections = new ConcurrentHashMap<>();
    private final Object stateLock = new Object();
    private final boolean debugEnabled;
    private final StatefulRedisConnection<K, V> writers[] = new StatefulRedisConnection[SlotHash.SLOT_COUNT];
    private final StatefulRedisConnection<K, V> readers[][] = new StatefulRedisConnection[SlotHash.SLOT_COUNT][];
    private final RedisClusterClient redisClusterClient;
    private final ConnectionFactory connectionFactory;

    private Partitions partitions;
    private boolean autoFlushCommands = true;
    private ReadFrom readFrom;

    public PooledClusterConnectionProvider(RedisClusterClient redisClusterClient, RedisChannelWriter clusterWriter,
            RedisCodec<K, V> redisCodec) {
        this.redisClusterClient = redisClusterClient;
        this.debugEnabled = logger.isDebugEnabled();
        this.connectionFactory = new ConnectionFactory(redisClusterClient, redisCodec, clusterWriter);
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
            return writers[slot] = getOrCreateConnection(key);

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
                throw new RedisException(
                        "Cannot determine a partition to read for slot " + slot + " (Partitions: " + partitions + ")");
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
                throw new RedisException("Cannot determine a partition to read for slot " + slot + " (Partitions: " + partitions
                        + ") with setting " + readFrom);
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
            ConnectionKey key = new ConnectionKey(
                    redisClusterNode.getRole() == RedisInstance.Role.MASTER ? Intent.WRITE : Intent.READ, uri.getHost(),
                    uri.getPort());

            readerCandidates[i] = getOrCreateConnection(key);
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
        return getOrCreateConnection(key);
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
            return getOrCreateConnection(key);
        } catch (RedisException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new RedisException(e);
        }
    }

    private RedisClusterNode getPartition(String host, int port) {
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

        this.connections.clear();
        resetFastConnectionCache();

        new HashMap<>(this.connections) //
                .values() //
                .stream() //
                .filter(StatefulConnection::isOpen).forEach(StatefulConnection::close);
    }

    @Override
    public void reset() {
        allConnections().forEach(StatefulRedisConnection::reset);
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
        }

        if (reconfigurePartitions) {
            reconfigurePartitions();
        }
    }

    private void reconfigurePartitions() {

        if (!redisClusterClient.expireStaleConnections()) {
            return;
        }

        Set<ConnectionKey> staleConnections = getStaleConnectionKeys();

        for (ConnectionKey key : staleConnections) {
            StatefulRedisConnection<K, V> connection = connections.get(key);

            RedisChannelHandler<K, V> endpoint = (RedisChannelHandler<K, V>) connection;

            if (endpoint.getChannelWriter() instanceof ClusterNodeEndpoint) {
                ClusterNodeEndpoint clusterNodeEndpoint = (ClusterNodeEndpoint) endpoint
                        .getChannelWriter();
                clusterNodeEndpoint.prepareClose();
            }
        }

        resetFastConnectionCache();
        closeStaleConnections();
    }

    /**
     * Close stale connections.
     */
    @Override
    public void closeStaleConnections() {
        logger.debug("closeStaleConnections() count before expiring: {}", getConnectionCount());

        Set<ConnectionKey> stale = getStaleConnectionKeys();

        for (ConnectionKey connectionKey : stale) {
            StatefulRedisConnection<K, V> connection = connections.get(connectionKey);
            if (connection != null) {
                connections.remove(connectionKey);
                connection.close();
            }
        }

        logger.debug("closeStaleConnections() count after expiring: {}", getConnectionCount());
    }

    /**
     * Retrieve a set of PoolKey's for all pooled connections that are within the pool but not within the {@link Partitions}.
     * 
     * @return Set of {@link ConnectionKey}s
     */
    private Set<ConnectionKey> getStaleConnectionKeys() {
        Map<ConnectionKey, StatefulRedisConnection<K, V>> map = new HashMap<>(connections);
        Set<ConnectionKey> stale = new HashSet<>();

        for (ConnectionKey connectionKey : map.keySet()) {

            if (connectionKey.nodeId != null && partitions.getPartitionByNodeId(connectionKey.nodeId) != null) {
                continue;
            }

            if (connectionKey.host != null && getPartition(connectionKey.host, connectionKey.port) != null) {
                continue;
            }
            stale.add(connectionKey);
        }
        return stale;
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

        allConnections().forEach(connection -> connection.setAutoFlushCommands(autoFlush));
    }

    private Collection<StatefulRedisConnection<K, V>> allConnections() {
        return LettuceLists.unmodifiableList(connections.values());
    }

    @Override
    public void flushCommands() {
        allConnections().forEach(StatefulConnection::flushCommands);
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
        return connections.size();
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

    private RuntimeException invalidConnectionPoint(String message) {
        return new IllegalArgumentException(
                "Connection to " + message + " not allowed. This connection point is not known in the cluster view");
    }

    private Supplier<SocketAddress> getSocketAddressSupplier(final ConnectionKey connectionKey) {
        return () -> {
            if (connectionKey.nodeId != null) {
                SocketAddress socketAddress = getSocketAddress(connectionKey.nodeId);
                logger.debug("Resolved SocketAddress {} using for Cluster node {}", socketAddress, connectionKey.nodeId);
                return socketAddress;
            }
            SocketAddress socketAddress = new InetSocketAddress(connectionKey.host, connectionKey.port);
            logger.debug("Resolved SocketAddress {} using for Cluster node at {}:{}", socketAddress, connectionKey.host,
                    connectionKey.port);
            return socketAddress;
        };
    }

    private SocketAddress getSocketAddress(String nodeId) {
        for (RedisClusterNode partition : partitions) {
            if (partition.getNodeId().equals(nodeId)) {
                return SocketAddressResolver.resolve(partition.getUri(), redisClusterClient.getResources().dnsResolver());
            }
        }
        return null;
    }

    /**
     * Connection to identify a connection either by nodeId or host/port.
     */
    private static class ConnectionKey {
        private final ClusterConnectionProvider.Intent intent;
        private final String nodeId;
        private final String host;
        private final int port;

        public ConnectionKey(Intent intent, String nodeId) {
            this.intent = intent;
            this.nodeId = nodeId;
            this.host = null;
            this.port = 0;
        }

        public ConnectionKey(Intent intent, String host, int port) {
            this.intent = intent;
            this.host = host;
            this.port = port;
            this.nodeId = null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ConnectionKey))
                return false;

            ConnectionKey key = (ConnectionKey) o;

            if (port != key.port)
                return false;
            if (intent != key.intent)
                return false;
            if (nodeId != null ? !nodeId.equals(key.nodeId) : key.nodeId != null)
                return false;
            return !(host != null ? !host.equals(key.host) : key.host != null);
        }

        @Override
        public int hashCode() {
            int result = intent != null ? intent.name().hashCode() : 0;
            result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
            result = 31 * result + (host != null ? host.hashCode() : 0);
            result = 31 * result + port;
            return result;
        }
    }

    private boolean validateClusterNodeMembership() {
        return redisClusterClient.getClusterClientOptions() == null
                || redisClusterClient.getClusterClientOptions().isValidateClusterNodeMembership();
    }

    private StatefulRedisConnection<K, V> getOrCreateConnection(ConnectionKey key) {
        return connections.computeIfAbsent(key, connectionFactory);
    }

    private class ConnectionFactory implements Function<ConnectionKey, StatefulRedisConnection<K, V>> {

        private final RedisClusterClient redisClusterClient;
        private final RedisCodec<K, V> redisCodec;
        private final RedisChannelWriter clusterWriter;

        public ConnectionFactory(RedisClusterClient redisClusterClient, RedisCodec<K, V> redisCodec,
                RedisChannelWriter clusterWriter) {
            this.redisClusterClient = redisClusterClient;
            this.redisCodec = redisCodec;
            this.clusterWriter = clusterWriter;
        }

        @Override
        public StatefulRedisConnection<K, V> apply(ConnectionKey key) {

            StatefulRedisConnection<K, V> connection = null;

            if (key.nodeId != null) {
                if (partitions.getPartitionByNodeId(key.nodeId) == null) {
                    throw invalidConnectionPoint("node id " + key.nodeId);
                }

                // NodeId connections do not provide command recovery due to cluster reconfiguration
                connection = redisClusterClient.connectToNode(redisCodec, key.nodeId, null, getSocketAddressSupplier(key));
            }

            if (key.host != null) {

                if (validateClusterNodeMembership()) {
                    if (getPartition(key.host, key.port) == null) {
                        throw invalidConnectionPoint(key.host + ":" + key.port);
                    }
                }

                // Host and port connections do provide command recovery due to cluster reconfiguration
                connection = redisClusterClient.connectToNode(redisCodec, key.host + ":" + key.port, clusterWriter,
                        getSocketAddressSupplier(key));
            }

            LettuceAssert.notNull(connection, "Connection is null. Check ConnectionKey because host and nodeId are null");

            try {
                if (key.intent == Intent.READ) {
                    connection.sync().readOnly();
                }

                synchronized (stateLock) {
                    connection.setAutoFlushCommands(autoFlushCommands);
                }
            } catch (RuntimeException e) {
                connection.close();
                throw e;
            }

            return connection;
        }
    }
}
