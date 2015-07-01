package com.lambdaworks.redis.cluster.models.partitions;

import java.util.*;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.cluster.SlotHash;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class Partitions extends AbstractCollection<RedisClusterNode> implements Collection<RedisClusterNode> {
    private List<RedisClusterNode> partitions = Lists.newArrayList();
    private RedisClusterNode slotCache[];

    /**
     * Retrieve a {@link RedisClusterNode} by it's slot number. This method does not distinguish between masters and slaves.
     * 
     * @param slot the slot
     * @return RedisClusterNode or {@literal null}
     */
    public RedisClusterNode getPartitionBySlot(int slot) {
        return slotCache[slot];
    }

    /**
     * Update the partition cache.
     */
    public void updateCache() {
        if (slotCache == null) {
            slotCache = new RedisClusterNode[SlotHash.SLOT_COUNT];
        } else {
            Arrays.fill(slotCache, null);
        }

        for (RedisClusterNode partition : partitions) {
            for (Integer integer : partition.getSlots()) {
                slotCache[integer.intValue()] = partition;
            }
        }
    }

    @Override
    public Iterator<RedisClusterNode> iterator() {
        return partitions.iterator();
    }

    public List<RedisClusterNode> getPartitions() {
        return partitions;
    }

    public void addPartition(RedisClusterNode partition) {
        slotCache = null;
        partitions.add(partition);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" ").append(partitions);
        return sb.toString();
    }

    @Override
    public int size() {
        return getPartitions().size();
    }

    public RedisClusterNode getPartition(int index) {
        return getPartitions().get(index);
    }

    /**
     * Update partitions and clear slot cache.
     * 
     * @param partitions list of new partitions
     */
    public void reload(List<RedisClusterNode> partitions) {
        this.partitions.clear();
        this.partitions.addAll(partitions);
        updateCache();
    }
}
