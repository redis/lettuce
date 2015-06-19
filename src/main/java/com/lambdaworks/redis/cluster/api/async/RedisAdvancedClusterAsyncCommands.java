package com.lambdaworks.redis.cluster.api.async;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterAsyncConnection;
import com.lambdaworks.redis.cluster.api.NodeSelection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Advanced asynchronous and thread-safe Redis Cluster API.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
public interface RedisAdvancedClusterAsyncCommands<K, V> extends RedisClusterAsyncCommands<K, V>,
        RedisAdvancedClusterAsyncConnection<K, V> {

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list.
     * 
     * In contrast to the {@link RedisAdvancedClusterAsyncCommands}, node-connections do not route commands to other cluster
     * nodes
     * 
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    RedisClusterAsyncCommands<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. In contrast to the
     * {@link RedisAdvancedClusterAsyncCommands}, node-connections do not route commands to other cluster nodes
     * 
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    RedisClusterAsyncCommands<K, V> getConnection(String host, int port);

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterConnection<K, V> getStatefulConnection();

    /**
     * Select all masters.
     *
     * @return API with asynchronous executed commands on a selection of master cluster nodes.
     */
    default AsyncNodeSelection<K, V> masters() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MASTER));
    }

    /**
     * Select all slaves.
     *
     * @return API with asynchronous executed commands on a selection of slave cluster nodes.
     */
    default AsyncNodeSelection<K, V> slaves() {
        return readonly(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE));
    }

    /**
     * Select all slaves.
     *
     * @param predicate Predicate to filter nodes
     * @return API with asynchronous executed commands on a selection of slave cluster nodes.
     */
    default AsyncNodeSelection<K, V> slaves(Predicate<RedisClusterNode> predicate) {
        return readonly(redisClusterNode -> predicate.test(redisClusterNode)
                && redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE));
    }

    /**
     * Select all known cluster nodes.
     *
     * @return API with asynchronous executed commands on a selection of all luster nodes.
     */
    default AsyncNodeSelection<K, V> all() {
        return nodes(redisClusterNode -> true);
    }

    /**
     * Select slave nodes by a predicate and keeps a static selection. Slave connections operate in {@literal READONLY} mode.
     * The set of nodes within the {@link NodeSelection} does not change when the cluster view changes.
     *
     * @param predicate Predicate to filter nodes
     * @return a {@linkplain NodeSelectionAsyncCommands} matching {@code predicate}
     */
    AsyncNodeSelection<K, V> readonly(Predicate<RedisClusterNode> predicate);

    /**
     * Select nodes by a predicate and keeps a static selection. The set of nodes within the {@link NodeSelection} does not
     * change when the cluster view changes.
     *
     * @param predicate Predicate to filter nodes
     * @return a {@linkplain NodeSelectionAsyncCommands} matching {@code predicate}
     */
    AsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate);

    /**
     * Select nodes by a predicate
     *
     * @param predicate Predicate to filter nodes
     * @param dynamic Defines, whether the set of nodes within the {@link NodeSelection} can change when the cluster view
     *        changes.
     * @return a {@linkplain NodeSelection} matching {@code predicate}
     */
    AsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate, boolean dynamic);

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
     * @param map the map
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
