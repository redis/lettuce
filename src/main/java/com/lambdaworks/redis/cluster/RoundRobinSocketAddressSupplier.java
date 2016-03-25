package com.lambdaworks.redis.cluster;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.function.Function;

import com.google.common.base.Supplier;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.internal.LettuceLists;

/**
 * Round-Robin socket address supplier. Cluster nodes are iterated circular/infinitely.
 * 
 * @author Mark Paluch
 */
class RoundRobinSocketAddressSupplier implements Supplier<SocketAddress> {

    private final Collection<RedisClusterNode> partitions;
    private final Collection<RedisClusterNode> clusterNodes = LettuceLists.newList();
    private final Function<Collection<RedisClusterNode>, Collection<RedisClusterNode>> sort;
    private RoundRobin<? extends RedisClusterNode> roundRobin;

    public RoundRobinSocketAddressSupplier(Collection<RedisClusterNode> partitions,
            Function<Collection<RedisClusterNode>, Collection<RedisClusterNode>> sort) {
        this.sort = sort;
        LettuceAssert.notNull(partitions, "Partitions must not be null");
        LettuceAssert.notNull(sort, "Sort-Function must not be null");
        this.partitions = partitions;
        this.clusterNodes.addAll(partitions);
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
        clusterNodes.addAll(sort.apply(partitions));
        roundRobin.offset = null;
    }

    protected static SocketAddress getSocketAddress(RedisClusterNode redisClusterNode) {
        return redisClusterNode.getUri().getResolvedAddress();
    }
}