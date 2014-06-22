package com.lambdaworks.redis.cluster;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 26.05.14 17:11
 */
class Partitions implements Iterable<RedisClusterNode> {
    private List<RedisClusterNode> partitions = Lists.newArrayList();

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
