package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;

/**
 * Advanced synchronous and thread-safe cluster API.
 * 
 * @author Mark Paluch
 * @since 3.3
 * @deprecated Use {@link com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands}
 */
@Deprecated
public interface RedisAdvancedClusterConnection<K, V> extends RedisClusterConnection<K, V> {

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list. This
     * connection is bound to the node id. Once the cluster topology view is updated, the connection will try to reconnect the
     * to the node with the specified {@code nodeId}, that behavior can also lead to a closed connection once the node with the
     * specified {@code nodeId} is no longer part of the cluster.
     *
     * Do not close the connections. Otherwise, unpredictable behavior will occur. The nodeId must be part of the cluster and is
     * validated against the current topology view in {@link com.lambdaworks.redis.cluster.models.partitions.Partitions}.
     *
     * In contrast to the {@link RedisAdvancedClusterConnection}, node-connections do not route commands to other cluster nodes.
     *
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     * @throws RedisException if the requested node identified by {@code nodeId} is not part of the cluster
     */
    RedisClusterConnection<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. This connection is bound to a host and port.
     * Updates to the cluster topology view can close the connection once the host, identified by {@code host} and {@code port},
     * are no longer part of the cluster.
     * 
     * Do not close the connections. Otherwise, unpredictable behavior will occur. The node must be part of the cluster and
     * host/port are validated (exact check) against the current topology view in
     * {@link com.lambdaworks.redis.cluster.models.partitions.Partitions}.
     *
     * In contrast to the {@link RedisAdvancedClusterConnection}, node-connections do not route commands to other cluster nodes.
     * 
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     * @throws RedisException if the requested node identified by {@code host} and {@code port} is not part of the cluster
     */
    RedisClusterConnection<K, V> getConnection(String host, int port);

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterConnection<K, V> getStatefulConnection();

}
