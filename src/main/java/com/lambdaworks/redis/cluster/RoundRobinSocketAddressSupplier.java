package com.lambdaworks.redis.cluster;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.SocketAddressResolver;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Round-Robin socket address supplier. Cluster nodes are iterated circular/infinitely.
 *
 * @author Mark Paluch
 */
class RoundRobinSocketAddressSupplier implements Supplier<SocketAddress> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RoundRobinSocketAddressSupplier.class);
    private final Collection<RedisClusterNode> partitions;
    private final Collection<RedisClusterNode> clusterNodes = new ArrayList<>();
    private final Function<Collection<RedisClusterNode>, Collection<RedisClusterNode>> sortFunction;
    private final ClientResources clientResources;
    private RoundRobin<? extends RedisClusterNode> roundRobin;

    public RoundRobinSocketAddressSupplier(Collection<RedisClusterNode> partitions,
            Function<Collection<RedisClusterNode>, Collection<RedisClusterNode>> sortFunction, ClientResources clientResources) {

        LettuceAssert.notNull(partitions, "Partitions must not be null");
        LettuceAssert.notNull(sortFunction, "Sort-Function must not be null");

        this.partitions = partitions;
        this.clusterNodes.addAll(partitions);
        this.roundRobin = new RoundRobin<>(clusterNodes);
        this.sortFunction = sortFunction;
        this.clientResources = clientResources;
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
        clusterNodes.addAll(sortFunction.apply(partitions));
        roundRobin.offset = null;
    }

    protected SocketAddress getSocketAddress(RedisClusterNode redisClusterNode) {
        SocketAddress resolvedAddress = SocketAddressResolver.resolve(redisClusterNode.getUri(), clientResources.dnsResolver());
        logger.debug("Resolved SocketAddress {} using for Cluster node {}", resolvedAddress, redisClusterNode.getNodeId());
        return resolvedAddress;
    }
}
