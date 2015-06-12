package com.lambdaworks.redis.cluster.api.async;

import java.util.function.Predicate;

import com.lambdaworks.redis.cluster.RedisAdvancedClusterAsyncConnection;
import com.lambdaworks.redis.cluster.api.NodeSelection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Advanced asynchronous and thread-safe cluster API.
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
}
