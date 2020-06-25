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

import java.util.*;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
class NodeTopologyViews {

    private List<NodeTopologyView> views;

    public NodeTopologyViews(List<NodeTopologyView> views) {
        this.views = views;
    }

    /**
     * Return cluster node URI's using the topology query sources and partitions.
     *
     * @return
     */
    public Set<RedisURI> getClusterNodes() {

        Set<RedisURI> result = new HashSet<>();

        Map<String, RedisURI> knownUris = new HashMap<>();
        for (NodeTopologyView view : views) {
            knownUris.put(view.getNodeId(), view.getRedisURI());
        }

        for (NodeTopologyView view : views) {
            for (RedisClusterNode redisClusterNode : view.getPartitions()) {
                if (knownUris.containsKey(redisClusterNode.getNodeId())) {
                    result.add(knownUris.get(redisClusterNode.getNodeId()));
                } else {
                    result.add(redisClusterNode.getUri());
                }
            }
        }

        return result;
    }

    /**
     * @return {@code true} if no views are present.
     */
    public boolean isEmpty() {
        return views.isEmpty();
    }

    public Map<RedisURI, Partitions> toMap() {

        Map<RedisURI, Partitions> nodeSpecificViews = new TreeMap<>(TopologyComparators.RedisURIComparator.INSTANCE);

        for (NodeTopologyView view : views) {
            nodeSpecificViews.put(view.getRedisURI(), view.getPartitions());
        }

        return nodeSpecificViews;
    }

}
