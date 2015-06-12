package com.lambdaworks.redis.cluster;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class DynamicAsyncNodeSelection<K, V, CMDType> extends DynamicNodeSelection<RedisAsyncCommands<K, V>, CMDType, K, V> {

    public DynamicAsyncNodeSelection(StatefulRedisClusterConnection<K, V> globalConnection,
            Predicate<RedisClusterNode> selector, ClusterConnectionProvider.Intent intent) {
        super(globalConnection, selector, intent);
    }

    @Override
    public Iterator<RedisAsyncCommands<K, V>> iterator() {
        List<RedisClusterNode> list = ImmutableList.copyOf(nodes());
        return list.stream().map(node -> getConnection(node).async()).iterator();
    }

    @Override
    public RedisAsyncCommands<K, V> node(int index) {
        return statefulMap().get(nodes().get(index)).async();
    }

    @Override
    public Map<RedisClusterNode, RedisAsyncCommands<K, V>> asMap() {

        List<RedisClusterNode> list = ImmutableList.copyOf(nodes());
        Map<RedisClusterNode, RedisAsyncCommands<K, V>> map = Maps.newHashMap();

        list.forEach((key) -> map.put(key, getConnection(key).async()));

        return map;
    }
}
