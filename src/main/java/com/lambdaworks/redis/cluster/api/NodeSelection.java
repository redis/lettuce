package com.lambdaworks.redis.cluster.api;

import java.util.Map;

import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Selection of nodes. Provides access to particular node connections and allows the execution of commands on the selection of
 * nodes.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface NodeSelection<NODECONNType, CMDType> extends Iterable<NODECONNType> {

    /**
     * 
     * @return number of nodes
     */
    int size();

    /**
     * Obtain the connection/commands to the node.
     * 
     * @param index index of the node
     * @return the connection/commands object
     */
    NODECONNType node(int index);

    /**
     * Get the {@link RedisClusterNode}.
     *
     * @param index index of the cluster node
     * @return the cluster node
     */
    RedisClusterNode get(int index);

    /**
     * 
     * @return map of {@link RedisClusterNode} and the connection/commands objects
     */
    Map<RedisClusterNode, NODECONNType> asMap();

    /**
     * 
     * @return commands to run on this node selection
     */
    CMDType commands();

}
