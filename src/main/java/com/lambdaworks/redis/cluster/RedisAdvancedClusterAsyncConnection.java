package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;

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
     * Do not close the connections. Otherwise, unpredictable behavior will occur. The nodeId must be part of the cluster and is
     * validated against the current topology view in {@link com.lambdaworks.redis.cluster.models.partitions.Partitions}.
     * 
     * In contrast to the {@link RedisAdvancedClusterAsyncConnection}, node-connections do not route commands to other cluster
     * nodes
     * 
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     * @throws RedisException if the requested node identified by {@code nodeId} is not part of the cluster
     */
    RedisClusterAsyncConnection<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Do not close the connections. Otherwise,
     * unpredictable behavior will occur. The node must be part of the cluster and host/port are validated (exact check) against
     * the current topology view in {@link com.lambdaworks.redis.cluster.models.partitions.Partitions}.
     * 
     * In contrast to the {@link RedisAdvancedClusterAsyncConnection}, node-connections do not route commands to other cluster
     * nodes.
     * 
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     * @throws RedisException if the requested node identified by {@code host} and {@code port} is not part of the cluster
     */
    RedisClusterAsyncConnection<K, V> getConnection(String host, int port);

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterConnection<K, V> getStatefulConnection();

}
