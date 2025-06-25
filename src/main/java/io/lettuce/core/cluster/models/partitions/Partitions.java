/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.cluster.models.partitions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.SlotHash;
import io.lettuce.core.internal.LettuceAssert;

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
 * <li>Changes in {@link io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag#UPSTREAM}/
 * {@link io.lettuce.core.cluster.models.partitions.RedisClusterNode.NodeFlag#REPLICA} state</li>
 * <li>Newly added or removed nodes to/from the Redis Cluster</li>
 * <li>Changes in {@link RedisClusterNode#getSlots()} responsibility</li>
 * <li>Changes to the {@link RedisClusterNode#getSlaveOf() replication source} (the master of a replica)</li>
 * <li>Changes to the {@link RedisClusterNode#getUri()} () connection point}</li>
 * </ul>
 *
 * <p>
 * All query/read operations use the read-only view. Updates to Partitions are performed in an atomic way. Changes to the
 * read-only cache become visible after the partition update is completed.
 * </p>
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class Partitions implements Collection<RedisClusterNode> {

    private static final RedisClusterNode[] EMPTY = new RedisClusterNode[SlotHash.SLOT_COUNT];

    private final Lock lock = new ReentrantLock();

    private final List<RedisClusterNode> partitions = new ArrayList<>();

    private volatile RedisClusterNode[] slotCache = EMPTY;

    private volatile RedisClusterNode[] masterCache = EMPTY;

    private volatile Collection<RedisClusterNode> nodeReadView = Collections.emptyList();

    /**
     * Create a deep copy of this {@link Partitions} object.
     *
     * @return a deep copy of this {@link Partitions} object.
     */
    @Override
    public Partitions clone() {

        Collection<RedisClusterNode> readView = new ArrayList<>(nodeReadView);

        Partitions copy = new Partitions();

        for (RedisClusterNode node : readView) {
            copy.addPartition(node.clone());
        }

        copy.updateCache();

        return copy;
    }

    /**
     * Retrieve a {@link RedisClusterNode} by its slot number. This method does not distinguish between masters and slaves.
     *
     * @param slot the slot hash.
     * @return the {@link RedisClusterNode} or {@code null} if not found.
     */
    public RedisClusterNode getPartitionBySlot(int slot) {
        return slotCache[slot];
    }

    /**
     * Retrieve a {@link RedisClusterNode master node} by its slot number
     *
     * @param slot the slot hash.
     * @return the {@link RedisClusterNode} or {@code null} if not found.
     * @since 6.2
     */
    public RedisClusterNode getMasterBySlot(int slot) {
        return masterCache[slot];
    }

    /**
     * Retrieve a {@link RedisClusterNode} by its node id.
     *
     * @param nodeId the nodeId.
     * @return the {@link RedisClusterNode} or {@code null} if not found.
     */
    public RedisClusterNode getPartitionByNodeId(String nodeId) {

        for (RedisClusterNode partition : nodeReadView) {
            if (partition.getNodeId().equals(nodeId)) {
                return partition;
            }
        }

        return null;
    }

    /**
     * Retrieve a {@link RedisClusterNode} by its hostname/port considering node aliases.
     *
     * @param host hostname.
     * @param port port number.
     * @return the {@link RedisClusterNode} or {@code null} if not found.
     */
    public RedisClusterNode getPartition(String host, int port) {

        for (RedisClusterNode partition : nodeReadView) {

            RedisURI uri = partition.getUri();

            if (matches(uri, host, port)) {
                return partition;
            }

            for (RedisURI redisURI : partition.getAliases()) {

                if (matches(redisURI, host, port)) {
                    return partition;
                }
            }
        }

        return null;
    }

    private static boolean matches(RedisURI uri, String host, int port) {
        return uri.getPort() == port && host.equals(uri.getHost());
    }

    /**
     * Update the partition cache. Updates are necessary after the partition details have changed.
     */
    public void updateCache() {

        lock.lock();
        try {
            if (partitions.isEmpty()) {
                invalidateCache();
                return;
            }

            RedisClusterNode[] slotCache = new RedisClusterNode[SlotHash.SLOT_COUNT];
            RedisClusterNode[] masterCache = new RedisClusterNode[SlotHash.SLOT_COUNT];
            List<RedisClusterNode> readView = new ArrayList<>(partitions.size());

            for (RedisClusterNode partition : partitions) {

                readView.add(partition);
                if (partition.is(RedisClusterNode.NodeFlag.UPSTREAM)) {
                    partition.forEachSlot(i -> masterCache[i] = partition);
                }

                partition.forEachSlot(i -> slotCache[i] = partition);
            }

            this.slotCache = slotCache;
            this.masterCache = masterCache;
            this.nodeReadView = Collections.unmodifiableCollection(readView);
        } finally {
            lock.unlock();
        }
    }

    private void invalidateCache() {
        this.slotCache = EMPTY;
        this.masterCache = EMPTY;
        this.nodeReadView = Collections.emptyList();
    }

    /**
     * Returns an iterator over the {@link RedisClusterNode nodes} in this {@link Partitions} from the read-view. The
     * {@link Iterator} remains consistent during partition updates with the nodes that have been part of the {@link Partitions}
     * . {@link RedisClusterNode Nodes} added/removed during iteration/after obtaining the {@link Iterator} don't become visible
     * during iteration but upon the next call to {@link #iterator()}.
     *
     * @return an iterator over the {@link RedisClusterNode nodes} in this {@link Partitions} from the read-view.
     */
    @Override
    public Iterator<RedisClusterNode> iterator() {
        return nodeReadView.iterator();
    }

    /**
     * Returns the internal {@link List} of {@link RedisClusterNode} that holds the partition source. This {@link List} is used
     * to populate partition caches and should not be used directly and subject to change by refresh processes. Access
     * (read/write) requires synchronization on {@link #getPartitions()}.
     *
     * @return the internal partition source.
     */
    public List<RedisClusterNode> getPartitions() {
        return partitions;
    }

    /**
     * Adds a partition <b>without</b> updating the read view/cache.
     *
     * @param partition the partition
     */
    public void addPartition(RedisClusterNode partition) {

        LettuceAssert.notNull(partition, "Partition must not be null");

        lock.lock();
        try {
            invalidateCache();
            partitions.add(partition);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return the number of elements using the read-view.
     */
    @Override
    public int size() {
        return nodeReadView.size();
    }

    /**
     * Returns the {@link RedisClusterNode} at {@code index}.
     *
     * @param index the index
     * @return the requested element using the read-view.
     */
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

        lock.lock();
        try {
            this.partitions.clear();
            this.partitions.addAll(partitions);
            updateCache();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns {@code true} if this {@link Partitions} contains no elements using the read-view.
     *
     * @return {@code true} if this {@link Partitions} contains no elements using the read-view.
     */
    @Override
    public boolean isEmpty() {
        return nodeReadView.isEmpty();
    }

    /**
     * Returns {@code true} if this {@link Partitions} contains the specified element.
     *
     * @param o the element to check for
     * @return {@code true} if this {@link Partitions} contains the specified element
     */
    @Override
    public boolean contains(Object o) {
        return nodeReadView.contains(o);
    }

    /**
     * Add all {@link RedisClusterNode nodes} from the given collection and update the read-view/caches.
     *
     * @param c must not be {@code null}
     * @return {@code true} if this {@link Partitions} changed as a result of the call
     */
    @Override
    public boolean addAll(Collection<? extends RedisClusterNode> c) {

        LettuceAssert.noNullElements(c, "Partitions must not contain null elements");

        lock.lock();
        try {
            boolean b = partitions.addAll(c);
            updateCache();
            return b;
        } finally {
            lock.unlock();
        }
    }

    // just used in topology refresh, no race condition and lock is removed
    public void addAllWithoutCache(Collection<? extends RedisClusterNode> c) {

        LettuceAssert.noNullElements(c, "Partitions must not contain null elements");
        partitions.addAll(c);
        this.nodeReadView = Collections.unmodifiableCollection(c);
    }

    public void updateReadView() {
        this.nodeReadView = Collections.unmodifiableCollection(partitions);
    }

    /**
     * Remove all {@link RedisClusterNode nodes} from the {@link Partitions} using elements from the given collection and update
     * the read-view/caches.
     *
     * @param c must not be {@code null}
     * @return {@code true} if this {@link Partitions} changed as a result of the call
     */
    @Override
    public boolean removeAll(Collection<?> c) {

        lock.lock();
        try {
            boolean b = getPartitions().removeAll(c);
            updateCache();
            return b;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retains only the elements in this {@link Partitions} that are contained in the specified collection (optional
     * operation)and update the read-view/caches. In other words, removes from this collection all of its elements that are not
     * contained in the specified collection.
     *
     * @param c must not be {@code null}
     * @return {@code true} if this {@link Partitions} changed as a result of the call
     */
    @Override
    public boolean retainAll(Collection<?> c) {

        lock.lock();
        try {
            boolean b = getPartitions().retainAll(c);
            updateCache();
            return b;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes all {@link RedisClusterNode nodes} and update the read-view/caches.
     */
    @Override
    public void clear() {

        lock.lock();
        try {
            getPartitions().clear();
            updateCache();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an array containing all the elements in this {@link Partitions} using the read-view.
     *
     * @return an array containing all the elements in this {@link Partitions} using the read-view.
     */
    @Override
    public Object[] toArray() {
        return nodeReadView.toArray();
    }

    /**
     * Returns an array containing all the elements in this {@link Partitions} using the read-view.
     *
     * @param a the array into which the elements of this collection are to be stored, if it is big enough; otherwise, a new
     *        array of the same runtime type is allocated for this purpose.
     * @param <T> type of the array to contain the collection
     * @return an array containing all the elements in this {@link Partitions} using the read-view.
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return nodeReadView.toArray(a);
    }

    /**
     * Adds the {@link RedisClusterNode} to this {@link Partitions}.
     *
     * @param redisClusterNode must not be {@code null}
     * @return {@code true} if this {@link Partitions} changed as a result of the call
     */
    @Override
    public boolean add(RedisClusterNode redisClusterNode) {

        lock.lock();
        try {
            LettuceAssert.notNull(redisClusterNode, "RedisClusterNode must not be null");

            boolean add = getPartitions().add(redisClusterNode);
            updateCache();
            return add;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove the element from this {@link Partitions}.
     *
     * @param o must not be {@code null}
     * @return {@code true} if this {@link Partitions} changed as a result of the call
     */
    @Override
    public boolean remove(Object o) {

        lock.lock();
        try {
            boolean remove = getPartitions().remove(o);
            updateCache();
            return remove;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns {@code true} if this collection contains all of the elements in the specified collection.
     *
     * @param c collection to be checked for containment in this collection, must not be {@code null}
     * @return
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return nodeReadView.containsAll(c);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" ").append(partitions);
        return sb.toString();
    }

}
