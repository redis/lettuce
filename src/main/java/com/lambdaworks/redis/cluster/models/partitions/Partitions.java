package com.lambdaworks.redis.cluster.models.partitions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.lambdaworks.redis.cluster.SlotHash;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Cluster topology view. An instance of {@link Partitions} provides access to the partitions of a Redis Cluster. A partition is
 * represented by a Redis Cluster node that has a {@link RedisClusterNode#getNodeId() nodeId} and
 * {@link RedisClusterNode#getUri() connection point details}.
 * <p>
 * Partitions can be looked up by {@code nodeId} or {@code slot} (masters only). A nodeId can be migrated to a different host.
 * Partitions are cached to ensure a cheap lookup by {@code slot}. Users of {@link Partitions} are required to call
 * {@link #updateCache()} after topology changes occur.
 * </p>
 *
 * Topology changes are:
 *
 * <ul>
 * <li>Changes in {@link com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode.NodeFlag#MASTER}/
 * {@link com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode.NodeFlag#SLAVE} state</li>
 * <li>Newly added or removed nodes to/from the Redis Cluster</li>
 * <li>Changes in {@link RedisClusterNode#getSlots()} responsibility</li>
 * <li>Changes to the {@link RedisClusterNode#getSlaveOf() slave replication source} (the master of a slave)</li>
 * <li>Changes to the {@link RedisClusterNode#getUri()} () connection point}</li>
 * </ul>
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class Partitions implements Collection<RedisClusterNode> {

    private final List<RedisClusterNode> partitions = new ArrayList<>();
    private final static RedisClusterNode[] EMPTY = new RedisClusterNode[SlotHash.SLOT_COUNT];
    private final static RedisClusterNode[] NO_NODES = new RedisClusterNode[0];

    private volatile RedisClusterNode slotCache[] = EMPTY;
    private volatile RedisClusterNode nodes[] = NO_NODES;

    /**
     * Retrieve a {@link RedisClusterNode} by its slot number. This method does not distinguish between masters and slaves.
     *
     * @param slot the slot
     * @return RedisClusterNode or {@literal null}
     */
    public RedisClusterNode getPartitionBySlot(int slot) {
        return slotCache[slot];
    }

    /**
     * Retrieve a {@link RedisClusterNode} by its node id.
     *
     * @param nodeId the nodeId
     * @return RedisClusterNode or {@literal null}
     */
    public RedisClusterNode getPartitionByNodeId(String nodeId) {

        RedisClusterNode nodes[] = this.nodes;
        for (RedisClusterNode partition : nodes) {
            if (partition.getNodeId().equals(nodeId)) {
                return partition;
            }
        }
        return null;
    }

    /**
     * Update the partition cache. Updates are necessary after the partition details have changed.
     */
    public void updateCache() {

        synchronized (partitions) {
            if (partitions.isEmpty()) {
                this.slotCache = EMPTY;
                return;
            }

            RedisClusterNode[] slotCache = new RedisClusterNode[SlotHash.SLOT_COUNT];
            RedisClusterNode[] nodes = new RedisClusterNode[partitions.size()];

            int i = 0;
            for (RedisClusterNode partition : partitions) {

                nodes[i++] = partition;
                for (Integer integer : partition.getSlots()) {
                    slotCache[integer.intValue()] = partition;
                }
            }

            this.slotCache = slotCache;
            this.nodes = nodes;
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

        LettuceAssert.notNull(partition, "Partition must not be null");

        synchronized (this) {
            slotCache = EMPTY;
            partitions.add(partition);
        }
    }

    @Override
    public int size() {
        return getPartitions().size();
    }

    public RedisClusterNode getPartition(int index) {
        return partitions.get(index);
    }

    /**
     * Update partitions and rebuild slot cache.
     *
     * @param partitions list of new partitions
     */
    public void reload(List<RedisClusterNode> partitions) {

        LettuceAssert.noNullElements(partitions, "Partitions must not contain null elements");

        synchronized (partitions) {
            this.partitions.clear();
            this.partitions.addAll(partitions);
            updateCache();
        }
    }

    @Override
    public boolean isEmpty() {
        return getPartitions().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return getPartitions().contains(o);
    }

    @Override
    public boolean addAll(Collection<? extends RedisClusterNode> c) {

        LettuceAssert.noNullElements(c, "Partitions must not contain null elements");

        synchronized (partitions) {
            boolean b = partitions.addAll(c);
            updateCache();
            return b;
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {

        synchronized (partitions) {
            boolean b = getPartitions().removeAll(c);
            updateCache();
            return b;
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {

        synchronized (partitions) {
            boolean b = getPartitions().retainAll(c);
            updateCache();
            return b;
        }
    }

    @Override
    public void clear() {

        synchronized (partitions) {
            getPartitions().clear();
            updateCache();
        }
    }

    @Override
    public Object[] toArray() {

        synchronized (partitions) {
            return getPartitions().toArray();
        }
    }

    @Override
    public <T> T[] toArray(T[] a) {

        synchronized (partitions) {
            return getPartitions().toArray(a);
        }
    }

    @Override
    public boolean add(RedisClusterNode redisClusterNode) {

        synchronized (partitions) {
            LettuceAssert.notNull(redisClusterNode, "RedisClusterNode must not be null");

            boolean add = getPartitions().add(redisClusterNode);
            updateCache();
            return add;
        }
    }

    @Override
    public boolean remove(Object o) {

        synchronized (partitions) {
            boolean remove = getPartitions().remove(o);
            updateCache();
            return remove;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return getPartitions().containsAll(c);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" ").append(partitions);
        return sb.toString();
    }
}
