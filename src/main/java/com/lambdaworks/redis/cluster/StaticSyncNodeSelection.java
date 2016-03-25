package com.lambdaworks.redis.cluster;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @param <CMD> Command command interface type to invoke multi-node operations.
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
class StaticSyncNodeSelection<CMD, K, V> extends StaticNodeSelection<RedisCommands<K, V>, CMD, K, V> {

    public StaticSyncNodeSelection(StatefulRedisClusterConnection<K, V> globalConnection, Predicate<RedisClusterNode> selector,
            ClusterConnectionProvider.Intent intent) {
        super(globalConnection, selector, intent);
    }

    public Iterator<RedisCommands<K, V>> iterator() {
        List<RedisClusterNode> list = ImmutableList.copyOf(nodes());
        return list.stream().map(node -> getConnection(node).sync()).iterator();
    }

    @Override
    public RedisCommands<K, V> commands(int index) {
        return statefulMap().get(nodes().get(index)).sync();
    }

    @Override
    public Map<RedisClusterNode, RedisCommands<K, V>> asMap() {

        List<RedisClusterNode> list = ImmutableList.copyOf(nodes());
        Map<RedisClusterNode, RedisCommands<K, V>> map = Maps.newHashMap();

        list.forEach((key) -> map.put(key, getConnection(key).sync()));

        return map;
    }

    // This method is never called, the value is supplied by AOP magic.
    @Override
    public CMD commands() {
        return null;
    }
}
