package com.lambdaworks.redis.cluster;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class DynamicNodeSelection<T, K, V, CMDType> extends AbstractNodeSelection<T, CMDType, K, V> {

    private final Predicate<RedisClusterNode> selector;

    public DynamicNodeSelection(StatefulRedisClusterConnection<K, V> globalConnection, Predicate<RedisClusterNode> selector,
            ClusterConnectionProvider.Intent intent) {
        super(globalConnection, intent);
        this.selector = selector;
    }

    @Override
    protected List<RedisClusterNode> nodes() {
        return globalConnection.getPartitions().getPartitions().stream().filter(selector).collect(Collectors.toList());
    }
}
