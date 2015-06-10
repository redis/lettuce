package com.lambdaworks.redis.cluster.api;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterAsyncConnection;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterConnection;

/**
 * A stateful cluster connection providing. Advanced cluster connections provide transparent command routing based on the first
 * command key.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
public interface StatefulRedisClusterConnection<K, V> extends StatefulConnection<K, V> {

    /**
     * Returns the {@link RedisAdvancedClusterAsyncConnection} API for the current connection. Does not create a new connection.
     *
     * @return the asynchronous API for the underlying connection.
     */
    RedisAdvancedClusterAsyncConnection<K, V> async();

    /**
     * Returns the {@link RedisAdvancedClusterConnection} API for the current connection. Does not create a new connection.
     *
     * @return the synchronous API for the underlying connection.
     */
    RedisAdvancedClusterConnection<K, V> sync();

    /**
     * Retrieve a stateful connection to the specified cluster node using the nodeId. Host and port are looked up in the node
     * list. In contrast to the RedisAdvancedClusterConnection, node-connections do not route commands to other cluster nodes.
     * 
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    StatefulRedisConnection<K, V> getConnection(String nodeId);

    /**
     * Retrieve a stateful connection to the specified cluster node using the nodeId. In contrast to the
     * RedisAdvancedClusterConnection, node-connections do not route commands to other cluster nodes.
     *
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    StatefulRedisConnection<K, V> getConnection(String host, int port);

}
