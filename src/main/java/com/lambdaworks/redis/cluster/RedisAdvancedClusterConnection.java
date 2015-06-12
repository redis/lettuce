package com.lambdaworks.redis.cluster;

import java.util.function.Predicate;

import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Advanced synchronous and thread-safe cluster API.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.3
 * @deprecated Use {@link com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands}
 */
@Deprecated
public interface RedisAdvancedClusterConnection<K, V> extends RedisClusterConnection<K, V> {

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list. In
     * contrast to the {@link RedisAdvancedClusterConnection}, node-connections do not route commands to other cluster nodes
     * 
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    RedisClusterConnection<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. In contrast to the
     * {@link RedisAdvancedClusterConnection}, node-connections do not route commands to other cluster nodes
     * 
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    RedisClusterConnection<K, V> getConnection(String host, int port);

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterConnection<K, V> getStatefulConnection();

}
