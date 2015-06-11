package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;

/**
 * Advanced synchronous and thread-safe cluster API.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 */
public interface RedisAdvancedClusterConnection<K, V> extends RedisClusterConnection<K, V> {

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list. In
     * contrast to the {@link RedisAdvancedClusterConnection}, node-connections do not route commands to other cluster nodes
     * 
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    RedisClusterConnection<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. In contrast to the
     * {@link RedisAdvancedClusterConnection}, node-connections do not route commands to other cluster nodes
     * 
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    RedisClusterConnection<K, V> getConnection(String host, int port);

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterConnection<K, V> getStatefulConnection();

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

    /**
     * Delete a key with pipelining. Cross-slot keys will result in multiple calls to the particular cluster nodes.
     * 
     * @param keys the key
     * @return RedisFuture&lt;Long&gt; integer-reply The number of keys that were removed.
     */
    RedisFuture<Long> del(K... keys);

    /**
     * Get the values of all the given keys with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     * 
     * @param keys the key
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of values at the specified keys.
     */
    RedisFuture<List<V>> mget(K... keys);

    /**
     * Set multiple keys to multiple values with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     * 
     * @param map the null
     * @return RedisFuture&lt;String&gt; simple-string-reply always {@code OK} since {@code MSET} can't fail.
     */
    RedisFuture<String> mset(Map<K, V> map);

    /**
     * Set multiple keys to multiple values, only if none of the keys exist with pipelining. Cross-slot keys will result in
     * multiple calls to the particular cluster nodes.
     * 
     * @param map the null
     * @return RedisFuture&lt;Boolean&gt; integer-reply specifically:
     * 
     *         {@code 1} if the all the keys were set. {@code 0} if no key was set (at least one key already existed).
     */
    RedisFuture<Boolean> msetnx(Map<K, V> map);
}
