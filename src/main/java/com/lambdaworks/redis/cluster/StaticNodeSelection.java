package com.lambdaworks.redis.cluster;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class StaticNodeSelection<K, V> extends AbstractNodeSelection<K, V> {

    private final List<RedisClusterNode> redisClusterNodes;

    public StaticNodeSelection(RedisAdvancedClusterConnectionImpl<K, V> globalConnection, Predicate<RedisClusterNode> selector) {
        super(globalConnection);

        this.redisClusterNodes = globalConnection.getPartitions().getPartitions().stream().filter(selector)
                .collect(Collectors.toList());
    }

    @Override
    protected List<RedisClusterNode> nodes() {
        return redisClusterNodes;
    }
}
