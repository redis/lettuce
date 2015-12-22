package com.lambdaworks.redis.cluster;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.SocketAddress;
import java.util.Collection;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Round-Robin socket address supplier. Cluster nodes are iterated circular/infinitely.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class RoundRobinSocketAddressSupplier implements Supplier<SocketAddress> {

    protected final Collection<RedisClusterNode> clusterNodes = Lists.newArrayList();
    protected final Partitions partitions;
    protected RoundRobin<? extends RedisClusterNode> roundRobin;

    public RoundRobinSocketAddressSupplier(Partitions partitions) {
        checkArgument(partitions != null, "Partitions must not be null");
        this.partitions = partitions;
        this.roundRobin = new RoundRobin<>(clusterNodes);
        resetRoundRobin();
    }

    @Override
    public SocketAddress get() {
        if (!clusterNodes.containsAll(partitions)) {
            resetRoundRobin();
        }

        RedisClusterNode redisClusterNode = roundRobin.next();
        return getSocketAddress(redisClusterNode);

    }

    protected void resetRoundRobin() {
        clusterNodes.clear();
        clusterNodes.addAll(ClusterTopologyRefresh.createSortedList(partitions));
        roundRobin.offset = null;
    }

    protected static SocketAddress getSocketAddress(RedisClusterNode redisClusterNode) {
        return redisClusterNode.getUri().getResolvedAddress();
    }
}