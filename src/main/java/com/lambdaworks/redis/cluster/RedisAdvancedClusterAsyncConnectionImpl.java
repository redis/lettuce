package com.lambdaworks.redis.cluster;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import io.netty.channel.ChannelHandler;

/**
 * Advanced asynchronous Cluster connection.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 */
@ChannelHandler.Sharable
public class RedisAdvancedClusterAsyncConnectionImpl<K, V> extends RedisAsyncConnectionImpl<K, V> implements
        RedisAdvancedClusterAsyncConnection<K, V> {

    private Partitions partitions;

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public RedisAdvancedClusterAsyncConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, codec, timeout, unit);
    }

    @SuppressWarnings("unchecked")
    ClusterDistributionChannelWriter<K, V> getWriter() {
        return (ClusterDistributionChannelWriter<K, V>) super.getChannelWriter();
    }

    @Override
    public RedisClusterAsyncConnection<K, V> getConnection(String nodeId) {

        RedisAsyncConnectionImpl<K, V> connection = getWriter().getClusterConnectionProvider().getConnection(
                ClusterConnectionProvider.Intent.WRITE, nodeId);

        return connection;
    }

    @Override
    public RedisFuture<List<K>> clusterGetKeysInSlot(int slot, int count) {
        RedisClusterAsyncConnection<K, V> connectionBySlot = findConnectionBySlot(slot);

        if (connectionBySlot != null) {
            return connectionBySlot.clusterGetKeysInSlot(slot, count);
        }

        return super.clusterGetKeysInSlot(slot, count);
    }

    @Override
    public RedisFuture<Long> clusterCountKeysInSlot(int slot) {
        RedisClusterAsyncConnection<K, V> connectionBySlot = findConnectionBySlot(slot);

        if (connectionBySlot != null) {
            return connectionBySlot.clusterCountKeysInSlot(slot);
        }

        return super.clusterCountKeysInSlot(slot);
    }

    private RedisClusterAsyncConnection<K, V> findConnectionBySlot(int slot) {
        RedisClusterNode node = partitions.getPartitionBySlot(slot);
        if (node != null) {
            return getConnection(node.getUri().getHost(), node.getUri().getPort());
        }

        return null;
    }

    @Override
    public RedisClusterAsyncConnection<K, V> getConnection(String host, int port) {
        RedisAsyncConnectionImpl<K, V> connection = getWriter().getClusterConnectionProvider().getConnection(
                ClusterConnectionProvider.Intent.WRITE, host, port);

        return connection;
    }

    public void setPartitions(Partitions partitions) {
        getWriter().getClusterConnectionProvider().setPartitions(partitions);
        this.partitions = partitions;
    }

    @Override
    public void setReadFrom(ReadFrom readFrom) {
        checkArgument(readFrom != null, "readFrom must not be null");
        getWriter().setReadFrom(readFrom);
    }

    @Override
    public ReadFrom getReadFrom() {
        return getWriter().getReadFrom();
    }

}
