/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster.api;

import java.util.Map;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * A node selection represents a set of Redis Cluster nodes. Provides access to particular node connection APIs and allows the
 * execution of commands on the selected cluster nodes.
 *
 * @param <API> API type.
 * @param <CMD> Command command interface type to invoke multi-node operations.
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
