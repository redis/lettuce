package com.lambdaworks.redis.cluster.api.sync;

import com.lambdaworks.redis.cluster.RedisAdvancedClusterConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;

/**
 * Advanced synchronous and thread-safe cluster API.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
public interface RedisAdvancedClusterCommands<K, V> extends RedisClusterCommands<K, V>, RedisAdvancedClusterConnection<K, V> {

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list. In
     * contrast to the {@link RedisAdvancedClusterCommands}, node-connections do not route commands to other cluster nodes
     * 
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    RedisClusterCommands<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. In contrast to the
     * {@link RedisAdvancedClusterCommands}, node-connections do not route commands to other cluster nodes
     * 
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    RedisClusterCommands<K, V> getConnection(String host, int port);

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterConnection<K, V> getStatefulConnection();

}
