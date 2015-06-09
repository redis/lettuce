package com.lambdaworks.redis.cluster.api;

import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterAsyncConnection;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterConnection;

/**
 * A stateful cluster connection providing command dispatching, timeouts and open/close methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
public interface StatefulClusterConnection<K, V> extends StatefulConnection<K, V> {

    RedisAdvancedClusterAsyncConnection<K, V> async();

    RedisAdvancedClusterConnection<K, V> sync();

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list.
     *
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    StatefulClusterConnection<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId.
     *
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    StatefulClusterConnection<K, V> getConnection(String host, int port);

}
