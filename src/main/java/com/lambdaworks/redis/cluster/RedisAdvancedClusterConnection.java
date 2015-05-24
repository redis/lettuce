package com.lambdaworks.redis.cluster;

import java.util.function.Predicate;

import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface RedisAdvancedClusterConnection<K, V> extends RedisClusterConnection<K, V> {

    /**
     * Select all masters.
     * 
     * @return
     */
    default NodeSelection<K, V> masters() {
        return nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MASTER));
    }

    /**
     * Select all slaves.
     * 
     * @return
     */
    default NodeSelection<K, V> slaves() {
        return nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.SLAVE));
    }

    /**
     * Select all known cluster nodes.
     * 
     * @return
     */
    default NodeSelection<K, V> all() {
        return nodes(redisClusterNode -> true);
    }

    /**
     * Select nodes by a predicate
     * 
     * @param predicate
     * @return
     */
    NodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate);
}
