package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisClusterConnection;

/**
 * Advanced asynchronous cluster API.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 */
public interface RedisAdvancedClusterAsyncConnection<K, V> extends RedisClusterAsyncConnection<K, V> {

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list.
     * 
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    RedisClusterAsyncConnection<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId.
     * 
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    RedisClusterAsyncConnection<K, V> getConnection(String host, int port);
}
