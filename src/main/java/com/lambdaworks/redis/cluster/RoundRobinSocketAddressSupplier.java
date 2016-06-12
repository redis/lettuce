package com.lambdaworks.redis.cluster;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.SocketAddressResolver;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Round-Robin socket address supplier. Cluster nodes are iterated circular/infinitely.
 *
 * @author Mark Paluch
 */
class RoundRobinSocketAddressSupplier implements Supplier<SocketAddress> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RoundRobinSocketAddressSupplier.class);
    private final Collection<RedisClusterNode> partitions;
    private final Collection<RedisClusterNode> clusterNodes = new ArrayList<RedisClusterNode>();
    private final Function<Collection<RedisClusterNode>, Collection<RedisClusterNode>> sortFunction;
    private final ClientResources clientResources;
    private RoundRobin<? extends RedisClusterNode> roundRobin;

    public RoundRobinSocketAddressSupplier(Collection<RedisClusterNode> partitions,
            Function<? extends Collection<RedisClusterNode>, Collection<RedisClusterNode>> sortFunction,
            ClientResources clientResources) {

        checkArgument(partitions != null, "Partitions must not be null");
        checkArgument(sortFunction != null, "Sort-Function must not be null");

        this.partitions = partitions;
        this.clusterNodes.addAll(partitions);
        this.roundRobin = new RoundRobin<RedisClusterNode>(clusterNodes);
        this.sortFunction = (Function) sortFunction;
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