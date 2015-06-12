package com.lambdaworks.redis.cluster;

import java.util.function.Predicate;

import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.cluster.api.NodeSelection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Advanced asynchronous and thread-safe cluster API.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 * @deprecated Use {@link com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands}
 */
@Deprecated
public interface RedisAdvancedClusterAsyncConnection<K, V> extends RedisClusterAsyncConnection<K, V> {

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list.
     * 
     * In contrast to the {@link RedisAdvancedClusterAsyncConnection}, node-connections do not route commands to other cluster
     * nodes
     * 
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    RedisClusterAsyncConnection<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. In contrast to the
     * {@link RedisAdvancedClusterAsyncConnection}, node-connections do not route commands to other cluster nodes
     * 
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    RedisClusterAsyncConnection<K, V> getConnection(String host, int port);

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterConnection<K, V> getStatefulConnection();

}
