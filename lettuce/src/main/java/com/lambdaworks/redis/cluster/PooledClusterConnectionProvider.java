package com.lambdaworks.redis.cluster;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import com.lambdaworks.redis.LettuceStrings;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.RedisCodec;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Connection provider with built-in pooling
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:54
 */
public class PooledClusterConnectionProvider<K, V> implements ClusterConnectionProvider {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PooledClusterConnectionProvider.class);
    private KeyedObjectPool<PoolKey, RedisAsyncConnection<K, V>> partitionPool;
    private final Partitions partitions;

    public PooledClusterConnectionProvider(RedisClusterClient redisClusterClient, Partitions partitions,
            RedisCodec<K, V> redisCodec) {
        this.partitions = partitions;

        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxIdlePerKey(1);
        config.setMaxTotalPerKey(1);
        config.setTestOnBorrow(true);

        partitionPool = new GenericKeyedObjectPool<PoolKey, RedisAsyncConnection<K, V>>(new KeyedConnectionFactory<K, V>(
                redisClusterClient, redisCodec), config);

    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding", "rawtypes" })
    public <K, V> RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, int slot) {
        logger.debug("getConnection(" + intent + ", " + slot + ")");
        RedisClusterNode partition = partitions.getPartitionBySlot(slot);
        if (partition == null) {
            throw new RedisException("Cannot determine a partition for slot " + slot + " (Partitions: " + partitions + ")");
        }

        try {
            PoolKey key = new PoolKey(intent, partition.getUri());
            RedisAsyncConnection connection = partitionPool.borrowObject(key);
            partitionPool.returnObject(key, connection);
            return (RedisAsyncConnectionImpl<K, V>) connection;
        } catch (Exception e) {
            throw new RedisException(e);
        }
    }

    @Override
    @SuppressWarnings({ "unchecked", "hiding", "rawtypes" })
    public <K, V> RedisAsyncConnectionImpl<K, V> getConnection(Intent intent, String host, int port) {
        try {
            logger.debug("getConnection(" + intent + ", " + host + ", " + port + ")");
            PoolKey key = new PoolKey(intent, host, port);
            RedisAsyncConnection connection = partitionPool.borrowObject(key);
            partitionPool.returnObject(key, connection);
            return (RedisAsyncConnectionImpl<K, V>) connection;
        } catch (Exception e) {
            throw new RedisException(e);
        }
    }

    private static class KeyedConnectionFactory<K, V> extends BaseKeyedPooledObjectFactory<PoolKey, RedisAsyncConnection<K, V>> {
        private final RedisClusterClient redisClusterClient;
        private final RedisCodec<K, V> redisCodec;

        private KeyedConnectionFactory(RedisClusterClient redisClusterClient, RedisCodec<K, V> redisCodec) {
            this.redisClusterClient = redisClusterClient;
            this.redisCodec = redisCodec;
        }

        @Override
        public RedisAsyncConnection<K, V> create(final PoolKey key) throws Exception {

            logger.debug("createConnection(" + key.getIntent() + ", " + key.getSocketAddress() + ")");
            return redisClusterClient.connectAsyncImpl(redisCodec, key.getSocketAddress());
        }

        @Override
        public boolean validateObject(PoolKey key, PooledObject<RedisAsyncConnection<K, V>> p) {
            return p.getObject().isOpen();
        }

        @Override
        public void destroyObject(PoolKey key, PooledObject<RedisAsyncConnection<K, V>> p) throws Exception {
            p.getObject().close();
        }

        @Override
        public PooledObject<RedisAsyncConnection<K, V>> wrap(RedisAsyncConnection<K, V> value) {
            return new DefaultPooledObject<RedisAsyncConnection<K, V>>(value);
        }
    }

    @Override
    public void close() {
        if (partitionPool != null) {
            partitionPool.close();
        }
        partitionPool = null;
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
