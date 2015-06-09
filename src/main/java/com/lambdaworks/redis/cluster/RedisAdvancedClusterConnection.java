package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.cluster.api.StatefulClusterConnection;

/**
 * Advanced synchronous cluster API.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 */
public interface RedisAdvancedClusterConnection<K, V> extends RedisClusterConnection<K, V> {

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list.
     * 
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    RedisClusterConnection<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId.
     * 
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    RedisClusterConnection<K, V> getConnection(String host, int port);

    /**
     * @return the underlying connection.
     */
    StatefulClusterConnection<K, V> getStatefulConnection();
}
