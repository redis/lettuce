package com.lambdaworks.redis.cluster;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Connection provider with built-in pooling
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class PooledClusterConnectionProvider<K, V> implements ClusterConnectionProvider {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PooledClusterConnectionProvider.class);
    private final Partitions partitions;
    private LoadingCache<PoolKey, RedisAsyncConnectionImpl<K, V>> connections;
    private final boolean debugEnabled;

    public PooledClusterConnectionProvider(final RedisClusterClient redisClusterClient, Partitions partitions,
            final RedisCodec<K, V> redisCodec) {
        this.partitions = partitions;
        this.debugEnabled = logger.isDebugEnabled();
        this.connections = CacheBuilder.newBuilder().build(new CacheLoader<PoolKey, RedisAsyncConnectionImpl<K, V>>() {
            @Override
            public RedisAsyncConnectionImpl<K, V> load(PoolKey key) throws Exception {
                return redisClusterClient.connectAsyncImpl(redisCodec, key.getSocketAddress());
            }
        });

    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding", "rawtypes" })
    public RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, int slot) {
        if (debugEnabled) {
            logger.debug("getConnection(" + intent + ", " + slot + ")");
        }
        RedisClusterNode partition = partitions.getPartitionBySlot(slot);
        if (partition == null) {
            throw new RedisException("Cannot determine a partition for slot " + slot + " (Partitions: " + partitions + ")");
        }

        try {
            PoolKey key = new PoolKey(intent, partition.getUri());
            return getConnection(key);
        } catch (Exception e) {
            throw new RedisException(e);
        }
    }

    private RedisAsyncConnectionImpl<K, V> getConnection(PoolKey key) throws java.util.concurrent.ExecutionException {
        RedisAsyncConnectionImpl<K, V> result = connections.get(key);
        if (!result.isOpen()) {
            connections.invalidate(key);
            return connections.get(key);
        }

        return result;
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

        if (connections != null) {
            reset();
        }
        connections = null;

    }

    @Override
    public void reset() {
        for (RedisAsyncConnection<K, V> kvRedisAsyncConnection : connections.asMap().values()) {
            kvRedisAsyncConnection.close();
        }
        connections.invalidateAll();
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

}
