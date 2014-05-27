package com.lambdaworks.redis.cluster;

import java.net.SocketAddress;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import com.google.common.base.Supplier;
import com.lambdaworks.redis.Connections;
import com.lambdaworks.redis.RedisAsyncConnection;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.codec.RedisCodec;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:54
 */
public class PooledClusterConnectionProvider<K, V> implements ClusterConnectionProvider {
    private KeyedObjectPool<PoolKey, RedisAsyncConnection<K, V>> partitionPool;
    private boolean readSlaveOk = false;
    private Partitions partitions;
    private RedisClusterClient redisClusterClient;
    private RedisCodec<K, V> redisCodec;

    public PooledClusterConnectionProvider(RedisClusterClient redisClusterClient, Partitions partitions,
            RedisCodec<K, V> redisCodec) {
        this.partitions = partitions;
        this.redisClusterClient = redisClusterClient;

        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setTestOnBorrow(true);

        partitionPool = new GenericKeyedObjectPool<PoolKey, RedisAsyncConnection<K, V>>(new KeyedConnectionFactory<K, V>(
                redisClusterClient, redisCodec), config);

    }

    @Override
    public RedisAsyncConnectionImpl<K, V> getConnection(int hash, Intent intent) {
        RedisClusterPartition partition = partitions.getPartitionByHash(hash);

        try {
            PoolKey key = new PoolKey(intent, partition);
            RedisAsyncConnection<K, V> connection = partitionPool.borrowObject(key);
            partitionPool.returnObject(key, connection);
            return (RedisAsyncConnectionImpl<K, V>) connection;
        } catch (Exception e) {
            throw new RedisException(e);
        }

    }

    private static class KeyedConnectionFactory<K, V> extends BaseKeyedPooledObjectFactory<PoolKey, RedisAsyncConnection<K, V>> {
        private RedisClusterClient redisClusterClient;
        private RedisCodec<K, V> redisCodec;

        private KeyedConnectionFactory(RedisClusterClient redisClusterClient, RedisCodec<K, V> redisCodec) {
            this.redisClusterClient = redisClusterClient;
            this.redisCodec = redisCodec;
        }

        @Override
        public RedisAsyncConnection<K, V> create(final PoolKey key) throws Exception {
            return redisClusterClient.connectCluster(redisCodec, new Supplier<SocketAddress>() {
                @Override
                public SocketAddress get() {
                    return key.getPartition().getUri().getResolvedAddress();
                }
            });
        }

        @Override
        public boolean validateObject(PoolKey key, PooledObject<RedisAsyncConnection<K, V>> p) {
            return Connections.isValid(p);
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

    private static class PoolKey {
        private ClusterConnectionProvider.Intent intent;
        private RedisClusterPartition partition;

        private PoolKey(ClusterConnectionProvider.Intent intent, RedisClusterPartition partition) {
            this.intent = intent;
            this.partition = partition;
        }

        public ClusterConnectionProvider.Intent getIntent() {
            return intent;
        }

        public RedisClusterPartition getPartition() {
            return partition;
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

            if (intent != poolKey.intent) {
                return false;
            }
            if (partition != null ? !partition.equals(poolKey.partition) : poolKey.partition != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = intent != null ? intent.hashCode() : 0;
            result = 31 * result + (partition != null ? partition.hashCode() : 0);
            return result;
        }
    }

}
