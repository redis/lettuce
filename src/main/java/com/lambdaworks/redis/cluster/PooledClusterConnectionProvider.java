package com.lambdaworks.redis.cluster;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Connection provider with built-in connection caching.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class PooledClusterConnectionProvider<K, V> implements ClusterConnectionProvider {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PooledClusterConnectionProvider.class);

    private final LoadingCache<PoolKey, StatefulRedisConnection<K, V>> connections;
    private final boolean debugEnabled;
    private final StatefulRedisConnection<K, V> writers[] = new StatefulRedisConnection[SlotHash.SLOT_COUNT];
    private Partitions partitions;

    private boolean autoFlushCommands = true;
    private Object stateLock = new Object();

    public PooledClusterConnectionProvider(final RedisClusterClient redisClusterClient, final RedisCodec<K, V> redisCodec) {
        this.debugEnabled = logger.isDebugEnabled();
        this.connections = CacheBuilder.newBuilder().build(new CacheLoader<PoolKey, StatefulRedisConnection<K, V>>() {
            @Override
            public StatefulRedisConnection<K, V> load(PoolKey key) throws Exception {

                Set<HostAndPort> redisUris = getConnectionPointsOfAllNodes();
                HostAndPort hostAndPort = HostAndPort.fromParts(key.host, key.port);

                if (!redisUris.contains(hostAndPort)) {
                    throw new IllegalArgumentException("Connection to " + hostAndPort
                            + " not allowed. This connection point is not known in the cluster view");
                }

                StatefulRedisConnection<K, V> connection = redisClusterClient.connectToNode(redisCodec, key.getSocketAddress());
                if (key.getIntent() == Intent.READ) {
                    connection.sync().readOnly();
                }

                synchronized (stateLock) {
                    connection.setAutoFlushCommands(autoFlushCommands);
                }
                return connection;
            }
        });
    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding", "rawtypes" })
    public StatefulRedisConnection<K, V> getConnection(Intent intent, int slot) {
        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ", " + slot + ")");
        }

        StatefulRedisConnection<K, V> writer = writers[slot];
        if (writer == null) {
            RedisClusterNode partition = partitions.getPartitionBySlot(slot);
            if (partition == null) {
                throw new RedisException("Cannot determine a partition for slot " + slot + " (Partitions: " + partitions + ")");
            }

            try {
                PoolKey key = new PoolKey(intent, partition.getUri());
                return writers[slot] = getConnection(key);
            } catch (UncheckedExecutionException e) {
                throw new RedisException(e.getCause());
            } catch (Exception e) {
                throw new RedisException(e);
            }
        }
        return writer;
    }

    private StatefulRedisConnection<K, V> getConnection(PoolKey key) throws java.util.concurrent.ExecutionException {
        return connections.get(key);
    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding", "rawtypes" })
    public StatefulRedisConnection<K, V> getConnection(Intent intent, String host, int port) {
        try {
            if (debugEnabled) {
                logger.debug("getConnection(" + intent + ", " + host + ", " + port + ")");
            }
            PoolKey key = new PoolKey(intent, host, port);
            return getConnection(key);
        } catch (UncheckedExecutionException e) {
            throw new RedisException(e.getCause());
        } catch (Exception e) {
            throw new RedisException(e);
        }
    }

    @Override
    public void close() {
        ImmutableMap<PoolKey, StatefulRedisConnection<K, V>> copy = ImmutableMap.copyOf(this.connections.asMap());
        this.connections.invalidateAll();
        resetPartitions();
        for (StatefulRedisConnection<K, V> kvRedisAsyncConnection : copy.values()) {
            if (kvRedisAsyncConnection.isOpen()) {
                kvRedisAsyncConnection.close();
            }
        }
    }

    @Override
    public void reset() {
        ImmutableMap<PoolKey, StatefulRedisConnection<K, V>> copy = ImmutableMap.copyOf(this.connections.asMap());
        for (StatefulRedisConnection<K, V> kvRedisAsyncConnection : copy.values()) {
            kvRedisAsyncConnection.reset();
        }
    }

    private static class PoolKey {
        private final ClusterConnectionProvider.Intent intent;
        private SocketAddress socketAddress;
        private final String host;
        private final int port;

        private PoolKey(ClusterConnectionProvider.Intent intent, RedisURI uri) {
            this.intent = intent;
            this.host = uri.getHost();
            this.port = uri.getPort();
            this.socketAddress = uri.getResolvedAddress();
        }

        private PoolKey(Intent intent, String host, int port) {
            this.intent = intent;
            this.host = host;
            this.port = port;
        }

        public ClusterConnectionProvider.Intent getIntent() {
            return intent;
        }

        public SocketAddress getSocketAddress() {

            if (socketAddress == null && LettuceStrings.isNotEmpty(host)) {
                socketAddress = new InetSocketAddress(host, port);
            }

            return socketAddress;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PoolKey)) {
                return false;
            }

            PoolKey poolKey = (PoolKey) o;

            if (port != poolKey.port) {
                return false;
            }
            if (host != null ? !host.equals(poolKey.host) : poolKey.host != null) {
                return false;
            }
            if (intent != poolKey.intent) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = intent != null ? intent.hashCode() : 0;
            result = 31 * result + (host != null ? host.hashCode() : 0);
            result = 31 * result + port;
            return result;
        }
    }

    @Override
    public void setPartitions(Partitions partitions) {
        this.partitions = partitions;
        resetPartitions();
    }

    @Override
    public void closeStaleConnections() {
        logger.debug("closeStaleConnections() count before expiring: {}", getConnectionCount());
        Map<PoolKey, StatefulRedisConnection<K, V>> map = Maps.newHashMap(connections.asMap());
        Set<HostAndPort> redisUris = getConnectionPointsOfAllNodes();

        for (PoolKey poolKey : map.keySet()) {
            if (redisUris.contains(HostAndPort.fromParts(poolKey.host, poolKey.port))) {
                continue;
            }

            connections.invalidate(poolKey);
            map.get(poolKey).close();
        }

        logger.debug("closeStaleConnections() count after expiring: {}", getConnectionCount());
    }

    protected Set<HostAndPort> getConnectionPointsOfAllNodes() {
        Set<HostAndPort> redisUris = Sets.newHashSet();

        for (RedisClusterNode partition : partitions) {
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MASTER)) {
                redisUris.add(HostAndPort.fromParts(partition.getUri().getHost(), partition.getUri().getPort()));
            }

            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.SLAVE)) {
                redisUris.add(HostAndPort.fromParts(partition.getUri().getHost(), partition.getUri().getPort()));
            }
        }
        return redisUris;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        synchronized (stateLock) {
            this.autoFlushCommands = autoFlush;
        }

        for (StatefulRedisConnection<K, V> connection : connections.asMap().values()) {
            connection.setAutoFlushCommands(autoFlush);
        }
    }

    @Override
    public void flushCommands() {

        for (StatefulRedisConnection<K, V> connection : connections.asMap().values()) {
            connection.flushCommands();
        }
    }

    protected long getConnectionCount() {
        return connections.size();
    }

    protected void resetPartitions() {

        synchronized (stateLock) {
            Arrays.fill(writers, null);
        }
    }
}
