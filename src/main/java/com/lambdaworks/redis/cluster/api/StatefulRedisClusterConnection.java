package com.lambdaworks.redis.cluster.api;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.rx.RedisAdvancedClusterReactiveCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;

/**
 * A stateful cluster connection providing. Advanced cluster connections provide transparent command routing based on the first
 * command key.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface StatefulRedisClusterConnection<K, V> extends StatefulConnection<K, V> {

    /**
     * Returns the {@link RedisAdvancedClusterCommands} API for the current connection. Does not create a new connection.
     *
     * @return the synchronous API for the underlying connection.
     */
    RedisAdvancedClusterCommands<K, V> sync();

    /**
     * Returns the {@link RedisAdvancedClusterAsyncCommands} API for the current connection. Does not create a new connection.
     *
     * @return the asynchronous API for the underlying connection.
     */
    RedisAdvancedClusterAsyncCommands<K, V> async();

    /**
     * Returns the {@link RedisAdvancedClusterReactiveCommands} API for the current connection. Does not create a new
     * connection.
     *
     * @return the reactive API for the underlying connection.
     */
    RedisAdvancedClusterReactiveCommands<K, V> reactive();

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list. This
     * connection is bound to the node id. Once the cluster topology view is updated, the connection will try to reconnect the
     * to the node with the specified {@code nodeId}, that behavior can also lead to a closed connection once the node with the
     * specified {@code nodeId} is no longer part of the cluster.
     *
     * Do not close the connections. Otherwise, unpredictable behavior will occur. The nodeId must be part of the cluster and is
     * validated against the current topology view in {@link com.lambdaworks.redis.cluster.models.partitions.Partitions}.
     * 
     *
     * In contrast to the {@link StatefulRedisClusterConnection}, node-connections do not route commands to other cluster nodes.
     *
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     * @throws RedisException if the requested node identified by {@code nodeId} is not part of the cluster
     */
    StatefulRedisConnection<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. This connection is bound to a host and port.
     * Updates to the cluster topology view can close the connection once the host, identified by {@code host} and {@code port},
     * are no longer part of the cluster.
     *
     * Do not close the connections. Otherwise, unpredictable behavior will occur. The node must be part of the cluster and
     * host/port are validated (exact check) against the current topology view in
     * {@link com.lambdaworks.redis.cluster.models.partitions.Partitions}.
     *
     * In contrast to the {@link StatefulRedisClusterConnection}, node-connections do not route commands to other cluster nodes.
     *
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     * @throws RedisException if the requested node identified by {@code host} and {@code port} is not part of the cluster
     */
    StatefulRedisConnection<K, V> getConnection(String host, int port);

    /**
     * Set from which nodes data is read. The setting is used as default for read operations on this connection. See the
     * documentation for {@link ReadFrom} for more information.
     * 
     * @param readFrom the read from setting, must not be {@literal null}
     */
    void setReadFrom(ReadFrom readFrom);

    /**
     * Gets the {@link ReadFrom} setting for this connection. Defaults to {@link ReadFrom#MASTER} if not set.
     * 
     * @return the read from setting
     */
    ReadFrom getReadFrom();

    /**
     *
     * @return Known partitions for this connection.
     */
    Partitions getPartitions();
}
