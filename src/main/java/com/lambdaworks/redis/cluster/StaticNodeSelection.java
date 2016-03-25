package com.lambdaworks.redis.cluster;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Static selection of nodes.
 * 
 * @param <API> API type.
 * @param <CMD> Command command interface type to invoke multi-node operations.
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
abstract class StaticNodeSelection<API, CMD, K, V> extends AbstractNodeSelection<API, CMD, K, V> {

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
