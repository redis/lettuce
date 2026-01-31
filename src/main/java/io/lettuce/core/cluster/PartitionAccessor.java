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
                || (redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA)
                        && upstream.getNodeId().equals(redisClusterNode.getReplicaOf())));
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
