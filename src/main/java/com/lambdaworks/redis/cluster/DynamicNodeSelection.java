package com.lambdaworks.redis.cluster;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class DynamicNodeSelection<K, V> extends AbstractNodeSelection<K, V> {

    private final Predicate<RedisClusterNode> selector;

    public DynamicNodeSelection(RedisAdvancedClusterConnectionImpl<K, V> globalConnection, Predicate<RedisClusterNode> selector) {
        super(globalConnection);

        this.selector = selector;
    }

    @Override
    protected List<RedisClusterNode> nodes() {
        return globalConnection.getPartitions().getPartitions().stream().filter(selector).collect(Collectors.toList());
    }
}
