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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Accessor for Partitions.
 *
 * @author Mark Paluch
 */
class PartitionAccessor {

    private final Collection<RedisClusterNode> partitions;

    PartitionAccessor(Collection<RedisClusterNode> partitions) {
        this.partitions = partitions;
    }

    List<RedisClusterNode> getUpstream() {
        return get(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.UPSTREAM));
    }

    List<RedisClusterNode> getReadCandidates(RedisClusterNode upstream) {
        return get(redisClusterNode -> redisClusterNode.getNodeId().equals(upstream.getNodeId())
                || (redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE)
                        && upstream.getNodeId().equals(redisClusterNode.getSlaveOf())));
    }

    List<RedisClusterNode> get(Predicate<RedisClusterNode> test) {

        List<RedisClusterNode> result = new ArrayList<>(partitions.size());
        for (RedisClusterNode partition : partitions) {
            if (test.test(partition)) {
                result.add(partition);
            }
        }
        return result;
    }

}
