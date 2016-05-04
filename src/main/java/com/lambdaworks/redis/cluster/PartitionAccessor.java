package com.lambdaworks.redis.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

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

    List<RedisClusterNode> getMasters() {
        return get(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MASTER));
    }

    List<RedisClusterNode> getSlaves() {
        return get(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE));

    }

    List<RedisClusterNode> getSlaves(RedisClusterNode master) {
        return get(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE)
                && master.getNodeId().equals(redisClusterNode.getSlaveOf()));
    }

    List<RedisClusterNode> getReadCandidates(RedisClusterNode master) {
        return get(redisClusterNode -> redisClusterNode.getNodeId().equals(master.getNodeId())
                || (redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE) && master.getNodeId().equals(
                        redisClusterNode.getSlaveOf())));
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
