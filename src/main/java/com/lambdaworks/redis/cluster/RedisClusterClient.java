package com.lambdaworks.redis.cluster;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.AbstractRedisClient;
import com.lambdaworks.redis.BaseRedisAsyncConnection;
import com.lambdaworks.redis.BaseRedisConnection;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnectionStateListener;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandHandler;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:08
 */
public class RedisClusterClient extends AbstractRedisClient {
    private RedisClient redisClient;
    private Partitions partitions;

    private List<RedisURI> initialUris = Lists.newArrayList();

    public RedisClusterClient(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    public RedisClusterClient(List<RedisURI> initialUris) {
        this.initialUris = initialUris;
    }

    public void setDefaultTimeout(long timeout, TimeUnit unit) {
        redisClient.setDefaultTimeout(timeout, unit);
    }

    public <T extends BaseRedisConnection<String, String>> T connect() {
        if (partitions == null) {
            initializePartitions();
        }

        return redisClient.connect();
    }

    public <T extends BaseRedisAsyncConnection<String, String>> T connectAsync() {
        if (partitions == null) {
            initializePartitions();
        }
        return redisClient.connectAsync();
    }

    public <K, V, T extends BaseRedisConnection<K, V>> T connect(RedisCodec<K, V> codec) {
        if (partitions == null) {
            initializePartitions();
        }
        return redisClient.connect(codec);
    }

    public <K, V, T extends BaseRedisAsyncConnection<K, V>> T connectAsync(RedisCodec<K, V> codec) {
        if (partitions == null) {
            initializePartitions();
        }
        return redisClient.connectAsync(codec);
    }

    private void initializePartitions() {

        Partitions partitions = getPartitions();
        this.partitions = partitions;

    }

    private Partitions getPartitions() {
        String clusterNodes = null;
        RedisURI nodeUri = null;
        for (RedisURI initialUri : initialUris) {

            RedisAsyncConnectionImpl<String, String> connection = connectAsyncImpl(initialUri.getResolvedAddress());
            nodeUri = initialUri;

            try {
                clusterNodes = connection.clusterNodes().get();
            } catch (InterruptedException e) {
                throw new RedisException(e);
            } catch (ExecutionException e) {
                throw new RedisException(e);
            }
            connection.close();
            break;
        }

        if (clusterNodes == null) {
            throw new RedisException("Cannot retrieve initial cluster partitions from initial URIs " + initialUris);
        }

        Partitions partitions = ClusterPartitionParser.parse(clusterNodes);

        for (RedisClusterNode partition : partitions) {
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                partition.setUri(nodeUri);
            }
        }
        return partitions;
    }

    RedisAsyncConnectionImpl<String, String> connectAsyncImpl(final SocketAddress socketAddress) {
        BlockingQueue<Command<String, String, ?>> queue = new LinkedBlockingQueue<Command<String, String, ?>>();

        CommandHandler<String, String> handler = new CommandHandler<String, String>(queue);
        RedisAsyncConnectionImpl<String, String> connection = new RedisAsyncConnectionImpl<String, String>(handler,
                new Utf8StringCodec(), timeout, unit);

        return connectAsyncImpl(handler, connection, new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {
                return socketAddress;
            }
        }, false);
    }

    public <K, V> RedisAsyncConnectionImpl<K, V> connectCluster(RedisCodec<K, V> codec) {
        BlockingQueue<Command<K, V, ?>> queue = new LinkedBlockingQueue<Command<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(queue);

        PooledClusterConnectionProvider<K, V> pooledClusterConnectionProvider = new PooledClusterConnectionProvider<K, V>(this,
                partitions, codec);

        ClusterDistributionChannelWriter<K, V> clusterWriter = new ClusterDistributionChannelWriter<K, V>(handler,
                pooledClusterConnectionProvider);
        RedisAsyncConnectionImpl<K, V> connection = new RedisAsyncConnectionImpl<K, V>(clusterWriter, codec, timeout, unit);

        return connectAsyncImpl(handler, connection, new Supplier<SocketAddress>() {
            @Override
            public SocketAddress get() {
                // FEATURE: Balancing/reconnecting to different nodes.
                return partitions.iterator().next().getUri().getResolvedAddress();
            }
        }, true);
    }

    <K, V> RedisAsyncConnectionImpl<K, V> connectCluster(RedisCodec<K, V> codec,
            final Supplier<SocketAddress> socketAddressSupplier) {
        BlockingQueue<Command<K, V, ?>> queue = new LinkedBlockingQueue<Command<K, V, ?>>();

        CommandHandler<K, V> handler = new CommandHandler<K, V>(queue);
        RedisAsyncConnectionImpl<K, V> connection = new RedisAsyncConnectionImpl<K, V>(handler, codec, timeout, unit);

        RedisAsyncConnectionImpl<K, V> result = connectAsyncImpl(handler, connection, socketAddressSupplier, true);

        if (initialUris.get(0).getPassword() != null) {
            result.auth(new String(initialUris.get(0).getPassword()));
        }

        return result;

    }

    protected RedisClusterNode getPartition(int hash) {
        return partitions.getPartitionBySlot(hash);
    }

    public void shutdown() {
        redisClient.shutdown();
    }

    public void addListener(RedisConnectionStateListener listener) {
        redisClient.addListener(listener);
    }

    public void removeListener(RedisConnectionStateListener listener) {
        redisClient.removeListener(listener);
    }

}
