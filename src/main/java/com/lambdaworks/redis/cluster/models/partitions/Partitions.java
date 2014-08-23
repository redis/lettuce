package com.lambdaworks.redis.cluster.models.partitions;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class Partitions implements Iterable<RedisClusterNode> {
    private List<RedisClusterNode> partitions = Lists.newArrayList();

    /**
     * Retrieve a {@link RedisClusterNode} by it's slot number. This method does not distinguish between masters and slaves.
     * 
     * @param slot
     * @return RedisClusterNode or {@literal null}
     */
    public RedisClusterNode getPartitionBySlot(int slot) {

        for (RedisClusterNode partition : partitions) {
            if (partition.getSlots().contains(slot)) {
                return partition;
            }
        }
        return null;
    }

    @Override
    public Iterator<RedisClusterNode> iterator() {
        return Lists.newArrayList(partitions).iterator();
    }

    public List<RedisClusterNode> getPartitions() {
        return partitions;
    }

    public void addPartition(RedisClusterNode partition) {
        partitions.add(partition);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" ").append(partitions);
        return sb.toString();
    }
}
