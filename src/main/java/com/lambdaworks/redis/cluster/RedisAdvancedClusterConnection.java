package com.lambdaworks.redis.cluster;

import java.util.function.Predicate;

import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface RedisAdvancedClusterConnection<K, V> extends RedisClusterAsyncConnection<K, V> {

    /**
     * Select all masters.
     * 
     * @return
     */
    default NodeSelectionAsyncOperations<K, V> masters() {
        return nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.MASTER));
    }

    /**
     * Select all slaves.
     * 
     * @return
     */
    default NodeSelectionAsyncOperations<K, V> slaves() {
        return nodes(redisClusterNode -> redisClusterNode.getFlags().contains(RedisClusterNode.NodeFlag.SLAVE));
    }

    /**
     * Select all known cluster nodes.
     * 
     * @return
     */
    default NodeSelectionAsyncOperations<K, V> all() {
        return nodes(redisClusterNode -> true);
    }

    /**
     * Select nodes by a predicate and keeps a static selection. The set of nodes within the {@link NodeSelection} does not
     * change when the cluster view changes.
     * 
     * @param predicate
     * @return a {@linkplain NodeSelectionAsyncOperations} matching {@code predicate}
     */
    NodeSelectionAsyncOperations<K, V> nodes(Predicate<RedisClusterNode> predicate);

    /**
     * Select nodes by a predicate
     *
     * @param predicate
     * @param dynamic Defines, whether the set of nodes within the {@link NodeSelection} can change when the cluster view
     *        changes.
     * @return a {@linkplain NodeSelection} matching {@code predicate}
     */
    NodeSelectionAsyncOperations<K, V> nodes(Predicate<RedisClusterNode> predicate, boolean dynamic);

    /**
     *
     * @return the Partitions/Cluster view.
     */
    Partitions getPartitions();
}
