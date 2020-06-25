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
package io.lettuce.core.cluster;

import java.util.*;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
class PartitionsConsensusTestSupport {

    static RedisClusterNode createNode(int nodeId) {
        return new RedisClusterNode(RedisURI.create("localhost", 6379 + nodeId), "" + nodeId, true, "", 0, 0, 0,
                Collections.emptyList(), new HashSet<>());
    }

    static Partitions createPartitions(RedisClusterNode... nodes) {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(nodes));
        return partitions;
    }

    static Map<RedisURI, Partitions> createMap(Partitions... partitionses) {

        Map<RedisURI, Partitions> partitionsMap = new HashMap<>();

        int counter = 0;
        for (Partitions partitions : partitionses) {
            partitionsMap.put(createNode(counter++).getUri(), partitions);
        }

        return partitionsMap;
    }

}
