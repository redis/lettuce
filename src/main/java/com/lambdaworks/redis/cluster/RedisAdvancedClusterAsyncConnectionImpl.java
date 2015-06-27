package com.lambdaworks.redis.cluster;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisAsyncConnectionImpl;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
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

    ClusterDistributionChannelWriter<K, V> getWriter() {
        return (ClusterDistributionChannelWriter) super.getChannelWriter();
    }

    @Override
    public RedisClusterAsyncConnection<K, V> getConnection(String nodeId) {
        RedisURI redisURI = lookup(nodeId);
        if (redisURI == null) {
            throw new RedisException("NodeId " + nodeId + " does not belong to the cluster");
        }

        return getConnection(redisURI.getHost(), redisURI.getPort());
    }

    private RedisURI lookup(String nodeId) {

        for (RedisClusterNode partition : partitions) {
            if (partition.getNodeId().equals(nodeId)) {
                return partition.getUri();
            }
        }
        return null;
    }

    @Override
    public RedisClusterAsyncConnection<K, V> getConnection(String host, int port) {
        // there is currently no check whether the node belongs to the cluster or not.
        // A check against the partition table could be done, but this reflects only a particular
        // point of view. What if the cluster is multi-homed, proxied, natted...?

        RedisAsyncConnectionImpl<K, V> connection = getWriter().getClusterConnectionProvider().getConnection(
                ClusterConnectionProvider.Intent.WRITE, host, port);

        return connection;
    }

    public void setPartitions(Partitions partitions) {
        getWriter().getClusterConnectionProvider().setPartitions(partitions);
        this.partitions = partitions;
    }
}
