package io.lettuce.core.cluster.api;

import java.util.Map;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * A node selection represents a set of Redis Cluster nodes. Provides access to particular node connection APIs and allows the
 * execution of commands on the selected cluster nodes.
 *
 * @param <API> API type.
 * @param <CMD> Command interface type to invoke multi-node operations.
 * @author Mark Paluch
 * @since 4.0
 */
public interface NodeSelectionSupport<API, CMD> {

    /**
     * @return number of nodes.
     */
    int size();

    /**
     * @return commands API to run on this node selection.
     */
    CMD commands();

    /**
     * Obtain the connection/commands to a particular node.
     *
     * @param index index of the node
     * @return the connection/commands object
     */
    API commands(int index);

    /**
     * Get the {@link RedisClusterNode}.
     *
     * @param index index of the cluster node
     * @return the cluster node
     */
    RedisClusterNode node(int index);

    /**
     * @return map of {@link RedisClusterNode} and the connection/commands objects
     */
    Map<RedisClusterNode, API> asMap();

}
