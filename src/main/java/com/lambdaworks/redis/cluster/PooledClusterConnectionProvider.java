package com.lambdaworks.redis.cluster;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
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

    private final LoadingCache<PoolKey, RedisAsyncConnectionImpl<K, V>> connections;
    private final boolean debugEnabled;
    private final RedisAsyncConnectionImpl<K, V> writers[] = new RedisAsyncConnectionImpl[SlotHash.SLOT_COUNT];
    private Partitions partitions;

    private boolean autoFlushCommands = true;
    private Object stateLock = new Object();

    public PooledClusterConnectionProvider(final RedisClusterClient redisClusterClient, final RedisCodec<K, V> redisCodec) {
        this.debugEnabled = logger.isDebugEnabled();
        this.connections = CacheBuilder.newBuilder().build(new CacheLoader<PoolKey, RedisAsyncConnectionImpl<K, V>>() {
            @Override
            public RedisAsyncConnectionImpl<K, V> load(PoolKey key) throws Exception {
                RedisAsyncConnectionImpl<K, V> connection = redisClusterClient.connectAsyncImpl(redisCodec,
                        key.getSocketAddress());
                synchronized (stateLock) {
                    connection.getChannelWriter().setAutoFlushCommands(autoFlushCommands);
                }
                return connection;
            }
        });
    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding", "rawtypes" })
    public RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, int slot) {
        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ", " + slot + ")");
        }

        RedisAsyncConnectionImpl<K, V> writer = writers[slot];
        if (writer == null) {
            RedisClusterNode partition = partitions.getPartitionBySlot(slot);
            if (partition == null) {
                throw new RedisException("Cannot determine a partition for slot " + slot + " (Partitions: " + partitions + ")");
            }

            try {
                PoolKey key = new PoolKey(intent, partition.getUri());
                return writers[slot] = getConnection(key);
            } catch (Exception e) {
                throw new RedisException(e);
            }
        }
        return writer;
    }

    private RedisAsyncConnectionImpl<K, V> getConnection(PoolKey key) throws java.util.concurrent.ExecutionException {
        return connections.get(key);
    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding", "rawtypes" })
    public RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, String host, int port) {
        try {
            if (debugEnabled) {
                logger.debug("getConnection(" + intent + ", " + host + ", " + port + ")");
            }
            PoolKey key = new PoolKey(intent, host, port);
            return getConnection(key);
        } catch (Exception e) {
            throw new RedisException(e);
        }
    }

    @Override
    public void close() {
        ImmutableMap<PoolKey, RedisAsyncConnectionImpl<K, V>> copy = ImmutableMap.copyOf(this.connections.asMap());
        this.connections.invalidateAll();
        resetPartitions();
        for (RedisAsyncConnection<K, V> kvRedisAsyncConnection : copy.values()) {
            if (kvRedisAsyncConnection.isOpen()) {
                kvRedisAsyncConnection.close();
            }
        }
    }

    @Override
    public void reset() {
        ImmutableMap<PoolKey, RedisAsyncConnectionImpl<K, V>> copy = ImmutableMap.copyOf(this.connections.asMap());
        for (RedisAsyncConnectionImpl<K, V> kvRedisAsyncConnection : copy.values()) {
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
    public void setAutoFlushCommands(boolean autoFlush) {
        synchronized (stateLock) {
            this.autoFlushCommands = autoFlush;
        }
        for (RedisAsyncConnectionImpl<K, V> connection : connections.asMap().values()) {
            connection.getChannelWriter().setAutoFlushCommands(autoFlush);
        }
    }

    @Override
    public void flushCommands() {

        for (RedisAsyncConnectionImpl<K, V> connection : connections.asMap().values()) {
            connection.getChannelWriter().flushCommands();
        }

    }

    protected void resetPartitions() {

        synchronized (stateLock) {
            Arrays.fill(writers, null);
        }
    }
}
