package com.lambdaworks.redis.cluster;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class StaticNodeSelection<T, CMDType, K, V> extends AbstractNodeSelection<T, CMDType, K, V> {

    private final List<RedisClusterNode> redisClusterNodes;

    public StaticNodeSelection(StatefulRedisClusterConnection<K, V> globalConnection, Predicate<RedisClusterNode> selector,
            ClusterConnectionProvider.Intent intent) {
        super(globalConnection, intent);

        this.redisClusterNodes = globalConnection.getPartitions().getPartitions().stream().filter(selector)
                .collect(Collectors.toList());
    }

    @Override
    protected List<RedisClusterNode> nodes() {
        return redisClusterNodes;
    }
}
