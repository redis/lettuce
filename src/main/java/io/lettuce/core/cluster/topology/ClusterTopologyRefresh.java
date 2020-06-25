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
package io.lettuce.core.cluster.topology;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.resource.ClientResources;

/**
 * Utility to refresh the cluster topology view based on {@link Partitions}.
 *
 * @author Mark Paluch
 */
public interface ClusterTopologyRefresh {

    /**
     * Create a new {@link ClusterTopologyRefresh} instance.
     *
     * @param nodeConnectionFactory
     * @param clientResources
     * @return
     */
    static ClusterTopologyRefresh create(NodeConnectionFactory nodeConnectionFactory, ClientResources clientResources) {
        return new DefaultClusterTopologyRefresh(nodeConnectionFactory, clientResources);
    }

    /**
     * Load topology views from a collection of {@link RedisURI}s and return the view per {@link RedisURI}. Partitions contain
     * an ordered list of {@link RedisClusterNode}s. The sort key is latency. Nodes with lower latency come first.
     *
     * @param seed collection of {@link RedisURI}s
     * @param connectTimeout connect timeout
     * @param discovery {@code true} to discover additional nodes
     * @return mapping between {@link RedisURI} and {@link Partitions}
     */
    CompletionStage<Map<RedisURI, Partitions>> loadViews(Iterable<RedisURI> seed, Duration connectTimeout, boolean discovery);

}
